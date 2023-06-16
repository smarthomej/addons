/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CONFIG_DEVICE_UUID;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CONFIG_LOCAL_KEY;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.CONFIG_PRODUCT_ID;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.PROPERTY_CATEGORY;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.PROPERTY_MAC;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.THING_TYPE_TUYA_DEVICE;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.THING_TYPE_TUYA_GATEWAY;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.THING_TYPE_TUYA_SUB_DEVICE;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.cloud.TuyaOpenAPI;
import org.smarthomej.binding.tuya.internal.cloud.dto.DeviceListInfo;
import org.smarthomej.binding.tuya.internal.cloud.dto.SubDeviceInfo;
import org.smarthomej.binding.tuya.internal.handler.ProjectHandler;
import org.smarthomej.binding.tuya.internal.schema.DeviceSchemaMapper;
import org.smarthomej.binding.tuya.internal.schema.SchemaRegistry;
import org.smarthomej.binding.tuya.internal.util.SchemaDp;

import com.google.gson.Gson;

/**
 * The {@link TuyaDiscoveryService} implements the discovery service for Tuya devices from the cloud
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TuyaDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_TUYA_DEVICE,
            THING_TYPE_TUYA_GATEWAY, THING_TYPE_TUYA_SUB_DEVICE);
    private static final int SEARCH_TIME = 5;

    private final Logger logger = LoggerFactory.getLogger(TuyaDiscoveryService.class);
    private final Gson gson = new Gson();

    private @Nullable ProjectHandler bridgeHandler;
    private @NonNullByDefault({}) SchemaRegistry schemaRegistry;
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
        bridgeHandler.getThingRegistry().stream().filter(t -> t.getThingTypeUID().equals(THING_TYPE_TUYA_GATEWAY))
                .forEach(t -> discoverSubDevices(t, api));
    }

    private void discoverSubDevices(Thing gatewayThing, TuyaOpenAPI api) {
        Object gatewayId = gatewayThing.getConfiguration().get(CONFIG_DEVICE_ID);
        if (gatewayId == null) {
            logger.debug("There is no device id in gateway {} properties", gatewayThing.getUID());
            return;
        }
        api.getSubDevices(gatewayId.toString())
                .thenAccept(subDevices -> subDevices.forEach(d -> processSubDevice(d, api, gatewayThing.getUID())));
    }

    private void processSubDevice(SubDeviceInfo subDeviceInfo, TuyaOpenAPI api, ThingUID gatewayThingUid) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_CATEGORY, subDeviceInfo.category);
        properties.put(CONFIG_DEVICE_UUID, subDeviceInfo.nodeId);
        properties.put(CONFIG_PRODUCT_ID, subDeviceInfo.productId);
        properties.put(CONFIG_DEVICE_ID, subDeviceInfo.id);

        ThingUID thingUid = new ThingUID(THING_TYPE_TUYA_SUB_DEVICE, gatewayThingUid, subDeviceInfo.id);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUid).withLabel(subDeviceInfo.name)
                .withBridge(gatewayThingUid).withRepresentationProperty(CONFIG_DEVICE_ID).withProperties(properties)
                .build();

        refreshSchemaForDevice(subDeviceInfo.id, api);
        thingDiscovered(discoveryResult);
    }

    private void refreshSchemaForDevice(String deviceId, TuyaOpenAPI api) {
        api.getDeviceSchema(deviceId).thenAccept(schema -> {
            DeviceSchemaMapper deviceSchemaMapper = new DeviceSchemaMapper(gson);
            List<SchemaDp> schemaDps = deviceSchemaMapper.covert(schema);
            SchemaRegistry schemaRegistry = this.schemaRegistry;
            schemaRegistry.add(deviceId, schemaDps);
        });
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
            String deviceMac = fiList.stream().filter(fi -> fi.id.equals(device.id)).findAny().map(fi -> fi.mac)
                    .orElse("");
            deviceMac = Objects.requireNonNull(deviceMac).replaceAll("(..)(?!$)", "$1:");
            if ("wfcon".equals(device.category)) {
                processGateway(device, api, deviceMac);
            } else if (!device.subDevice) {
                processSimpleDevice(device, api, deviceMac);
            }
        });
    }

    private void processGateway(DeviceListInfo device, TuyaOpenAPI api, String deviceMac) {
        ThingUID thingUid = new ThingUID(THING_TYPE_TUYA_GATEWAY, device.id);

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_CATEGORY, device.category);
        properties.put(PROPERTY_MAC, deviceMac);
        properties.put(CONFIG_LOCAL_KEY, device.localKey);
        properties.put(CONFIG_DEVICE_ID, device.id);
        properties.put(CONFIG_DEVICE_UUID, device.uuid);
        properties.put(CONFIG_PRODUCT_ID, device.productId);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUid).withLabel(device.name)
                .withRepresentationProperty(CONFIG_DEVICE_ID).withProperties(properties).build();

        thingDiscovered(discoveryResult);
    }

    private void processSimpleDevice(DeviceListInfo device, TuyaOpenAPI api, String deviceMac) {
        ThingUID thingUid = new ThingUID(THING_TYPE_TUYA_DEVICE, device.id);

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_CATEGORY, device.category);
        properties.put(PROPERTY_MAC, deviceMac);
        properties.put(CONFIG_LOCAL_KEY, device.localKey);
        properties.put(CONFIG_DEVICE_ID, device.id);
        properties.put(CONFIG_DEVICE_UUID, device.uuid);
        properties.put(CONFIG_PRODUCT_ID, device.productId);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUid).withLabel(device.name)
                .withRepresentationProperty(CONFIG_DEVICE_ID).withProperties(properties).build();

        refreshSchemaForDevice(device.id, api);
        thingDiscovered(discoveryResult);
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
            this.schemaRegistry = ((ProjectHandler) thingHandler).getSchemaRegistry();
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
