/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.amazonechocontrol.internal.discovery;

import static org.smarthomej.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.*;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.dto.DeviceTO;
import org.smarthomej.binding.amazonechocontrol.internal.dto.EnabledFeedTO;
import org.smarthomej.binding.amazonechocontrol.internal.handler.AccountHandler;

/**
 * The {@link AmazonEchoDiscovery} is responsible for discovering echo devices on
 * the amazon account specified in the binding.
 *
 * @author Michael Geramb - Initial contribution
 * @author Jan N. Klug - Refactored to ThingHandlerService
 */
@NonNullByDefault
public class AmazonEchoDiscovery extends AbstractDiscoveryService implements ThingHandlerService {
    private static final int BACKGROUND_INTERVAL = 10; // in seconds
    private @Nullable AccountHandler accountHandler;
    private final Logger logger = LoggerFactory.getLogger(AmazonEchoDiscovery.class);
    private final Set<List<EnabledFeedTO>> discoveredFlashBriefings = new HashSet<>();

    private @Nullable ScheduledFuture<?> startScanStateJob;
    private @Nullable Long activateTimeStamp;

    public AmazonEchoDiscovery() {
        super(SUPPORTED_ECHO_THING_TYPES_UIDS, 5);
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
        activate(new HashMap<>());
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
        stopScanJob();
        final Long activateTimeStamp = this.activateTimeStamp;
        if (activateTimeStamp != null) {
            removeOlderResults(activateTimeStamp);
        }
        setDevices(accountHandler.updateDeviceList());

        List<EnabledFeedTO> currentFlashBriefingConfiguration = accountHandler.updateFlashBriefingHandlers();
        discoverFlashBriefingProfiles(currentFlashBriefingConfiguration);
    }

    protected void startAutomaticScan() {
        AccountHandler accountHandler = this.accountHandler;
        if (accountHandler == null) {
            return;
        }
        if (!accountHandler.getThing().getThings().isEmpty()) {
            stopScanJob();
            return;
        }
        Connection connection = accountHandler.getConnection();
        // do discovery only if logged in and last login is more than 10 s ago
        Date verifyTime = connection.getVerifyTime();
        if (verifyTime == null || System.currentTimeMillis() < (verifyTime.getTime() + 10000)) {
            return;
        }

        startScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        stopScanJob();
        startScanStateJob = scheduler.scheduleWithFixedDelay(this::startAutomaticScan, BACKGROUND_INTERVAL,
                BACKGROUND_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        stopScanJob();
    }

    void stopScanJob() {
        @Nullable
        ScheduledFuture<?> currentStartScanStateJob = startScanStateJob;
        if (currentStartScanStateJob != null) {
            currentStartScanStateJob.cancel(false);
            startScanStateJob = null;
        }
    }

    @Override
    @Activate
    public void activate(@Nullable Map<String, Object> config) {
        super.activate(config);
        if (config != null) {
            modified(config);
        }
        if (activateTimeStamp == null) {
            activateTimeStamp = new Date().getTime();
        }
    }

    private synchronized void setDevices(List<DeviceTO> deviceList) {
        AccountHandler accountHandler = this.accountHandler;
        if (accountHandler == null) {
            return;
        }
        for (DeviceTO device : deviceList) {
            String serialNumber = device.serialNumber;
            if (serialNumber != null) {
                String deviceFamily = device.deviceFamily;
                if (deviceFamily != null) {
                    ThingTypeUID thingTypeId;
                    switch (deviceFamily) {
                        case "ECHO":
                            thingTypeId = THING_TYPE_ECHO;
                            break;
                        case "ROOK":
                            thingTypeId = THING_TYPE_ECHO_SPOT;
                            break;
                        case "KNIGHT":
                            thingTypeId = THING_TYPE_ECHO_SHOW;
                            break;
                        case "WHA":
                            thingTypeId = THING_TYPE_ECHO_WHA;
                            break;
                        default:
                            logger.debug("Unknown thing type '{}'", deviceFamily);
                            continue;
                    }

                    ThingUID bridgeThingUID = accountHandler.getThing().getUID();
                    ThingUID thingUID = new ThingUID(thingTypeId, bridgeThingUID, serialNumber);

                    DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withLabel(device.accountName)
                            .withProperty(DEVICE_PROPERTY_SERIAL_NUMBER, serialNumber)
                            .withProperty(DEVICE_PROPERTY_FAMILY, deviceFamily)
                            .withProperty(DEVICE_PROPERTY_DEVICE_TYPE_ID,
                                    Objects.requireNonNullElse(device.deviceType, "<unknown>"))
                            .withRepresentationProperty(DEVICE_PROPERTY_SERIAL_NUMBER).withBridge(bridgeThingUID)
                            .build();

                    logger.debug("Device [{}: {}] found. Mapped to thing type {}", device.deviceFamily, serialNumber,
                            thingTypeId.getAsString());

                    thingDiscovered(result);
                }
            }
        }
    }

    private synchronized void discoverFlashBriefingProfiles(List<EnabledFeedTO> enabledFeeds) {
        AccountHandler accountHandler = this.accountHandler;
        if (accountHandler == null || enabledFeeds.isEmpty()) {
            return;
        }

        if (!discoveredFlashBriefings.contains(enabledFeeds)) {
            ThingUID bridgeThingUID = accountHandler.getThing().getUID();
            ThingUID freeThingUID = new ThingUID(THING_TYPE_FLASH_BRIEFING_PROFILE, bridgeThingUID,
                    Integer.toString(enabledFeeds.hashCode()));
            DiscoveryResult result = DiscoveryResultBuilder.create(freeThingUID).withLabel("FlashBriefing")
                    .withProperty(DEVICE_PROPERTY_FLASH_BRIEFING_PROFILE, enabledFeeds)
                    .withBridge(accountHandler.getThing().getUID()).build();
            logger.debug("Flash Briefing {} discovered", enabledFeeds);
            thingDiscovered(result);
            discoveredFlashBriefings.add(enabledFeeds);
        }
    }
}
