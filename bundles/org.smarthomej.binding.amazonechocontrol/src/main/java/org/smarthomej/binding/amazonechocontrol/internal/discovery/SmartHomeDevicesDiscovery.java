/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.amazonechocontrol.internal.discovery;

import static org.smarthomej.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.handler.AccountHandler;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevice;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevice.DriverIdentity;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDeviceAlias;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeGroups.SmartHomeGroup;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.SmartHomeBaseDevice;
import org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants;

/**
 * @author Lukas Knoeller - Initial contribution
 * @author Jan N. Klug - Refactored to ThingHandlerService
 */
@NonNullByDefault
public class SmartHomeDevicesDiscovery extends AbstractDiscoveryService implements ThingHandlerService {
    private @Nullable AccountHandler accountHandler;
    private final Logger logger = LoggerFactory.getLogger(SmartHomeDevicesDiscovery.class);

    private @Nullable ScheduledFuture<?> discoveryJob;

    public SmartHomeDevicesDiscovery() {
        super(SUPPORTED_SMART_HOME_THING_TYPES_UIDS, 10);
    }

    @Override
    public void setThingHandler(ThingHandler thingHandler) {
        this.accountHandler = (AccountHandler) thingHandler;
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return accountHandler;
    }

    public void activate() {
        super.activate(null);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void startScan() {
        AccountHandler accountHandler = this.accountHandler;
        if (accountHandler == null) {
            return;
        }
        setSmartHomeDevices(accountHandler.updateSmartHomeDeviceList(false));
    }

    @Override
    protected void stopScan() {
        removeOlderResults(getTimestampOfLastScan());
        super.stopScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        ScheduledFuture<?> discoveryJob = this.discoveryJob;
        if (discoveryJob == null || discoveryJob.isCancelled()) {
            this.discoveryJob = scheduler.scheduleWithFixedDelay(this::startScan, 3, 60, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> discoveryJob = this.discoveryJob;
        if (discoveryJob != null) {
            discoveryJob.cancel(true);
            this.discoveryJob = null;
        }
    }

    private synchronized void setSmartHomeDevices(List<SmartHomeBaseDevice> deviceList) {
        AccountHandler accountHandler = this.accountHandler;
        if (accountHandler == null) {
            return;
        }
        int smartHomeDeviceDiscoveryMode = accountHandler.getSmartHomeDevicesDiscoveryMode();
        if (smartHomeDeviceDiscoveryMode == 0) {
            return;
        }

        for (Object smartHomeDevice : deviceList) {
            ThingUID bridgeThingUID = accountHandler.getThing().getUID();
            ThingUID thingUID = null;
            String deviceName = null;
            Map<String, Object> props = new HashMap<>();

            if (smartHomeDevice instanceof JsonSmartHomeDevice) {
                JsonSmartHomeDevice shd = (JsonSmartHomeDevice) smartHomeDevice;
                logger.trace("Found SmartHome device: {}", shd);

                String entityId = shd.entityId;
                if (entityId == null) {
                    // No entity id
                    continue;
                }
                String id = shd.findId();
                if (id == null) {
                    // No id
                    continue;
                }
                boolean isSkillDevice = false;
                DriverIdentity driverIdentity = shd.driverIdentity;
                isSkillDevice = driverIdentity != null && "SKILL".equals(driverIdentity.namespace);

                if (smartHomeDeviceDiscoveryMode == 1 && isSkillDevice) {
                    // Connected through skill and we want direct only
                    continue;
                }
                if (smartHomeDeviceDiscoveryMode == 2 && "openHAB".equalsIgnoreCase(shd.manufacturerName)) {
                    // openHAB device and we want non-openHAB only
                    continue;
                }

                if (shd.getCapabilities().stream()
                        .noneMatch(capability -> Constants.SUPPORTED_INTERFACES.contains(capability.interfaceName))) {
                    // No supported interface found
                    continue;
                }

                thingUID = new ThingUID(THING_TYPE_SMART_HOME_DEVICE, bridgeThingUID, entityId.replace(".", "-"));

                List<JsonSmartHomeDeviceAlias> aliases = shd.aliases;
                if (shd.manufacturerName != null && shd.manufacturerName.startsWith("Amazon")) {
                    List<@Nullable String> interfaces = shd.getCapabilities().stream().map(c -> c.interfaceName)
                            .collect(Collectors.toList());
                    if (driverIdentity != null && "SonarCloudService".equals(shd.driverIdentity.identifier)) {
                        if (interfaces.contains("Alexa.AcousticEventSensor")) {
                            deviceName = "Alexa Guard on " + shd.friendlyName;
                        } else if (interfaces.contains("Alexa.ColorController")) {
                            deviceName = "Alexa Color Controller on " + shd.friendlyName;
                        } else if (interfaces.contains("Alexa.PowerController")) {
                            deviceName = "Alexa Plug on " + shd.friendlyName;
                        } else {
                            deviceName = "Unknown Device on " + shd.friendlyName;
                        }
                    } else if (driverIdentity != null && "OnGuardSmartHomeBridgeService".equals(shd.driverIdentity.identifier)) {
                        deviceName = "Alexa Guard";
                    } else if (driverIdentity != null && "AlexaBridge".equals(shd.driverIdentity.namespace) && interfaces.contains("Alexa.AcousticEventSensor")) {
                        deviceName = "Alexa Guard on " + shd.friendlyName;
                    } else {
                        deviceName = "Unknown Device on " + shd.friendlyName;
                    }
                } else if (aliases != null && !aliases.isEmpty() && aliases.get(0).friendlyName != null) {
                    deviceName = aliases.get(0).friendlyName;
                } else {
                    deviceName = shd.friendlyName;
                }
                props.put(DEVICE_PROPERTY_ID, id);
                if (shd.alexaDeviceIdentifierList != null && !shd.alexaDeviceIdentifierList.isEmpty()) {
                    props.put(DEVICE_PROPERTY_DEVICE_IDENTIFIER_LIST, shd.alexaDeviceIdentifierList.stream().map(d -> d.dmsDeviceSerialNumber+" @ "+d.dmsDeviceTypeId).collect(
                            Collectors.joining(", ")));
                }
            } else if (smartHomeDevice instanceof SmartHomeGroup) {
                SmartHomeGroup shg = (SmartHomeGroup) smartHomeDevice;
                logger.trace("Found SmartHome device: {}", shg);

                String id = shg.findId();
                if (id == null) {
                    // No id
                    continue;
                }
                Set<JsonSmartHomeDevice> supportedChildren = SmartHomeDeviceHandler.getSupportedSmartHomeDevices(shg,
                        deviceList);
                if (supportedChildren.isEmpty()) {
                    // No children with a supported interface
                    continue;
                }
                thingUID = new ThingUID(THING_TYPE_SMART_HOME_DEVICE_GROUP, bridgeThingUID, id.replace(".", "-"));
                deviceName = shg.applianceGroupName;
                props.put(DEVICE_PROPERTY_ID, id);
            }

            if (thingUID != null) {
                DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withLabel(deviceName)
                        .withProperties(props).withBridge(bridgeThingUID).build();

                logger.debug("Device [{}] found.", deviceName);

                thingDiscovered(result);
            }
        }
    }
}
