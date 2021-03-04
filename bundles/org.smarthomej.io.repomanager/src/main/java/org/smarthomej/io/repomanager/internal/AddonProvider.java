/**
 * Copyright (c) 2021 Contributors to the SmartHome/J project
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.smarthomej.io.repomanager.internal;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

    private final Logger logger = LoggerFactory.getLogger(AddonProvider.class);

    private final FeaturesService featuresService;
    private final EventPublisher eventPublisher;
    private final ConfigurationAdmin configurationAdmin;
    private final ScheduledExecutorService scheduler;

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

        // install all feature repositories (if necessary)
        String featureRepoString = Objects.requireNonNullElse((String) configuration.get(FEATURE_REPO_CONFIG_ID), "");
        processFeatureRepoList(Arrays.stream(featureRepoString.split(",")).filter(s -> !s.isEmpty()).map(URI::create)
                .collect(Collectors.toSet()));

        // install all addons (if necessary)
        String addonsString = (String) configuration.get(ADDON_CONFIG_ID);
        if (addonsString != null && !addonsString.isEmpty()) {
            processAddonList(
                    Arrays.stream(addonsString.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
        }
        buildAddonList();

        logger.debug("Addon provider started.");
        logger.trace("Configuration: {}", configuration);
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
            properties.put(ADDON_CONFIG_ID, installedAddons.stream().collect(Collectors.joining(",")));
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
     * @param groupId Maven coordinates (groupId) of the repo
     * @param artifactId Maven coordinates (artifactId) of the repo
     * @param version Maven coordinates (version) of the repo
     */
    public void addFeatureRepository(String groupId, String artifactId, String version) {
        URI repoUri = uriFromCoordinates(groupId, artifactId, version);
        try {
            featuresService.addRepository(repoUri);
            installedFeatureRepos.add(repoUri);
            storeConfiguration();
            logger.debug("Added feature repository '{}'", repoUri);

            buildAddonList();
        } catch (Exception e) {
            logger.warn("Failed to add feature repository '{}': {}", repoUri, e.getMessage());
        }
    }

    /**
     * remove a feature repository from Addon provider (also uninstalls all provided features)
     *
     * @param groupId Maven coordinates (groupId) of the repo
     * @param artifactId Maven coordinates (artifactId) of the repo
     * @param version Maven coordinates (version) of the repo
     */
    public void removeFeatureRepository(String groupId, String artifactId, String version) {
        URI repoUri = uriFromCoordinates(groupId, artifactId, version);
        try {
            featuresService.removeRepository(repoUri, true);
            installedFeatureRepos.remove(repoUri);
            storeConfiguration();
            logger.debug("Removed feature repository '{}'", repoUri);

            buildAddonList();
        } catch (Exception e) {
            logger.warn("Failed to remove feature repository '{}': {}", repoUri, e.getMessage());
        }
    }

    public boolean statusFeatureRepository(String groupId, String artifactId, String version) {
        try {
            URI uri = uriFromCoordinates(groupId, artifactId, version);
            return Arrays.stream(featuresService.listRepositories()).map(Repository::getURI).anyMatch(uri::equals);
        } catch (Exception e) {
            return false;
        }
    }

    private URI uriFromCoordinates(String groupId, String artifactId, String version) {
        return URI.create("mvn:" + groupId + "/" + artifactId + "/" + version + "/xml/features");
    }

    private String addonIdFromFeature(Feature f) {
        return f.getName() + "_" + f.getVersion().replace(".", "_");
    }

    private String featureIdFromAddonId(String addonId) {
        return addonId.substring(0, addonId.indexOf("_"));
    }

    private String featureVersionFromAddonId(String addonId) {
        return addonId.substring(addonId.indexOf("_") + 1).replace("_", ".");
    }

    private void buildAddonList() {
        try {
            availableAddons = Arrays.stream(featuresService.listFeatures())
                    .filter(f -> f.getName().contains("smarthomej-binding"))
                    .map(f -> new Addon(addonIdFromFeature(f), "smarthomej-binding", f.getDescription(), f.getVersion(),
                            "https://github.com/smarthomej/addons/blob/main/bundles/org.smarthomej.binding."
                                    + f.getName().replace("smarthomej-binding-", "") + "/README.md",
                            featuresService.isInstalled(f), f.getDescription(), null, null))
                    .collect(Collectors.toMap(Addon::getId, a -> a));
            logger.trace("Available addons: {}", availableAddons);
        } catch (Exception e) {
            logger.warn("Failed to build addon list: {}", e.getMessage());
        }
    }

    private void processFeatureRepoList(Set<URI> featureRepos) {
        featureRepos.forEach(r -> {
            try {
                featuresService.addRepository(r);
            } catch (Exception e) {
                logger.warn("Failed to install feature repo '{}', some addons may not be available: {}", r,
                        e.getMessage());
            }
        });
        installedFeatureRepos = new HashSet<>(featureRepos);
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
                featuresService.installFeature(featureIdFromAddonId(addonId), featureVersionFromAddonId(addonId),
                        EnumSet.of(FeaturesService.Option.NoFailOnFeatureNotFound, FeaturesService.Option.Upgrade));
            } catch (Exception e) {
                logger.warn("Failed to install addon {}: {}", addonId, e.getMessage());
            }
        });

        installedAddons = new HashSet<>(addons);
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
        return List.of(new AddonType("smarthomej-binding", "SmartHome/J Bindings"));
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
