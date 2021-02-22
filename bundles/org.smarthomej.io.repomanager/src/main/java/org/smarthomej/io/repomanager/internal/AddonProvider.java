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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AddonProvider} is a service providing the SmartHome/J addons.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(name = "org.smarthomej.addons", service = { AddonProvider.class, AddonService.class })
public class AddonProvider implements AddonService {
    private final Logger logger = LoggerFactory.getLogger(AddonProvider.class);

    private final FeaturesService featuresService;
    private final EventPublisher eventPublisher;
    private final ScheduledExecutorService scheduler;

    private Map<String, Addon> addons = Map.of();

    @Activate
    public AddonProvider(@Reference FeaturesService featuresService, @Reference EventPublisher eventPublisher) {
        this.featuresService = featuresService;
        this.eventPublisher = eventPublisher;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("smarthomej-addons"));

        buildAddonList();

        logger.debug("Addon provider started.");
    }

    /**
     * add a feature repository from Addon provider
     *
     * @param groupId Maven coordinates (groupId) of the repo
     * @param artifactId Maven coordinates (artifactId) of the repo
     * @param version Maven coordinates (version) of the repo
     * @return true if successfully added, false otherwise
     */
    public boolean addFeatureRepository(String groupId, String artifactId, String version) {
        try {
            featuresService.addRepository(uriFromCoordinates(groupId, artifactId, version));
            logger.debug("Added feature repository '{}/{}/{}'", groupId, artifactId, version);
            buildAddonList();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * remove a feature repository from Addon provider (also uninstalls all provided features)
     *
     * @param groupId Maven coordinates (groupId) of the repo
     * @param artifactId Maven coordinates (artifactId) of the repo
     * @param version Maven coordinates (version) of the repo
     * @return true if successfully removed, false otherwise
     */
    public boolean removeFeatureRepository(String groupId, String artifactId, String version) {
        try {
            featuresService.removeRepository(uriFromCoordinates(groupId, artifactId, version), true);
            logger.debug("Removed feature repository '{}/{}/{}'", groupId, artifactId, version);
            buildAddonList();
        } catch (Exception e) {
            return false;
        }
        return true;
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
        return f.getName() + "_" + f.getVersion().replace(".", "-");
    }

    private String featureIdFromAddonId(String addonId) {
        return addonId.substring(0, addonId.indexOf("_"));
    }

    private void buildAddonList() {
        try {
            addons = Arrays.stream(featuresService.listFeatures())
                    .filter(f -> f.getName().contains("smarthomej-binding"))
                    .map(f -> new Addon(addonIdFromFeature(f), "smarthomej-binding", f.getDescription(), f.getVersion(),
                            "https://github.com/smarthomej/addons/blob/main/bundles/org.smarthomej.binding."
                                    + f.getName().replace("smarthomej-binding-", "") + "/README.md",
                            featuresService.isInstalled(f), f.getDescription(), null, null))
                    .collect(Collectors.toMap(Addon::getId, a -> a));
            logger.trace("Available addons: {}", addons);
        } catch (Exception e) {
            logger.warn("Failed to build addon list: {}", e.getMessage());
        }
    }

    @Override
    @NonNullByDefault({})
    public List<Addon> getAddons(@Nullable Locale locale) {
        return new ArrayList<>(addons.values());
    }

    @Override
    public @Nullable Addon getAddon(@Nullable String id, @Nullable Locale locale) {
        return addons.get(id);
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
                    Addon addon = addons.get(id);
                    if (addon == null) {
                        throw new IllegalArgumentException("No addon with found with id" + id);
                    }
                    featuresService.installFeature(featureIdFromAddonId(addon.getId()), addon.getVersion(),
                            EnumSet.of(FeaturesService.Option.Upgrade, FeaturesService.Option.NoFailOnFeatureNotFound));

                    eventPublisher.post(AddonEventFactory.createAddonInstalledEvent(id));
                    logger.info("Installed {}", id);
                    addon.setInstalled(true);
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
                    Addon addon = addons.get(id);
                    if (addon == null) {
                        throw new IllegalArgumentException("No addon with found with id" + id);
                    }
                    featuresService.uninstallFeature(featureIdFromAddonId(addon.getId()), addon.getVersion());
                    addon.setInstalled(false);
                    eventPublisher.post(AddonEventFactory.createAddonUninstalledEvent(id));
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
