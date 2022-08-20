/**
 * Copyright (c) 2021-2022 Contributors to the SmartHome/J project
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
package org.smarthomej.binding.tuya.internal;

import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CONFIG_DEVICE_ID;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CONFIG_LOCAL_KEY;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CONFIG_PRODUCT_ID;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.PROPERTY_CATEGORY;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.PROPERTY_MAC;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.THING_TYPE_TUYA_DEVICE;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.storage.Storage;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.cloud.TuyaOpenAPI;
import org.smarthomej.binding.tuya.internal.cloud.dto.DeviceListInfo;
import org.smarthomej.binding.tuya.internal.handler.ProjectHandler;
import org.smarthomej.binding.tuya.internal.util.SchemaDp;

import com.google.gson.Gson;

/**
 * The {@link TuyaDiscoveryService} implements the discovery service for Tuya devices from the cloud
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TuyaDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_TUYA_DEVICE);
    private static final int SEARCH_TIME = 5;

    private final Logger logger = LoggerFactory.getLogger(TuyaDiscoveryService.class);
    private final Gson gson = new Gson();

    private @Nullable ProjectHandler bridgeHandler;
    private @NonNullByDefault({}) Storage<String> storage;
    private @Nullable ScheduledFuture<?> discoveryJob;

    public TuyaDiscoveryService() {
        super(SUPPORTED_THING_TYPES, SEARCH_TIME);
    }

    @Override
    protected void startScan() {
        ProjectHandler bridgeHandler = this.bridgeHandler;
        if (bridgeHandler == null) {
            logger.warn("Could not start discovery, bridge handler not set");
            return;
        }

        TuyaOpenAPI api = bridgeHandler.getApi();
        if (!api.isConnected()) {
            logger.debug("Tried to start scan but API for bridge '{}' is not connected.",
                    bridgeHandler.getThing().getUID());
            return;
        }

        processDeviceResponse(List.of(), api, bridgeHandler, 0);
    }

    private void processDeviceResponse(List<DeviceListInfo> deviceList, TuyaOpenAPI api, ProjectHandler bridgeHandler,
            int page) {
        deviceList.forEach(device -> processDevice(device, api));
        if (page == 0 || deviceList.size() == 100) {
            int nextPage = page + 1;
            bridgeHandler.getAllDevices(nextPage)
                    .thenAccept(nextDeviceList -> processDeviceResponse(nextDeviceList, api, bridgeHandler, nextPage));
        }
    }

    private void processDevice(DeviceListInfo device, TuyaOpenAPI api) {
        api.getFactoryInformation(List.of(device.id)).thenAccept(fiList -> {
            ThingUID thingUid = new ThingUID(THING_TYPE_TUYA_DEVICE, device.id);
            String deviceMac = fiList.stream().filter(fi -> fi.id.equals(device.id)).findAny().map(fi -> fi.mac)
                    .orElse("");

            Map<String, Object> properties = new HashMap<>();
            properties.put(PROPERTY_CATEGORY, device.category);
            properties.put(PROPERTY_MAC, Objects.requireNonNull(deviceMac).replaceAll("(..)(?!$)", "$1:"));
            properties.put(CONFIG_LOCAL_KEY, device.localKey);
            properties.put(CONFIG_DEVICE_ID, device.id);
            properties.put(CONFIG_PRODUCT_ID, device.productId);

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUid).withLabel(device.name)
                    .withRepresentationProperty(CONFIG_DEVICE_ID).withProperties(properties).build();

            api.getDeviceSchema(device.id).thenAccept(schema -> {
                List<SchemaDp> functionDps = schema.functions.stream().map(fcn -> SchemaDp.fromRemoteSchema(gson, fcn))
                        .collect(Collectors.toList());
                List<SchemaDp> statusDps = schema.status.stream()
                        .filter(status -> functionDps.stream().noneMatch(functionDp -> functionDp.id == status.dp_id))
                        .map(status -> SchemaDp.fromRemoteSchema(gson, status)).collect(Collectors.toList());
                List<SchemaDp> schemaDps = Stream.of(functionDps, statusDps).flatMap(Collection::stream)
                        .collect(Collectors.toList());
                storage.put(device.id, gson.toJson(schemaDps));
            });
            thingDiscovered(discoveryResult);
        });
    }

    @Override
    protected synchronized void stopScan() {
        removeOlderResults(getTimestampOfLastScan());
        super.stopScan();
    }

    @Override
    public void setThingHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof ProjectHandler) {
            this.bridgeHandler = (ProjectHandler) thingHandler;
            this.storage = ((ProjectHandler) thingHandler).getStorage();
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    public void activate() {
        super.activate(null);
    }

    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime());

        super.deactivate();
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES;
    }

    @Override
    public void startBackgroundDiscovery() {
        ScheduledFuture<?> discoveryJob = this.discoveryJob;
        if (discoveryJob == null || discoveryJob.isCancelled()) {
            this.discoveryJob = scheduler.scheduleWithFixedDelay(this::startScan, 1, 5, TimeUnit.MINUTES);
        }
    }

    @Override
    public void stopBackgroundDiscovery() {
        ScheduledFuture<?> discoveryJob = this.discoveryJob;
        if (discoveryJob != null) {
            discoveryJob.cancel(true);
            this.discoveryJob = null;
        }
    }
}
