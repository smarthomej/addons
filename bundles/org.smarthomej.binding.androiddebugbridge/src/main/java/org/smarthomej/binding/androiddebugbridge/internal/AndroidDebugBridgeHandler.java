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
package org.smarthomej.binding.androiddebugbridge.internal;

import static org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeDevice.VolumeInfo;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link AndroidDebugBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public class AndroidDebugBridgeHandler extends BaseThingHandler {

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
    private final AndroidDebugBridgeDevice adbConnection;
    private int maxMediaVolume = 0;
    private AndroidDebugBridgeConfiguration config = new AndroidDebugBridgeConfiguration();
    private @Nullable ScheduledFuture<?> connectionCheckerSchedule;
    private AndroidDebugBridgeMediaStatePackageConfig @Nullable [] packageConfigs = null;

    public AndroidDebugBridgeHandler(Thing thing) {
        super(thing);
        this.adbConnection = new AndroidDebugBridgeDevice(scheduler);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        AndroidDebugBridgeConfiguration currentConfig = config;
        if (currentConfig == null) {
            return;
        }
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
                    adbConnection.stopPackage(adbConnection.getCurrentPackage());
                    handleCommandInternal(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL),
                            RefreshType.REFRESH);
                }
                break;
            case OPEN_URL_CHANNEL:
                adbConnection.openURL(command.toFullString());
                handleCommandInternal(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL),
                        RefreshType.REFRESH);
                break;
            case CURRENT_PACKAGE_CHANNEL:
                if (command instanceof RefreshType) {
                    updateState(channelUID, new StringType(adbConnection.getCurrentPackage()));
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
                    updateState(channelUID, new DecimalType(adbConnection.getPowerWakeLock()));
                }
                break;
            case AWAKE_STATE_CHANNEL:
                if (command instanceof RefreshType) {
                    updateState(channelUID, OnOffType.from(adbConnection.isAwake()));
                }
                break;
            case SCREEN_STATE_CHANNEL:
                if (command instanceof RefreshType) {
                    updateState(channelUID, OnOffType.from(adbConnection.isScreenOn()));
                }
                break;
            case HDMI_STATE_CHANNEL:
                if (command instanceof RefreshType) {
                    adbConnection.isHDMIOn().ifPresent(state -> updateState(channelUID, OnOffType.from(state)));
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
            String currentPackage = adbConnection.getCurrentPackage();
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
                        int wakeLockState = adbConnection.getPowerWakeLock();
                        playing = currentPackageConfig.wakeLockPlayStates.contains(wakeLockState);
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
        try {
            this.packageConfigs = GSON.fromJson(mediaStateJSONConfig,
                    AndroidDebugBridgeMediaStatePackageConfig[].class);
        } catch (JsonSyntaxException e) {
            logger.warn("unable to parse media state config: {}", e.getMessage());
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> schedule = connectionCheckerSchedule;
        if (schedule != null) {
            schedule.cancel(true);
            connectionCheckerSchedule = null;
        }
        packageConfigs = null;
        adbConnection.disconnect();
        super.dispose();
    }

    public void checkConnection() {
        AndroidDebugBridgeConfiguration currentConfig = config;
        if (currentConfig == null) {
            return;
        }
        try {
            logger.debug("Refresh device {} status", currentConfig.ip);
            if (adbConnection.isConnected()) {
                updateStatus(ThingStatus.ONLINE);
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
                    refreshStatus();
                }
            }
        } catch (InterruptedException ignored) {
        } catch (AndroidDebugBridgeDeviceException | ExecutionException e) {
            logger.debug("Connection checker error: {}", e.getMessage());
            adbConnection.disconnect();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
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
            handleCommandInternal(new ChannelUID(this.thing.getUID(), MEDIA_CONTROL_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh play status: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh play status: Timeout");
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
            handleCommandInternal(new ChannelUID(this.thing.getUID(), WAKE_LOCK_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh wake lock: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.debug("Unable to refresh wake lock: Timeout");
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
        public String mode = "";
        public List<Integer> wakeLockPlayStates = List.of();
    }
}
