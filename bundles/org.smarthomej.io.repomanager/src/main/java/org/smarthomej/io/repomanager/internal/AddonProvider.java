/**
 * Copyright (c) 2021 Contributors to the SmartHome/J project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.smarthomej.io.repomanager.internal;

import static org.smarthomej.io.repomanager.internal.RepoManagerConstants.*;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.events.EventPublisher;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AddonProvider} is a service providing the SmartHome/J addons.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = { AddonProvider.class,
        AddonService.class }, configurationPid = AddonProvider.CONFIGURATION_PID, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class AddonProvider implements AddonService {
    public static final String CONFIGURATION_PID = "smarthomej.AddonProvider";
    private static final String ADDON_CONFIG_ID = "installedAddons";
    private static final String FEATURE_REPO_CONFIG_ID = "installedFeatureRepos";
    private static final Pattern ADDON_TYPE = Pattern.compile("(smarthomej-[a-zA-Z]+)-.*");
    private static final Set<String> SUPPORTED_ADDON_TYPES = Set.of("smarthomej-automation", "smarthomej-binding",
            "smarthomej-persistence", "smarthomej-transform");

    private final Logger logger = LoggerFactory.getLogger(AddonProvider.class);

    private final FeaturesService featuresService;
    private final EventPublisher eventPublisher;
    private final ConfigurationAdmin configurationAdmin;
    private final ScheduledExecutorService scheduler;
    private final MavenRepoManager mavenRepoManager;

    private Map<String, Addon> availableAddons = Map.of();
    private Set<String> installedAddons = new HashSet<>();
    private Set<URI> installedFeatureRepos = new HashSet<>();

    @Activate
    public AddonProvider(@Reference FeaturesService featuresService, @Reference EventPublisher eventPublisher,
            @Reference ConfigurationAdmin configurationAdmin, @Reference MavenRepoManager mavenRepoManager,
            Map<String, Object> configuration) {
        this.featuresService = featuresService;
        this.eventPublisher = eventPublisher;
        this.configurationAdmin = configurationAdmin;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("smarthomej-addons"));
        this.mavenRepoManager = mavenRepoManager;

        initialize(configuration);
        logger.debug("Addon provider started.");
    }

    private void initialize(Map<String, Object> configuration) {
        try {
            // install all feature repositories (if necessary)
            String featureRepoString = (String) configuration.getOrDefault(FEATURE_REPO_CONFIG_ID, "");
            processFeatureRepoList(Arrays.stream(featureRepoString.split(",")).filter(s -> !s.isEmpty())
                    .map(URI::create).collect(Collectors.toSet()));

            // build list (status may be wrong, we install addons in the next step)
            buildAddonList();

            // install all addons (if necessary)
            String addonsString = (String) configuration.get(ADDON_CONFIG_ID);
            if (addonsString != null && !addonsString.isEmpty()) {
                processAddonList(
                        Arrays.stream(addonsString.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
            }

            // refresh status after installation
            buildAddonList();
        } catch (MavenRepoManagerException e) {
            logger.error("Failed to initialize RepoManager AddonProvider: {}, retrying in 10s.", e.getMessage());
            scheduler.schedule(() -> initialize(configuration), 10, TimeUnit.SECONDS);
        }
    }

    @SuppressWarnings("unused")
    @Modified
    public void modified(Map<String, Object> cfg) {
        // ignore configuration update, we already handle it by ourself
    }

    @SuppressWarnings("unused")
    @Deactivate
    public void deactivate() {
        logger.debug("Addon provider stopped.");
    }

    private void storeConfiguration() {
        try {
            // store current configuration
            Configuration configuration = configurationAdmin.getConfiguration(CONFIGURATION_PID);
            Dictionary<String, Object> properties = Objects.requireNonNullElse(configuration.getProperties(),
                    new Hashtable<>());
            properties.put(ADDON_CONFIG_ID, String.join(",", installedAddons));
            properties.put(FEATURE_REPO_CONFIG_ID,
                    installedFeatureRepos.stream().map(URI::toString).collect(Collectors.joining(",")));
            configurationAdmin.getConfiguration(CONFIGURATION_PID).update(properties);
        } catch (IOException e) {
            logger.warn("Could not store configuration: {}", e.getMessage());
        }
    }

    /**
     * add a feature repository from Addon provider
     *
     * @param gav Maven coordinates (groupId, artifactId, version) of the repo
     */
    public void addFeatureRepository(GAV gav) throws MavenRepoManagerException {
        addFeatureRepoUri(uriFromCoordinates(gav));
        buildAddonList();
    }

    private void addFeatureRepoUri(URI repoUri) {
        try {
            featuresService.addRepository(repoUri);
            installedFeatureRepos.add(repoUri);
            storeConfiguration();
            logger.debug("Added feature repository '{}'", repoUri);
        } catch (Exception e) {
            logger.warn("Failed to add feature repository '{}': {}", repoUri, e.getMessage());
        }
    }

    /**
     * remove a feature repository from Addon provider (also uninstalls all provided features)
     *
     * @param gav Maven coordinates (groupId, artifactId, version) of the repo
     */
    public void removeFeatureRepository(GAV gav) throws MavenRepoManagerException {
        removeFeatureRepoURI(uriFromCoordinates(gav));
        buildAddonList();
    }

    private void removeFeatureRepoURI(URI repoUri) {
        try {
            featuresService.removeRepository(repoUri, true);
            installedFeatureRepos.remove(repoUri);
            storeConfiguration();
            logger.debug("Removed feature repository '{}'", repoUri);
        } catch (Exception e) {
            logger.warn("Failed to remove feature repository '{}': {}", repoUri, e.getMessage());
        }
    }

    /**
     * get the status of a given feature repository
     *
     * @param gav Maven coordinates (groupId, artifactId, version) of the repo
     * @return true if installed, false if not installed or check failed
     */
    public boolean statusFeatureRepository(GAV gav) {
        try {
            URI uri = uriFromCoordinates(gav);
            return Arrays.stream(featuresService.listRepositories()).map(Repository::getURI).anyMatch(uri::equals);
        } catch (Exception e) {
            return false;
        }
    }

    private URI uriFromCoordinates(GAV gav) {
        return URI.create("mvn:" + gav.groupId + "/" + gav.artifactId + "/" + gav.version + "/xml/features");
    }

    private String addonIdFromFeature(Feature f) {
        return f.getName() + "_" + f.getVersion().replace(".", "_");
    }

    private String addonTypeFromFeature(Feature f) {
        Matcher matcher = ADDON_TYPE.matcher(f.getName());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "smarthomej-unknown";
    }

    private String featureIdFromAddonId(String addonId) {
        return addonId.substring(0, addonId.indexOf("_"));
    }

    private String featureVersionFromAddonId(String addonId) {
        return addonId.substring(addonId.indexOf("_") + 1).replace("_", ".");
    }

    private boolean isSupportedAddon(Feature f) {
        return SUPPORTED_ADDON_TYPES.contains(addonTypeFromFeature(f));
    }

    private String docLinkFromFeature(Feature f) {
        return "https://github.com/smarthomej/addons/blob/main/bundles/org." + f.getName().replace("-", ".")
                + "/README.md";
    }

    private void buildAddonList() throws MavenRepoManagerException {
        try {
            availableAddons = Arrays.stream(featuresService.listFeatures()).filter(this::isSupportedAddon)
                    .map(f -> new Addon(addonIdFromFeature(f), addonTypeFromFeature(f), f.getDescription(),
                            f.getVersion(), docLinkFromFeature(f), featuresService.isInstalled(f), f.getDescription(),
                            null, null))
                    .collect(Collectors.toMap(Addon::getId, a -> a));
            logger.trace("Available addons: {}", availableAddons);
        } catch (MavenRepoManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to build addon list: {}", e.getMessage());
        }
    }

    private void processFeatureRepoList(Set<URI> featureRepos) throws MavenRepoManagerException {
        try {
            Set<URI> successURIs = new HashSet<>();
            featureRepos.forEach(repoUri -> {
                try {
                    if (repoUri.toString().contains("SNAPSHOT")) {
                        // SNAPSHOTs may have moved on, check if the version is still available
                        List<String> availableVersions = mavenRepoManager.getAvailableVersions(SNAPSHOT_REPO_ID,
                                KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID);
                        if (availableVersions.stream().anyMatch(v -> repoUri.toString().contains(v))) {
                            featuresService.addRepository(repoUri);
                            successURIs.add(repoUri);
                        } else {
                            // selected SNAPSHOT version is not available
                            availableVersions.stream().min(Comparator.reverseOrder()).ifPresentOrElse(v -> {
                                // a new version is available
                                URI newSnapshotRepoURI = URI
                                        .create(repoUri.toString().replaceAll("\\d+\\.\\d+\\.\\d+-SNAPSHOT", v));
                                logger.info(
                                        "Requested feature repository '{}' is no longer available, upgrading to best matching repository '{}'",
                                        repoUri, newSnapshotRepoURI);
                                removeFeatureRepoURI(repoUri);
                                addFeatureRepoUri(newSnapshotRepoURI);
                                successURIs.add(newSnapshotRepoURI);
                            }, () -> {
                                // there is no compatible snapshot
                                logger.warn(
                                        "Requested feature repository '{}' is no longer available, can't find matching repository, removing",
                                        repoUri);
                                removeFeatureRepoURI(repoUri);
                                mavenRepoManager.removeRepository(SNAPSHOT_REPO_ID);
                            });
                        }
                    } else {
                        featuresService.addRepository(repoUri);
                        successURIs.add(repoUri);
                    }
                } catch (MavenRepoManagerException e) {
                    // encapsulate MavenRepoManagerException in RuntimeException to get it out of the lambda
                    throw new IllegalStateException(e);
                } catch (Exception e) {
                    logger.warn("Failed to install feature repo '{}', some addons may not be available: {}", repoUri,
                            e.getMessage());
                }
            });
            installedFeatureRepos = new HashSet<>(successURIs);
        } catch (IllegalStateException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MavenRepoManagerException) {
                throw (MavenRepoManagerException) cause;
            }
        }
    }

    private void processAddonList(Set<String> addons) {
        Set<String> installedFeatures = Set.of();

        try {
            installedFeatures = Arrays.stream(featuresService.listInstalledFeatures()).map(Feature::getId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.warn("Failed to get list of installed features, installing everything");
        }

        Set<String> toBeInstalled = new HashSet<>(addons);
        toBeInstalled.removeAll(installedFeatures);
        toBeInstalled.forEach(addonId -> {
            try {
                String featureId = featureIdFromAddonId(addonId);
                String featureVersion = featureVersionFromAddonId(addonId);
                Feature feature = featuresService.getFeature(featureId, featureVersion);
                if (feature != null) {
                    install(addonId);
                } else {
                    if (featureVersion.contains("SNAPSHOT")) {
                        availableAddons.keySet().stream().filter(addon -> addon.contains(featureId))
                                .filter(addon -> addon.contains("SNAPSHOT")).findAny().ifPresentOrElse(newAddonId -> {
                                    logger.info(
                                            "Could not install addon '{}' but found a matching alternative: '{}'. Upgrading.",
                                            addonId, newAddonId);
                                    install(newAddonId);
                                }, () -> logger.warn(
                                        "Could not install addon '{}' because it is missing in the available repository and no matching alternative version could be found. Removing.",
                                        addonId));
                    } else {
                        logger.warn(
                                "Could not install addon '{}' because it is missing in the available repositories. Removing.",
                                addonId);
                    }
                    addons.remove(addonId);
                }
            } catch (Exception e) {
                logger.warn("Failed to install addon {}: {}", addonId, e.getMessage());
            }
        });

        if (!installedAddons.equals(addons)) {
            installedAddons = new HashSet<>(addons);
            storeConfiguration();
        }
    }

    @Override
    @NonNullByDefault({})
    public List<Addon> getAddons(@Nullable Locale locale) {
        return new ArrayList<>(availableAddons.values());
    }

    @Override
    public @Nullable Addon getAddon(@Nullable String id, @Nullable Locale locale) {
        return availableAddons.get(id);
    }

    @Override
    @NonNullByDefault({})
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return List.of(new AddonType("smarthomej-automation", "SmartHome/J Automation"),
                new AddonType("smarthomej-binding", "SmartHome/J Binding"),
                new AddonType("smarthomej-persistence", "SmartHome/J Persistence"),
                new AddonType("smarthomej-transform", "SmartHome/J Transformation"));
    }

    @Override
    public void install(@Nullable String id) {
        if (id != null) {
            scheduler.execute(() -> {
                try {
                    Addon addon = availableAddons.get(id);
                    if (addon == null) {
                        throw new IllegalArgumentException("No addon with found with id" + id);
                    }
                    featuresService.installFeature(featureIdFromAddonId(addon.getId()), addon.getVersion(),
                            EnumSet.of(FeaturesService.Option.Upgrade, FeaturesService.Option.NoFailOnFeatureNotFound));

                    eventPublisher.post(AddonEventFactory.createAddonInstalledEvent(id));
                    addon.setInstalled(true);
                    installedAddons.add(id);
                    storeConfiguration();

                    logger.info("Installed {}", id);
                } catch (Exception e) {
                    logger.warn("Failed to install {}: {}", id, e.getMessage());
                }
            });
        }
    }

    @Override
    public void uninstall(@Nullable String id) {
        if (id != null) {
            scheduler.execute(() -> {
                try {
                    Addon addon = availableAddons.get(id);
                    if (addon == null) {
                        throw new IllegalArgumentException("No addon with found with id" + id);
                    }
                    featuresService.uninstallFeature(featureIdFromAddonId(addon.getId()), addon.getVersion());

                    addon.setInstalled(false);
                    eventPublisher.post(AddonEventFactory.createAddonUninstalledEvent(id));
                    installedAddons.remove(id);
                    storeConfiguration();

                    logger.info("Uninstalled {}", id);
                } catch (Exception e) {
                    logger.warn("Failed to uninstall {}: {}", id, e.getMessage());
                }
            });
        }
    }

    @Override
    public @Nullable String getAddonId(@Nullable URI addonURI) {
        return null;
    }
}
