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
package org.smarthomej.binding.androiddebugbridge.internal;

import static org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeDevice.VolumeInfo;
import org.smarthomej.commons.UpdatingBaseThingHandler;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link AndroidDebugBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public class AndroidDebugBridgeHandler extends UpdatingBaseThingHandler {

    public static final String KEY_EVENT_PLAY = "126";
    public static final String KEY_EVENT_PAUSE = "127";
    public static final String KEY_EVENT_NEXT = "87";
    public static final String KEY_EVENT_PREVIOUS = "88";
    public static final String KEY_EVENT_MEDIA_REWIND = "89";
    public static final String KEY_EVENT_MEDIA_FAST_FORWARD = "90";
    private static final String SHUTDOWN_POWER_OFF = "POWER_OFF";
    private static final String SHUTDOWN_REBOOT = "REBOOT";
    private static final Gson GSON = new Gson();
    private final Logger logger = LoggerFactory.getLogger(AndroidDebugBridgeHandler.class);
    private final AndroidDebugBridgeDynamicCommandDescriptionProvider commandDescriptionProvider;
    private final AndroidDebugBridgeDevice adbConnection;
    private int maxMediaVolume = 0;
    private AndroidDebugBridgeConfiguration config = new AndroidDebugBridgeConfiguration();
    private @Nullable ScheduledFuture<?> connectionCheckerSchedule;
    private AndroidDebugBridgeMediaStatePackageConfig @Nullable [] packageConfigs;
    private final Map<String, Object> channelLastStateMap = new HashMap<>();
    /** Prevent a dispose/init cycle while this flag is set. Use for property updates */
    private boolean ignoreConfigurationUpdate;

    public AndroidDebugBridgeHandler(Thing thing,
            AndroidDebugBridgeDynamicCommandDescriptionProvider commandDescriptionProvider) {
        super(thing);
        this.commandDescriptionProvider = commandDescriptionProvider;
        this.adbConnection = new AndroidDebugBridgeDevice(scheduler);
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        if (!ignoreConfigurationUpdate) {
            super.handleConfigurationUpdate(configurationParameters);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        AndroidDebugBridgeConfiguration currentConfig = config;
        try {
            if (!adbConnection.isConnected()) {
                // try reconnect
                adbConnection.connect();
            }
            handleCommandInternal(channelUID, command);
        } catch (InterruptedException ignored) {
        } catch (AndroidDebugBridgeDeviceException | ExecutionException e) {
            if (!(e.getCause() instanceof InterruptedException)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                adbConnection.disconnect();
            }
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("{} - read error: {}", currentConfig.ip, e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("{} - timeout error", currentConfig.ip);
        }
    }

    private void handleCommandInternal(ChannelUID channelUID, Command command)
            throws InterruptedException, AndroidDebugBridgeDeviceException, AndroidDebugBridgeDeviceReadException,
            TimeoutException, ExecutionException {
        if (!isLinked(channelUID)) {
            return;
        }
        String channelId = channelUID.getId();
        switch (channelId) {
            case KEY_EVENT_CHANNEL:
                adbConnection.sendKeyEvent(command.toFullString());
                break;
            case TEXT_CHANNEL:
                adbConnection.sendText(command.toFullString());
                break;
            case TAP_CHANNEL:
                adbConnection.sendTap(command.toFullString());
                break;
            case MEDIA_VOLUME_CHANNEL:
                handleMediaVolume(channelUID, command);
                break;
            case MEDIA_CONTROL_CHANNEL:
                handleMediaControlCommand(channelUID, command);
                break;
            case START_PACKAGE_CHANNEL:
                adbConnection.startPackage(command.toFullString());
                handleCommandInternal(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL),
                        RefreshType.REFRESH);
                break;
            case STOP_PACKAGE_CHANNEL:
                adbConnection.stopPackage(command.toFullString());
                handleCommandInternal(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL),
                        RefreshType.REFRESH);
                break;
            case STOP_CURRENT_PACKAGE_CHANNEL:
                if (OnOffType.from(command.toFullString()).equals(OnOffType.OFF)) {
                    String lastCurrentPackage = (String) channelLastStateMap.remove(CURRENT_PACKAGE_CHANNEL);
                    if (lastCurrentPackage != null) {
                        adbConnection.stopPackage(lastCurrentPackage);
                        handleCommandInternal(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL),
                                RefreshType.REFRESH);
                    }
                }
                break;
            case OPEN_URL_CHANNEL:
                adbConnection.openURL(command.toFullString());
                handleCommandInternal(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL),
                        RefreshType.REFRESH);
                break;
            case START_INTENT_CHANNEL:
                adbConnection.startIntent(command.toFullString());
                handleCommandInternal(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL),
                        RefreshType.REFRESH);
                break;
            case CURRENT_PACKAGE_CHANNEL:
                if (command instanceof RefreshType) {
                    String currentPackage = adbConnection.getCurrentPackage();
                    updateState(channelUID, new StringType(currentPackage));
                    channelLastStateMap.put(CURRENT_PACKAGE_CHANNEL, currentPackage);
                }
                break;
            case SHUTDOWN_CHANNEL:
                switch (command.toFullString()) {
                    case SHUTDOWN_POWER_OFF:
                        adbConnection.powerOffDevice();
                        updateStatus(ThingStatus.OFFLINE);
                        break;
                    case SHUTDOWN_REBOOT:
                        adbConnection.rebootDevice();
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Rebooting");
                        break;
                }
            case WAKE_LOCK_CHANNEL:
                if (command instanceof RefreshType) {
                    int wakeLockState = adbConnection.getPowerWakeLock();
                    updateState(channelUID, new DecimalType(wakeLockState));
                    channelLastStateMap.put(WAKE_LOCK_CHANNEL, wakeLockState);
                }
                break;
            case AWAKE_STATE_CHANNEL:
                if (command instanceof RefreshType) {
                    boolean awakeState = adbConnection.isAwake();
                    boolean lastAwakeState = (boolean) channelLastStateMap.getOrDefault(AWAKE_STATE_CHANNEL, false);
                    if (awakeState == lastAwakeState) {
                        updateState(channelUID, OnOffType.from(awakeState));
                    }
                    channelLastStateMap.put(AWAKE_STATE_CHANNEL, awakeState);
                }
                break;
            case SCREEN_STATE_CHANNEL:
                if (command instanceof RefreshType) {
                    boolean screenState = adbConnection.isScreenOn();
                    boolean lastScreenState = (boolean) channelLastStateMap.getOrDefault(SCREEN_STATE_CHANNEL, false);
                    if (screenState == lastScreenState) {
                        updateState(channelUID, OnOffType.from(screenState));
                    }
                    channelLastStateMap.put(SCREEN_STATE_CHANNEL, screenState);
                }
                break;
            case HDMI_STATE_CHANNEL:
                if (command instanceof RefreshType) {
                    adbConnection.isHDMIOn().ifPresent(hdmiState -> {
                        boolean lastHDMIState = (boolean) channelLastStateMap.getOrDefault(HDMI_STATE_CHANNEL, false);
                        if (hdmiState.equals(lastHDMIState)) {
                            updateState(channelUID, OnOffType.from(hdmiState));
                        }
                        channelLastStateMap.put(HDMI_STATE_CHANNEL, hdmiState);
                    });
                }
                break;
        }
    }

    private void handleMediaVolume(ChannelUID channelUID, Command command)
            throws InterruptedException, AndroidDebugBridgeDeviceReadException, AndroidDebugBridgeDeviceException,
            TimeoutException, ExecutionException {
        if (command instanceof RefreshType) {
            VolumeInfo volumeInfo = adbConnection.getMediaVolume();
            maxMediaVolume = volumeInfo.max;
            updateState(channelUID, new PercentType((int) Math.round(toPercent(volumeInfo.current, volumeInfo.max))));
        } else {
            if (maxMediaVolume == 0) {
                return; // We can not transform percentage
            }
            int targetVolume = Integer.parseInt(command.toFullString());
            adbConnection.setMediaVolume((int) Math.round(fromPercent(targetVolume, maxMediaVolume)));
            updateState(channelUID, new PercentType(targetVolume));
        }
    }

    private double toPercent(double value, double maxValue) {
        return (value / maxValue) * 100;
    }

    private double fromPercent(double value, double maxValue) {
        return (value / 100) * maxValue;
    }

    private void handleMediaControlCommand(ChannelUID channelUID, Command command)
            throws InterruptedException, AndroidDebugBridgeDeviceException, AndroidDebugBridgeDeviceReadException,
            TimeoutException, ExecutionException {
        if (command instanceof RefreshType) {
            boolean playing;
            String lastCurrentPackage = (String) channelLastStateMap.getOrDefault(CURRENT_PACKAGE_CHANNEL, "");
            String currentPackage = lastCurrentPackage.isBlank() ? adbConnection.getCurrentPackage()
                    : lastCurrentPackage;
            AndroidDebugBridgeMediaStatePackageConfig currentPackageConfig = packageConfigs != null ? Arrays
                    .stream(packageConfigs).filter(pc -> pc.name.equals(currentPackage)).findFirst().orElse(null)
                    : null;
            if (currentPackageConfig != null) {
                logger.debug("media stream config found for {}, mode: {}", currentPackage, currentPackageConfig.mode);
                switch (currentPackageConfig.mode) {
                    case "idle":
                        playing = false;
                        break;
                    case "wake_lock":
                        int lastWakeLockState = (int) channelLastStateMap.getOrDefault(WAKE_LOCK_CHANNEL, 0);
                        playing = currentPackageConfig.wakeLockPlayStates.contains(lastWakeLockState);
                        break;
                    case "media_state":
                        playing = adbConnection.isPlayingMedia(currentPackage);
                        break;
                    case "audio":
                        playing = adbConnection.isPlayingAudio();
                        break;
                    default:
                        logger.warn("media state config: package {} unsupported mode", currentPackage);
                        playing = false;
                }
            } else {
                logger.debug("media stream config not found for {}", currentPackage);
                playing = adbConnection.isPlayingMedia(currentPackage);
            }
            updateState(channelUID, playing ? PlayPauseType.PLAY : PlayPauseType.PAUSE);
            updateState(STOP_CURRENT_PACKAGE_CHANNEL, OnOffType.from(playing));
        } else if (command instanceof PlayPauseType) {
            if (command == PlayPauseType.PLAY) {
                adbConnection.sendKeyEvent(KEY_EVENT_PLAY);
                updateState(channelUID, PlayPauseType.PLAY);
            } else if (command == PlayPauseType.PAUSE) {
                adbConnection.sendKeyEvent(KEY_EVENT_PAUSE);
                updateState(channelUID, PlayPauseType.PAUSE);
            }
        } else if (command instanceof NextPreviousType) {
            if (command == NextPreviousType.NEXT) {
                adbConnection.sendKeyEvent(KEY_EVENT_NEXT);
            } else if (command == NextPreviousType.PREVIOUS) {
                adbConnection.sendKeyEvent(KEY_EVENT_PREVIOUS);
            }
        } else if (command instanceof RewindFastforwardType) {
            if (command == RewindFastforwardType.FASTFORWARD) {
                adbConnection.sendKeyEvent(KEY_EVENT_MEDIA_FAST_FORWARD);
            } else if (command == RewindFastforwardType.REWIND) {
                adbConnection.sendKeyEvent(KEY_EVENT_MEDIA_REWIND);
            }
        } else {
            logger.warn("Unknown media control command: {}", command);
        }
    }

    @Override
    public void initialize() {
        AndroidDebugBridgeConfiguration currentConfig = getConfigAs(AndroidDebugBridgeConfiguration.class);
        config = currentConfig;
        String mediaStateJSONConfig = currentConfig.mediaStateJSONConfig;
        if (mediaStateJSONConfig != null && !mediaStateJSONConfig.isEmpty()) {
            loadMediaStateConfig(mediaStateJSONConfig);
        }
        adbConnection.configure(currentConfig.ip, currentConfig.port, currentConfig.timeout);
        updateStatus(ThingStatus.UNKNOWN);
        connectionCheckerSchedule = scheduler.scheduleWithFixedDelay(this::checkConnection, 0,
                currentConfig.refreshTime, TimeUnit.SECONDS);
    }

    private void loadMediaStateConfig(String mediaStateJSONConfig) {
        List<CommandOption> commandOptions;
        try {
            packageConfigs = GSON.fromJson(mediaStateJSONConfig, AndroidDebugBridgeMediaStatePackageConfig[].class);
            commandOptions = Arrays.stream(packageConfigs)
                    .map(AndroidDebugBridgeMediaStatePackageConfig::toCommandOption)
                    .collect(Collectors.toUnmodifiableList());
        } catch (JsonSyntaxException e) {
            logger.warn("Unable to parse media state config: {}", e.getMessage());
            commandOptions = List.of();
        }
        commandDescriptionProvider.setCommandOptions(new ChannelUID(getThing().getUID(), START_PACKAGE_CHANNEL),
                commandOptions);
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> schedule = connectionCheckerSchedule;
        if (schedule != null) {
            schedule.cancel(true);
            connectionCheckerSchedule = null;
        }
        packageConfigs = null;
        channelLastStateMap.clear();
        adbConnection.disconnect();
        super.dispose();
    }

    public void checkConnection() {
        try {
            logger.debug("Refresh device {} status", config.ip);
            if (adbConnection.isConnected()) {
                updateStatus(ThingStatus.ONLINE);
                refreshProperties();
                refreshStatus();
            } else {
                try {
                    adbConnection.connect();
                } catch (AndroidDebugBridgeDeviceException e) {
                    logger.debug("Error connecting to device; [{}]: {}", e.getClass().getCanonicalName(),
                            e.getMessage());
                    adbConnection.disconnect();
                    updateStatus(ThingStatus.OFFLINE);
                    return;
                } catch (TimeoutException e) {
                    logger.debug("Error connecting to device; [{}]: {}", e.getClass().getCanonicalName(),
                            e.getMessage());
                    adbConnection.disconnect();
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                    return;
                }
                if (adbConnection.isConnected()) {
                    updateStatus(ThingStatus.ONLINE);
                    refreshProperties();
                    refreshStatus();
                }
            }
        } catch (InterruptedException ignored) {
        } catch (AndroidDebugBridgeDeviceException | AndroidDebugBridgeDeviceReadException | ExecutionException e) {
            logger.debug("Connection checker error: {}", e.getMessage());
            adbConnection.disconnect();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void refreshProperties() throws InterruptedException, AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, ExecutionException {
        // Add some information about the device
        try {
            Map<String, String> editProperties = editProperties();
            editProperties.put(Thing.PROPERTY_SERIAL_NUMBER, adbConnection.getSerialNo());
            editProperties.put(Thing.PROPERTY_MODEL_ID, adbConnection.getModel());
            editProperties.put(Thing.PROPERTY_FIRMWARE_VERSION, adbConnection.getAndroidVersion());
            editProperties.put(Thing.PROPERTY_VENDOR, adbConnection.getBrand());
            try {
                editProperties.put(Thing.PROPERTY_MAC_ADDRESS, adbConnection.getMacAddress());
            } catch (AndroidDebugBridgeDeviceReadException e) {
                logger.debug("Refresh properties error: {}", e.getMessage());
            }
            ignoreConfigurationUpdate = true;
            updateProperties(editProperties);
            ignoreConfigurationUpdate = false;
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh properties error: Timeout");
            return;
        }
    }

    private void refreshStatus() throws InterruptedException, AndroidDebugBridgeDeviceException, ExecutionException {
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), MEDIA_VOLUME_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh media volume: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh media volume: Timeout");
            adbConnection.disconnect();
            return;
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), WAKE_LOCK_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh wake-lock: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh wake-lock: Timeout");
            adbConnection.disconnect();
            return;
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh current package: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh current package: Timeout");
            adbConnection.disconnect();
            return;
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), MEDIA_CONTROL_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh play status: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh play status: Timeout");
            adbConnection.disconnect();
            return;
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), AWAKE_STATE_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh awake state: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh awake state: Timeout");
            adbConnection.disconnect();
            return;
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), SCREEN_STATE_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh screen state: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh screen state: Timeout");
            adbConnection.disconnect();
            return;
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), HDMI_STATE_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh hdmi state: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh hdmi state: Timeout");
            adbConnection.disconnect();
            return;
        }
    }

    static class AndroidDebugBridgeMediaStatePackageConfig {
        public String name = "";
        public @Nullable String label;
        public String mode = "";
        public List<Integer> wakeLockPlayStates = List.of();

        public CommandOption toCommandOption() {
            return new CommandOption(name, label == null ? name : label);
        }
    }
}
