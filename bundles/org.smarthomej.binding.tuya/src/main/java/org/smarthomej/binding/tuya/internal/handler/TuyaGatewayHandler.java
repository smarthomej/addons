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
package org.smarthomej.binding.tuya.internal.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.TuyaBindingConstants;
import org.smarthomej.binding.tuya.internal.local.DeviceStatusListener;
import org.smarthomej.binding.tuya.internal.local.TuyaDeviceManager;
import org.smarthomej.binding.tuya.internal.local.TuyaDeviceManagerFactory;

/**
 * The {@link TuyaGatewayHandler} gateway bridge handler
 *
 * @author Vitalii Herhel - Initial contribution
 */
@NonNullByDefault
public class TuyaGatewayHandler extends BaseBridgeHandler implements DeviceStatusListener {

    private final Logger logger = LoggerFactory.getLogger(TuyaGatewayHandler.class);
    private final TuyaDeviceManagerFactory tuyaDeviceManagerFactory;
    private @Nullable TuyaDeviceManager tuyaDeviceManager;

    private final Map<String, TuyaSubDeviceHandler> subDeviceHandlers = Collections.synchronizedMap(new HashMap<>());

    public TuyaGatewayHandler(Bridge bridge, TuyaDeviceManagerFactory tuyaDeviceManagerFactory) {
        super(bridge);
        this.tuyaDeviceManagerFactory = tuyaDeviceManagerFactory;
    }

    @Override
    public void initialize() {
        tuyaDeviceManager = tuyaDeviceManagerFactory.create(getThing(), this, scheduler);
    }

    @Override
    public void childHandlerInitialized(ThingHandler thingHandler, Thing thing) {
        if (thingHandler instanceof TuyaSubDeviceHandler) {
            registerSubDevice((TuyaSubDeviceHandler) thingHandler);
        } else {
            logger.warn("Unsupported sub device handler: " + thingHandler.getClass());
        }
    }

    public void registerSubDevice(TuyaSubDeviceHandler subDeviceHandler) {
        subDeviceHandler.setTuyaDeviceManager(tuyaDeviceManager);
        Optional<String> deviceUuid = Optional
                .ofNullable(subDeviceHandler.getThing().getConfiguration().get(TuyaBindingConstants.CONFIG_DEVICE_UUID))
                .map(Objects::toString);
        if (deviceUuid.isPresent()) {
            subDeviceHandlers.put(deviceUuid.get(), subDeviceHandler);
        } else {
            logger.warn("Sub device handler " + subDeviceHandler.getThing().getUID() + " doesn't have deviceUuid");
        }
    }

    @Override
    public void childHandlerDisposed(ThingHandler thingHandler, Thing thing) {
        if (thingHandler instanceof TuyaSubDeviceHandler) {
            deregisterSubDevice((TuyaSubDeviceHandler) thingHandler);
        } else {
            logger.warn("Unsupported sub device handler: " + thingHandler.getClass());
        }
    }

    public void deregisterSubDevice(TuyaSubDeviceHandler subDeviceHandler) {
        subDeviceHandler.setTuyaDeviceManager(null);
        Optional<String> deviceUuid = Optional
                .ofNullable(subDeviceHandler.getThing().getConfiguration().get(TuyaBindingConstants.CONFIG_DEVICE_UUID))
                .map(Objects::toString);
        if (deviceUuid.isPresent()) {
            subDeviceHandlers.remove(deviceUuid.get());
        } else {
            logger.warn("Sub device handler " + subDeviceHandler.getThing().getUID() + " doesn't have deviceUuid");
        }
    }

    @Override
    public @Nullable Bridge getBridge() {
        return super.getBridge();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void processDeviceStatus(@Nullable String cid, Map<Integer, Object> deviceStatus) {
        TuyaSubDeviceHandler tuyaSubDeviceHandler = subDeviceHandlers.get(cid);
        if (tuyaSubDeviceHandler != null) {
            tuyaSubDeviceHandler.processDeviceStatus(cid, deviceStatus);
        } else {
            logger.debug("Received sub device status, but there is no sub device with cid " + cid);
        }
    }

    @Override
    public void onConnected() {
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void onDisconnected(ThingStatusDetail thingStatusDetail, @Nullable String reason) {
        updateStatus(ThingStatus.OFFLINE, thingStatusDetail, reason);
        subDeviceHandlers.values().forEach(s -> s.onDisconnected(thingStatusDetail, reason));
    }

    @Override
    public void dispose() {
        TuyaDeviceManager tuyaDeviceManager = this.tuyaDeviceManager;
        if (tuyaDeviceManager != null) {
            tuyaDeviceManager.dispose();
        }
    }
}
