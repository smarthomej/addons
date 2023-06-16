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
package org.smarthomej.binding.tuya.internal.local;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.config.DeviceConfiguration;
import org.smarthomej.binding.tuya.internal.local.dto.DeviceInfo;

import com.google.gson.Gson;

import io.netty.channel.EventLoopGroup;

/**
 * {@link TuyaDevice} manager. It creates {@link TuyaDevice} and keeps it connected
 *
 * @author Vitalii Herhel - Initial contribution
 */
public class TuyaDeviceManager implements DeviceInfoSubscriber, DeviceStatusListener {

    private final Logger logger = LoggerFactory.getLogger(TuyaDeviceManager.class);

    private final Thing thing;
    private final DeviceStatusListener deviceStatusListener;
    private final Gson gson;
    private final EventLoopGroup eventLoopGroup;
    private final DeviceConfiguration configuration;
    private final UdpDiscoveryListener udpDiscoveryListener;
    private final ScheduledExecutorService scheduler;
    private @Nullable TuyaDevice tuyaDevice;
    private @Nullable ScheduledFuture<?> reconnectFuture;
    private boolean disposing = false;

    public TuyaDeviceManager(Thing thing, DeviceStatusListener deviceStatusListener, Gson gson,
            EventLoopGroup eventLoopGroup, DeviceConfiguration configuration, UdpDiscoveryListener udpDiscoveryListener,
            ScheduledExecutorService scheduler) {
        this.thing = thing;
        this.deviceStatusListener = deviceStatusListener;
        this.gson = gson;
        this.eventLoopGroup = eventLoopGroup;
        this.configuration = configuration;
        this.udpDiscoveryListener = udpDiscoveryListener;
        this.scheduler = scheduler;
        init();
    }

    private void init() {
        if (!configuration.ip.isBlank()) {
            deviceInfoChanged(new DeviceInfo(configuration.ip, configuration.protocol));
        } else {
            deviceStatusListener.onDisconnected(ThingStatusDetail.CONFIGURATION_PENDING, "Waiting for IP address");
            udpDiscoveryListener.registerListener(configuration.deviceId, this);
        }
    }

    @Override
    public void deviceInfoChanged(DeviceInfo deviceInfo) {
        logger.info("Configuring IP address '{}' for thing '{}'.", deviceInfo, thing.getUID());

        TuyaDevice tuyaDevice = this.tuyaDevice;
        if (tuyaDevice != null) {
            tuyaDevice.dispose();
        }
        deviceStatusListener.onDisconnected(ThingStatusDetail.NONE, "");

        this.tuyaDevice = new TuyaDevice(gson, this, eventLoopGroup, configuration.deviceId,
                configuration.localKey.getBytes(StandardCharsets.UTF_8), deviceInfo.ip, deviceInfo.protocolVersion);
    }

    public TuyaDevice getTuyaDevice() {
        return tuyaDevice;
    }

    @Override
    public void processDeviceStatus(@Nullable String cid, Map<Integer, Object> deviceStatus) {
        deviceStatusListener.processDeviceStatus(cid, deviceStatus);
    }

    @Override
    public void onConnected() {
        deviceStatusListener.onConnected();
    }

    @Override
    public void onDisconnected(ThingStatusDetail thingStatusDetail, @Nullable String reason) {
        TuyaDevice tuyaDevice = this.tuyaDevice;
        ScheduledFuture<?> reconnectFuture = this.reconnectFuture;
        // only re-connect if a device is present, we are not disposing the thing and either the reconnectFuture is
        // empty or already done
        if (tuyaDevice != null && !disposing && (reconnectFuture == null || reconnectFuture.isDone())) {
            this.reconnectFuture = scheduler.schedule(tuyaDevice::connect, 5000, TimeUnit.MILLISECONDS);
        }
        deviceStatusListener.onDisconnected(thingStatusDetail, reason);
    }

    public void dispose() {
        disposing = true;
        ScheduledFuture<?> future = reconnectFuture;
        if (future != null) {
            future.cancel(true);
        }
        if (configuration.ip.isEmpty()) {
            // unregister listener only if IP is not fixed
            udpDiscoveryListener.unregisterListener(this);
        }
        TuyaDevice tuyaDevice = this.tuyaDevice;
        if (tuyaDevice != null) {
            tuyaDevice.dispose();
            this.tuyaDevice = null;
        }
    }
}
