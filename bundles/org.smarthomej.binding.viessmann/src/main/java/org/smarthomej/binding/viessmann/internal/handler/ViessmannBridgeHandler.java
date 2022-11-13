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
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.viessmann.internal.ViessmannDiscoveryService;
import org.smarthomej.binding.viessmann.internal.api.ViessmannApi;
import org.smarthomej.binding.viessmann.internal.config.BridgeConfiguration;
import org.smarthomej.binding.viessmann.internal.dto.device.DeviceDTO;
import org.smarthomej.binding.viessmann.internal.dto.device.DeviceData;
import org.smarthomej.binding.viessmann.internal.dto.events.EventsDTO;
import org.smarthomej.binding.viessmann.internal.dto.features.FeatureDataDTO;
import org.smarthomej.binding.viessmann.internal.dto.features.FeaturesDTO;
import org.smarthomej.commons.UpdatingBaseBridgeHandler;

import com.google.gson.JsonSyntaxException;

/**
 * The {@link ViessmannBridgeHandler} is responsible for handling the api connection.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class ViessmannBridgeHandler extends UpdatingBaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Set<String> ERROR_CHANNELS = Set.of("lastErrorMessage", "errorIsActive");

    private final HttpClient httpClient;
    private final @Nullable String callbackUrl;

    private @NonNullByDefault({}) ViessmannApi api;

    protected @Nullable ViessmannDiscoveryService discoveryService;

    private int apiCalls;
    private boolean countReset = true;

    private @Nullable String newInstallationId;
    private @Nullable String newGatewaySerial;

    private @Nullable ScheduledFuture<?> viessmannBridgePollingJob;
    private @Nullable ScheduledFuture<?> viessmannErrorsPollingJob;
    private @Nullable ScheduledFuture<?> viessmannBridgeLimitJob;

    public @Nullable List<DeviceData> devicesData;
    protected volatile List<String> devicesList = new ArrayList<>();
    protected volatile List<String> pollingDevicesList = new ArrayList<>();

    private BridgeConfiguration config = new BridgeConfiguration();

    public ViessmannBridgeHandler(Bridge bridge, HttpClient httpClient, @Nullable String callbackUrl) {
        super(bridge);
        this.httpClient = httpClient;
        this.callbackUrl = callbackUrl;
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
        pollingDevicesList.remove(deviceId);
    }

    private void setConfigInstallationGatewayId() {
        Configuration conf = editConfiguration();
        conf.put("installationId", newInstallationId);
        conf.put("gatewaySerial", newGatewaySerial);
        updateConfiguration(conf);
    }

    private boolean errorChannelsLinked() {
        return getThing().getChannels().stream()
                .anyMatch(c -> isLinked(c.getUID()) && ERROR_CHANNELS.contains(c.getUID().getId()));
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_RUN_QUERY_ONCE) && OnOffType.ON.equals(command)) {
            logger.debug("Received command: CHANNEL_RUN_QUERY_ONCE");
            pollingFeatures();
            updateState(CHANNEL_RUN_QUERY_ONCE, OnOffType.OFF);
        }
        if (channelUID.getId().equals(CHANNEL_RUN_ERROR_QUERY_ONCE) && OnOffType.ON.equals(command)) {
            logger.debug("Received command: CHANNEL_RUN_ERROR_QUERY_ONCE");
            getDeviceError();
            updateState(CHANNEL_RUN_ERROR_QUERY_ONCE, OnOffType.OFF);
        }
    }

    @Override
    public void dispose() {
        stopViessmannBridgePolling();
        stopViessmannErrorsPolling();
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
                this.config.installationId, this.config.gatewaySerial, callbackUrl);
        if (this.config.installationId.isEmpty() || this.config.gatewaySerial.isEmpty()) {
            setConfigInstallationGatewayId();
        }

        if (!config.disablePolling && errorChannelsLinked()) {
            startViessmannErrorsPolling(config.pollingIntervalErrors);
        }

        getAllDevices();
        if (!devicesList.isEmpty()) {
            updateBridgeStatus(ThingStatus.ONLINE);
            startViessmannBridgePolling(getPollingInterval(), 1);
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

    public void getDeviceError() {
        logger.trace("Loading error-list from Viessmann Bridge");
        EventsDTO errors = api.getSelectedEvents("device-error");
        countApiCalls();
        logger.trace("Errors:{}", errors);
        if (errors != null) {
            String state = errors.data.get(0).body.errorDescription;
            Boolean active = errors.data.get(0).body.active;
            updateState("lastErrorMessage", StringType.valueOf(state));
            updateState("errorIsActive", OnOffType.from(active));
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
            int errorApiCalls = 0;
            if (errorChannelsLinked()) {
                errorApiCalls = 1440 / this.config.pollingIntervalErrors;
            }
            return (86400 / (this.config.apiCallLimit - this.config.bufferApiCommands - errorApiCalls)
                    * devicesList.size()) + 1;
        }
    }

    private void countApiCalls() {
        apiCalls++;
        updateState(COUNT_API_CALLS, DecimalType.valueOf(Integer.toString(apiCalls)));
    }

    private void checkResetApiCalls() {
        LocalTime time = LocalTime.now();
        if (time.isAfter(LocalTime.of(0, 0, 1)) && (time.isBefore(LocalTime.of(1, 0, 0)))) {
            if (countReset) {
                logger.debug("Resetting API call counts");
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
                logger.debug("Loading features from Device ID: {}", deviceId);
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
                if (featuresData != null) {
                    for (FeatureDataDTO featureDataDTO : featuresData) {
                        notifyChildHandlers(featureDataDTO);
                    }
                } else {
                    logger.warn("Features list is empty.");
                }
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            logger.warn("Parsing Viessmann response fails: {}", e.getMessage());
        }
    }

    private void startViessmannBridgePolling(Integer pollingIntervalS, Integer initialDelay) {
        ScheduledFuture<?> currentPollingJob = viessmannBridgePollingJob;
        if (currentPollingJob == null) {
            viessmannBridgePollingJob = scheduler.scheduleWithFixedDelay(() -> {
                api.checkExpiringToken();
                checkResetApiCalls();
                if (!config.disablePolling) {
                    logger.debug("Refresh job scheduled to run every {} seconds for '{}'", pollingIntervalS,
                            getThing().getUID());
                    pollingFeatures();
                }
            }, initialDelay, pollingIntervalS, TimeUnit.SECONDS);
        }
    }

    protected synchronized void manageErrorPolling() {
        ScheduledFuture<?> errorPollingJob = viessmannErrorsPollingJob;
        if (errorChannelsLinked() && errorPollingJob == null) {
            stopViessmannBridgePolling();
            startViessmannBridgePolling(getPollingInterval(), getPollingInterval());
            startViessmannErrorsPolling(config.pollingIntervalErrors);
        } else {
            if (!errorChannelsLinked() && errorPollingJob != null) {
                stopViessmannErrorsPolling();
                stopViessmannBridgePolling();
                startViessmannBridgePolling(getPollingInterval(), getPollingInterval());
            }
        }
    }

    private void startViessmannErrorsPolling(Integer pollingInterval) {
        ScheduledFuture<?> currentPollingJob = viessmannErrorsPollingJob;
        if (currentPollingJob == null) {
            viessmannErrorsPollingJob = scheduler.scheduleWithFixedDelay(() -> {
                logger.debug("Refresh job scheduled to run every {} minutes for polling errors", pollingInterval);
                getDeviceError();
            }, 0, pollingInterval, TimeUnit.MINUTES);
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
                    startViessmannBridgePolling(getPollingInterval(), 1);
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

    public void stopViessmannErrorsPolling() {
        ScheduledFuture<?> currentPollingJob = viessmannErrorsPollingJob;
        if (currentPollingJob != null) {
            currentPollingJob.cancel(true);
            viessmannErrorsPollingJob = null;
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
        stopViessmannErrorsPolling();
        Long delay = (resetLimitMillis - Instant.now().toEpochMilli()) / 1000;
        stopViessmannBridgeLimitReset();
        startViessmannBridgeLimitReset(delay);
    }

    /**
     * Notify appropriate child thing handlers of a Viessmann message by calling their handleUpdate() methods.
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

    @Override
    public void channelLinked(ChannelUID channelUID) {
        manageErrorPolling();
        super.channelLinked(channelUID);
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        manageErrorPolling();
        super.channelUnlinked(channelUID);
    }
}
