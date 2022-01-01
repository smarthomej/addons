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
package org.smarthomej.binding.viessmann.internal.handler;

import static org.smarthomej.binding.viessmann.internal.ViessmannBindingConstants.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.viessmann.internal.ViessmannDiscoveryService;
import org.smarthomej.binding.viessmann.internal.api.ViessmannApi;
import org.smarthomej.binding.viessmann.internal.config.BridgeConfiguration;
import org.smarthomej.binding.viessmann.internal.dto.device.DeviceDTO;
import org.smarthomej.binding.viessmann.internal.dto.device.DeviceData;
import org.smarthomej.binding.viessmann.internal.dto.features.FeatureDataDTO;
import org.smarthomej.binding.viessmann.internal.dto.features.FeaturesDTO;

import com.google.gson.JsonSyntaxException;

/**
 * The {@link ViessmannBridgeHandler} is responsible for handling the api connection.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class ViessmannBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HttpClient httpClient;

    private @NonNullByDefault({}) ViessmannApi api;

    protected @Nullable ViessmannDiscoveryService discoveryService;

    private int apiCalls;
    private boolean countReset = true;

    private @Nullable String newInstallationId;
    private @Nullable String newGatewaySerial;

    private @Nullable ScheduledFuture<?> viessmannBridgePollingJob;
    private @Nullable ScheduledFuture<?> viessmannBridgeLimitJob;

    public @Nullable List<DeviceData> devicesData;
    protected volatile List<String> devicesList = new ArrayList<>();
    protected volatile List<String> pollingDevicesList = new ArrayList<>();

    private BridgeConfiguration config = new BridgeConfiguration();

    public ViessmannBridgeHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        this.httpClient = httpClient;
    }

    public void setInstallationGatewayId(String newInstallation, String newGateway) {
        newInstallationId = newInstallation;
        newGatewaySerial = newGateway;
    }

    /**
     * get the devices list (needed for discovery)
     *
     * @return a list of the all devices
     */
    public List<String> getDevicesList() {
        // return a copy of the list, so we don't run into concurrency problems
        return new ArrayList<>(devicesList);
    }

    public void setPollingDevice(String deviceId) {
        if (!pollingDevicesList.contains(deviceId)) {
            pollingDevicesList.add(deviceId);
        }
    }

    public void unsetPollingDevice(String deviceId) {
        if (pollingDevicesList.contains(deviceId)) {
            pollingDevicesList.remove(deviceId);
        }
    }

    private void setConfigInstallationGatewayId() {
        Configuration conf = editConfiguration();
        conf.put("installationId", newInstallationId);
        conf.put("gatewaySerial", newGatewaySerial);
        updateConfiguration(conf);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Nothing to handle here currently
    }

    @Override
    public void dispose() {
        stopViessmannBridgePolling();
        stopViessmannBridgeLimitReset();
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(ViessmannDiscoveryService.class);
    }

    @Override
    public void initialize() {
        logger.debug("Initialize Viessmann Accountservice");

        BridgeConfiguration config = getConfigAs(BridgeConfiguration.class);
        this.config = config;
        apiCalls = 0;
        newInstallationId = "";
        newGatewaySerial = "";
        api = new ViessmannApi(this, this.config.apiKey, httpClient, this.config.user, this.config.password,
                this.config.installationId, this.config.gatewaySerial);
        if (this.config.installationId.isEmpty() || this.config.gatewaySerial.isEmpty()) {
            setConfigInstallationGatewayId();
        }
        getAllDevices();
        if (!devicesList.isEmpty()) {
            updateBridgeStatus(ThingStatus.ONLINE);
            startViessmannBridgePolling(getPollingInterval());
        }
    }

    public void getAllDevices() {
        logger.trace("Loading Device List from Viessmann Bridge");
        DeviceDTO allDevices = api.getAllDevices();
        countApiCalls();
        if (allDevices != null) {
            devicesData = allDevices.data;
            if (devicesData == null) {
                logger.warn("Device list is empty.");
            } else {
                for (DeviceData deviceData : allDevices.data) {
                    String deviceId = deviceData.id;
                    String deviceType = deviceData.deviceType;
                    if (!devicesList.contains(deviceId)) {
                        devicesList.add(deviceId);
                    }
                    logger.trace("Device ID: {}, Type: {}", deviceId, deviceType);
                }
            }
        }
    }

    public boolean setData(@Nullable String url, @Nullable String json) {
        if (url != null && json != null) {
            countApiCalls();
            return api.setData(url, json);
        }
        return false;
    }

    private Integer getPollingInterval() {
        if (this.config.pollingInterval > 0) {
            return this.config.pollingInterval;
        } else {
            Integer interval = (86400 / (this.config.apiCallLimit - this.config.bufferApiCommands) * devicesList.size())
                    + 1;
            return interval;
        }
    }

    private void countApiCalls() {
        apiCalls++;
        updateState(COUNT_API_CALLS, DecimalType.valueOf(Integer.toString(apiCalls)));
    }

    private void checkResetApiCalls() {
        LocalTime time = LocalTime.now();
        if (time.isAfter(LocalTime.of(00, 00, 01)) && (time.isBefore(LocalTime.of(01, 00, 00)))) {
            if (countReset) {
                logger.debug("Resettig API call counts");
                apiCalls = 0;
                countReset = false;
            }
        } else {
            countReset = true;
        }
    }

    private void pollingFeatures() {
        List<String> devices = pollingDevicesList;
        if (devices != null) {
            for (String deviceId : devices) {
                logger.debug("Loading featueres from Device ID: {}", deviceId);
                getAllFeaturesByDeviceId(deviceId);
            }
        }
    }

    public void getAllFeaturesByDeviceId(String deviceId) {
        try {
            FeaturesDTO allFeatures = api.getAllFeatures(deviceId);
            countApiCalls();
            if (allFeatures != null) {
                List<FeatureDataDTO> featuresData = allFeatures.data;
                for (FeatureDataDTO featureDataDTO : featuresData) {
                    notifyChildHandlers(featureDataDTO);
                }
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            logger.warn("Parsing Viessmann response fails: {}", e.getMessage());
        }
    }

    private void startViessmannBridgePolling(Integer pollingIntervalS) {
        ScheduledFuture<?> currentPollingJob = viessmannBridgePollingJob;
        if (currentPollingJob == null) {
            viessmannBridgePollingJob = scheduler.scheduleWithFixedDelay(() -> {
                logger.debug("Refresh job scheduled to run every {} seconds for '{}'", pollingIntervalS,
                        getThing().getUID());
                api.checkExpiringToken();
                checkResetApiCalls();
                pollingFeatures();
            }, 1, pollingIntervalS, TimeUnit.SECONDS);
        }
    }

    private void startViessmannBridgeLimitReset(Long delay) {
        ScheduledFuture<?> currentPollingJob = viessmannBridgeLimitJob;
        if (currentPollingJob == null) {
            viessmannBridgeLimitJob = scheduler.scheduleWithFixedDelay(() -> {
                logger.debug("Resetting limit and reconnect for '{}'", getThing().getUID());
                api.checkExpiringToken();
                checkResetApiCalls();
                getAllDevices();
                if (!devicesList.isEmpty()) {
                    updateBridgeStatus(ThingStatus.ONLINE);
                    startViessmannBridgePolling(getPollingInterval());
                    stopViessmannBridgeLimitReset();
                }
            }, delay, 120, TimeUnit.SECONDS);
        }
    }

    public void stopViessmannBridgePolling() {
        ScheduledFuture<?> currentPollingJob = viessmannBridgePollingJob;
        if (currentPollingJob != null) {
            currentPollingJob.cancel(true);
            viessmannBridgePollingJob = null;
        }
    }

    public void stopViessmannBridgeLimitReset() {
        ScheduledFuture<?> currentPollingJob = viessmannBridgeLimitJob;
        if (currentPollingJob != null) {
            currentPollingJob.cancel(true);
            viessmannBridgeLimitJob = null;
        }
    }

    public void waitForApiCallLimitReset(Long resetLimitMillis) {
        stopViessmannBridgePolling();
        Long delay = (resetLimitMillis - Instant.now().toEpochMilli()) / 1000;
        stopViessmannBridgeLimitReset();
        startViessmannBridgeLimitReset(delay);
    }

    /**
     * Notify appropriate child thing handlers of an Viessmann message by calling their handleUpdate() methods.
     *
     * @param msg message to forward to child handler(s)
     */
    private void notifyChildHandlers(FeatureDataDTO msg) {
        for (Thing thing : getThing().getThings()) {
            ViessmannThingHandler handler = (ViessmannThingHandler) thing.getHandler();
            if (handler instanceof DeviceHandler && msg instanceof FeatureDataDTO) {
                handler.handleUpdate(msg);
            }
        }
    }

    public void updateBridgeStatus(ThingStatus status) {
        updateStatus(status);
    }

    public void updateBridgeStatus(ThingStatus status, ThingStatusDetail statusDetail, String statusMessage) {
        updateStatus(status, statusDetail, statusMessage);
    }
}
