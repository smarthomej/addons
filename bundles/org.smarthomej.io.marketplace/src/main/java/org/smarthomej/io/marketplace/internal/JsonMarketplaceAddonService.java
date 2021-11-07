/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.io.marketplace.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * This class is a {@link AddonService} retrieving JSON marketplace information.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Jan N. Klug - Refactored for JSON marketplaces
 *
 */
@Component(immediate = true)
public class JsonMarketplaceAddonService implements AddonService {
    private static final String ADDON_ID_PREFIX = "jsonmarketplace:";
    private static final String MARKETPLACE_URL = "https://download.smarthomej.org/addons.json";

    private static final Map<String, AddonType> TAG_ADDON_TYPE_MAP = Map.of( //
            "automation", new AddonType("automation", "Automation"), //
            "binding", new AddonType("binding", "Bindings"), //
            "misc", new AddonType("misc", "Misc"), //
            "persistence", new AddonType("persistence", "Persistence"), //
            "transformation", new AddonType("transformation", "Transformations"), //
            "ui", new AddonType("ui", "User Interfaces"), //
            "voice", new AddonType("voice", "Voice"));

    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
    private final Set<MarketplaceAddonHandler> addonHandlers = new HashSet<>();

    private EventPublisher eventPublisher;

    @Activate
    public void activate() {
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.add(handler);
    }

    protected void removeAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.remove(handler);
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Override
    public String getId() {
        return "jsonmarketplace";
    }

    @Override
    public String getName() {
        return "JSON 3rd Party Marketplace";
    }

    @Override
    public void refreshSource() {
    }

    @Override
    public List<Addon> getAddons(Locale locale) {
        return getRemoteAddonList().stream().map(this::fromAddonEntry).collect(Collectors.toList());
    }

    @Override
    public Addon getAddon(String id, Locale locale) {
        String remoteId = id.replace(ADDON_ID_PREFIX, "");
        return getRemoteAddonList().stream().filter(e -> remoteId.equals(e.id)).map(this::fromAddonEntry).findAny()
                .orElse(null);
    }

    @Override
    public List<AddonType> getTypes(Locale locale) {
        return new ArrayList<>(TAG_ADDON_TYPE_MAP.values());
    }

    @Override
    public void install(String id) {
        Addon addon = getAddon(id, null);
        for (MarketplaceAddonHandler handler : addonHandlers) {
            if (handler.supports(addon.getType(), addon.getContentType())) {
                if (!handler.isInstalled(addon.getId())) {
                    try {
                        handler.install(addon);
                        postInstalledEvent(addon.getId());
                    } catch (MarketplaceHandlerException e) {
                        postFailureEvent(addon.getId(), e.getMessage());
                    }
                } else {
                    postFailureEvent(addon.getId(), "Add-on is already installed.");
                }
                return;
            }
        }
        postFailureEvent(id, "Add-on not known.");
    }

    @Override
    public void uninstall(String id) {
        Addon addon = getAddon(id, null);
        for (MarketplaceAddonHandler handler : addonHandlers) {
            if (handler.supports(addon.getType(), addon.getContentType())) {
                if (handler.isInstalled(addon.getId())) {
                    try {
                        handler.uninstall(addon);
                        postUninstalledEvent(addon.getId());
                    } catch (MarketplaceHandlerException e) {
                        postFailureEvent(addon.getId(), e.getMessage());
                    }
                } else {
                    postFailureEvent(addon.getId(), "Add-on is not installed.");
                }
                return;
            }
        }
        postFailureEvent(id, "Add-on not known.");
    }

    @Override
    public String getAddonId(URI addonURI) {
        return null;
    }

    private List<AddonEntryDTO> getRemoteAddonList() {
        try {
            URL url = new URL(MARKETPLACE_URL);
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Accept", "application/json");

            try (Reader reader = new InputStreamReader(connection.getInputStream())) {
                Type type = TypeToken.getParameterized(List.class, AddonEntryDTO.class).getType();
                return Objects.requireNonNull(gson.fromJson(reader, type));
            }
        } catch (IOException e) {
            return List.of();
        }
    }

    private Addon fromAddonEntry(AddonEntryDTO addonEntry) {
        String fullId = ADDON_ID_PREFIX + addonEntry.id;
        boolean installed = addonHandlers.stream().anyMatch(
                handler -> handler.supports(addonEntry.type, addonEntry.contentType) && handler.isInstalled(fullId));

        Map<String, Object> properties = new HashMap<>();
        if (addonEntry.url.endsWith(".jar")) {
            properties.put("jar_download_url", addonEntry.url);
        }
        if (addonEntry.url.endsWith(".kar")) {
            properties.put("kar_download_url", addonEntry.url);
        }
        if (addonEntry.url.endsWith(".json")) {
            properties.put("json_download_url", addonEntry.url);
        }
        if (addonEntry.url.endsWith(".yaml")) {
            properties.put("yaml_download_url", addonEntry.url);
        }

        return Addon.create(fullId).withType(addonEntry.type).withInstalled(installed)
                .withDetailedDescription(addonEntry.description).withContentType(addonEntry.contentType)
                .withAuthor(addonEntry.author).withVersion(addonEntry.version).withLabel(addonEntry.title)
                .withMaturity(addonEntry.maturity).withProperties(properties).withLink(addonEntry.link).build();
    }

    private void postInstalledEvent(String extensionId) {
        Event event = AddonEventFactory.createAddonInstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private void postUninstalledEvent(String extensionId) {
        Event event = AddonEventFactory.createAddonUninstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private void postFailureEvent(String extensionId, String msg) {
        Event event = AddonEventFactory.createAddonFailureEvent(extensionId, msg);
        eventPublisher.post(event);
    }
}