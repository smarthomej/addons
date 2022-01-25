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
package org.smarthomej.binding.amazonechocontrol.internal.handler;

import static com.jayway.jsonpath.Criteria.where;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.storage.Storage;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.AccountHandlerConfig;
import org.smarthomej.binding.amazonechocontrol.internal.AccountServlet;
import org.smarthomej.binding.amazonechocontrol.internal.ConnectionException;
import org.smarthomej.binding.amazonechocontrol.internal.JsonDocument;
import org.smarthomej.binding.amazonechocontrol.internal.channelhandler.AmazonHandlerCallback;
import org.smarthomej.binding.amazonechocontrol.internal.channelhandler.ChannelHandler;
import org.smarthomej.binding.amazonechocontrol.internal.channelhandler.ChannelHandlerSendMessage;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.discovery.AmazonEchoDiscovery;
import org.smarthomej.binding.amazonechocontrol.internal.discovery.SmartHomeDevicesDiscovery;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonBluetoothStates;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonBluetoothStates.BluetoothState;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonCommandPayloadPushDevice;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonCommandPayloadPushDevice.DopplerId;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonCustomerHistoryRecords.CustomerHistoryRecord;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonDevices.Device;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonFeed;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonMusicProvider;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonNotificationResponse;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonNotificationSound;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonPlaylists;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonPushCommand;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevice;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.SmartHomeBaseDevice;
import org.smarthomej.binding.amazonechocontrol.internal.smarthome.SmartHomeDeviceStateGroupUpdateCalculator;
import org.smarthomej.binding.amazonechocontrol.internal.websocket.WebSocketCommandHandler;
import org.smarthomej.binding.amazonechocontrol.internal.websocket.WebSocketConnection;
import org.smarthomej.binding.amazonechocontrol.internal.websocket.WebsocketException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;

/**
 * Handles the connection to the amazon server.
 *
 * @author Michael Geramb - Initial Contribution
 */
@NonNullByDefault
public class AccountHandler extends BaseBridgeHandler implements WebSocketCommandHandler, AmazonHandlerCallback {
    private final Logger logger = LoggerFactory.getLogger(AccountHandler.class);
    private final Storage<String> stateStorage;
    private final HttpClient httpClient;
    private @Nullable Connection connection;
    private @Nullable WebSocketConnection webSocketConnection;

    private final Set<EchoHandler> echoHandlers = new CopyOnWriteArraySet<>();
    private final Set<SmartHomeDeviceHandler> smartHomeDeviceHandlers = new CopyOnWriteArraySet<>();
    private final Set<FlashBriefingProfileHandler> flashBriefingProfileHandlers = new CopyOnWriteArraySet<>();

    private final Object synchronizeConnection = new Object();
    private Map<String, Device> jsonSerialNumberDeviceMapping = new HashMap<>();
    private Map<String, SmartHomeBaseDevice> jsonIdSmartHomeDeviceMapping = new HashMap<>();

    private @Nullable ScheduledFuture<?> checkDataJob;
    private @Nullable ScheduledFuture<?> checkLoginJob;
    private @Nullable ScheduledFuture<?> updateSmartHomeStateJob;
    private @Nullable ScheduledFuture<?> refreshAfterCommandJob;
    private @Nullable ScheduledFuture<?> refreshSmartHomeAfterCommandJob;
    private final Object synchronizeSmartHomeJobScheduler = new Object();
    private @Nullable ScheduledFuture<?> forceCheckDataJob;
    private String currentFlashBriefingJson = "";
    private final HttpService httpService;
    private @Nullable AccountServlet accountServlet;
    private final Gson gson;
    private int checkDataCounter;
    private final LinkedBlockingQueue<String> requestedDeviceUpdates = new LinkedBlockingQueue<>();
    private @Nullable SmartHomeDeviceStateGroupUpdateCalculator smartHomeDeviceStateGroupUpdateCalculator;
    private final List<ChannelHandler> channelHandlers = new ArrayList<>();

    private AccountHandlerConfig handlerConfig = new AccountHandlerConfig();

    public AccountHandler(Bridge bridge, HttpService httpService, Storage<String> stateStorage, Gson gson,
            HttpClient httpClient) {
        super(bridge);
        this.gson = gson;
        this.httpClient = httpClient;
        this.httpService = httpService;
        this.stateStorage = stateStorage;
        channelHandlers.add(new ChannelHandlerSendMessage(this, this.gson));
    }

    @Override
    public void initialize() {
        handlerConfig = getConfig().as(AccountHandlerConfig.class);

        synchronized (synchronizeConnection) {
            Connection connection = this.connection;
            if (connection == null) {
                this.connection = new Connection(null, gson);
            }
        }

        if (accountServlet == null) {
            try {
                accountServlet = new AccountServlet(httpService, this.getThing().getUID().getId(), this, gson);
            } catch (IllegalStateException e) {
                logger.warn("Failed to create account servlet", e);
            }
        }

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING, "Wait for login");

        checkLoginJob = scheduler.scheduleWithFixedDelay(this::checkLogin, 0, 60, TimeUnit.SECONDS);
        checkDataJob = scheduler.scheduleWithFixedDelay(this::checkData, 4, 60, TimeUnit.SECONDS);

        int pollingIntervalAlexa = Math.max(10, handlerConfig.pollingIntervalSmartHomeAlexa);
        int pollingIntervalSkills = Math.max(60, handlerConfig.pollingIntervalSmartSkills);

        smartHomeDeviceStateGroupUpdateCalculator = new SmartHomeDeviceStateGroupUpdateCalculator(pollingIntervalAlexa,
                pollingIntervalSkills);
        updateSmartHomeStateJob = scheduler.scheduleWithFixedDelay(() -> updateSmartHomeState(null), 20, 10,
                TimeUnit.SECONDS);
    }

    @Override
    public void updateChannelState(String channelId, State state) {
        updateState(channelId, state);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            logger.trace("Command '{}' received for channel '{}'", command, channelUID);
            Connection connection = this.connection;
            if (connection == null) {
                return;
            }

            String channelId = channelUID.getId();
            for (ChannelHandler channelHandler : channelHandlers) {
                if (channelHandler.tryHandleCommand(new Device(), connection, channelId, command)) {
                    return;
                }
            }
            if (command instanceof RefreshType) {
                refreshData();
            }
        } catch (ConnectionException e) {
            logger.info("handleCommand fails", e);
        }
    }

    @Override
    public void startAnnouncement(Device device, String speak, String bodyText, @Nullable String title,
            @Nullable Integer volume) {
        EchoHandler echoHandler = findEchoHandlerBySerialNumber(device.serialNumber);
        if (echoHandler != null) {
            echoHandler.startAnnouncement(device, speak, bodyText, title, volume);
        }
    }

    public Set<FlashBriefingProfileHandler> getFlashBriefingProfileHandlers() {
        return new HashSet<>(flashBriefingProfileHandlers);
    }

    public List<Device> getLastKnownDevices() {
        return new ArrayList<>(jsonSerialNumberDeviceMapping.values());
    }

    public List<SmartHomeBaseDevice> getLastKnownSmartHomeDevices() {
        return new ArrayList<>(jsonIdSmartHomeDeviceMapping.values());
    }

    public void forceCheckData() {
        if (forceCheckDataJob == null) {
            forceCheckDataJob = scheduler.schedule(this::checkData, 1000, TimeUnit.MILLISECONDS);
        }
    }

    public @Nullable Thing findThingBySerialNumber(@Nullable String deviceSerialNumber) {
        EchoHandler echoHandler = findEchoHandlerBySerialNumber(deviceSerialNumber);
        if (echoHandler != null) {
            return echoHandler.getThing();
        }
        return null;
    }

    public @Nullable EchoHandler findEchoHandlerBySerialNumber(@Nullable String deviceSerialNumber) {
        if (deviceSerialNumber == null) {
            return null;
        }
        return echoHandlers.stream().filter(echoHandler -> deviceSerialNumber.equals(echoHandler.findSerialNumber()))
                .findAny().orElse(null);
    }

    private void scheduleUpdate() {
        checkDataCounter = 999;
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        super.childHandlerInitialized(childHandler, childThing);

        if (childHandler instanceof EchoHandler) {
            if (echoHandlers.add((EchoHandler) childHandler)) {
                forceCheckData();
            }
        } else if (childHandler instanceof SmartHomeDeviceHandler) {
            if (smartHomeDeviceHandlers.add((SmartHomeDeviceHandler) childHandler)) {
                forceCheckData();
            }
        } else if (childHandler instanceof FlashBriefingProfileHandler) {
            FlashBriefingProfileHandler flashBriefingProfileHandler = (FlashBriefingProfileHandler) childHandler;
            flashBriefingProfileHandlers.add(flashBriefingProfileHandler);
            Connection connection = this.connection;
            if (connection != null && connection.getIsLoggedIn()) {
                if (currentFlashBriefingJson.isEmpty()) {
                    currentFlashBriefingJson = getFlashBriefingProfiles(connection);
                }
                flashBriefingProfileHandler.initialize(this, currentFlashBriefingJson);
            }
            // set flashbriefing description on echo handlers
            echoHandlers.forEach(h -> h.createStartCommandCommandOptions(flashBriefingProfileHandlers));
        }

        scheduleUpdate();
    }

    @Override
    public void handleRemoval() {
        cleanup();
        super.handleRemoval();
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        if (childHandler instanceof EchoHandler) {
            echoHandlers.remove(childHandler);
        } else if (childHandler instanceof FlashBriefingProfileHandler) {
            flashBriefingProfileHandlers.remove(childHandler);
            echoHandlers.forEach(h -> h.createStartCommandCommandOptions(flashBriefingProfileHandlers));
        } else if (childHandler instanceof SmartHomeDeviceHandler) {
            smartHomeDeviceHandlers.remove(childHandler);
        }

        super.childHandlerDisposed(childHandler, childThing);
    }

    @Override
    public void dispose() {
        AccountServlet accountServlet = this.accountServlet;
        if (accountServlet != null) {
            accountServlet.dispose();
        }
        this.accountServlet = null;
        cleanup();
        super.dispose();
    }

    private void cleanup() {
        logger.debug("cleanup {}", getThing().getUID().getAsString());
        ScheduledFuture<?> updateSmartHomeStateJob = this.updateSmartHomeStateJob;
        if (updateSmartHomeStateJob != null) {
            updateSmartHomeStateJob.cancel(true);
            this.updateSmartHomeStateJob = null;
        }
        ScheduledFuture<?> refreshJob = this.checkDataJob;
        if (refreshJob != null) {
            refreshJob.cancel(true);
            this.checkDataJob = null;
        }
        ScheduledFuture<?> refreshLogin = this.checkLoginJob;
        if (refreshLogin != null) {
            refreshLogin.cancel(true);
            this.checkLoginJob = null;
        }
        ScheduledFuture<?> foceCheckDataJob = this.forceCheckDataJob;
        if (foceCheckDataJob != null) {
            foceCheckDataJob.cancel(true);
            this.forceCheckDataJob = null;
        }
        ScheduledFuture<?> refreshAfterCommandJob = this.refreshAfterCommandJob;
        if (refreshAfterCommandJob != null) {
            refreshAfterCommandJob.cancel(true);
            this.refreshAfterCommandJob = null;
        }
        ScheduledFuture<?> refreshSmartHomeAfterCommandJob = this.refreshSmartHomeAfterCommandJob;
        if (refreshSmartHomeAfterCommandJob != null) {
            refreshSmartHomeAfterCommandJob.cancel(true);
            this.refreshSmartHomeAfterCommandJob = null;
        }
        Connection connection = this.connection;
        if (connection != null) {
            connection.logout();
            this.connection = null;
        }
        closeWebSocketConnection();
    }

    private void checkLogin() {
        try {
            ThingUID uid = getThing().getUID();
            logger.debug("check login {}", uid.getAsString());

            synchronized (synchronizeConnection) {
                Connection currentConnection = this.connection;
                if (currentConnection == null) {
                    return;
                }

                try {
                    if (currentConnection.getIsLoggedIn()) {
                        if (currentConnection.checkRenewSession()) {
                            setConnection(currentConnection);
                        }
                    } else {
                        // read session data from property
                        String sessionStore = this.stateStorage.get("sessionStorage");

                        // try to use the session data
                        if (currentConnection.tryRestoreLogin(sessionStore, null)) {
                            setConnection(currentConnection);
                        }
                    }
                    if (!currentConnection.getIsLoggedIn()) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                                "Please login in through web site: http(s)://<YOUROPENHAB>:<YOURPORT>/amazonechocontrol/"
                                        + URLEncoder.encode(uid.getId(), StandardCharsets.UTF_8));
                    }
                } catch (ConnectionException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
                } catch (URISyntaxException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
                }
            }
        } catch (Exception e) { // this handler can be removed later, if we know that nothing else can fail.
            logger.error("check login fails with unexpected error", e);
        }
    }

    // used to set a valid connection from the web proxy login
    public void setConnection(@Nullable Connection connection) {
        this.connection = connection;
        if (connection != null) {
            String serializedStorage = connection.getLoginData().serializeLoginData();
            this.stateStorage.put("sessionStorage", serializedStorage);
        } else {
            this.stateStorage.put("sessionStorage", null);
            updateStatus(ThingStatus.OFFLINE);
        }
        closeWebSocketConnection();
        if (connection != null) {
            updateDeviceList();
            updateSmartHomeDeviceList(false);
            getFlashBriefingHandlers();
            updateStatus(ThingStatus.ONLINE);
            scheduleUpdate();
            checkData();
        }
    }

    void closeWebSocketConnection() {
        WebSocketConnection webSocketConnection = this.webSocketConnection;
        this.webSocketConnection = null;
        if (webSocketConnection != null) {
            webSocketConnection.close();
        }
    }

    private boolean checkWebSocketConnection() {
        WebSocketConnection webSocketConnection = this.webSocketConnection;
        if (webSocketConnection == null || webSocketConnection.isClosed()) {
            Connection connection = this.connection;
            if (connection != null && connection.getIsLoggedIn()) {
                try {
                    this.webSocketConnection = new WebSocketConnection(connection, this, gson, httpClient);
                } catch (WebsocketException e) {
                    if (e.getCause() != null) {
                        logger.warn("{}", e.getMessage(), e);
                    } else {
                        logger.warn("{}", e.getMessage());
                    }
                }
            }
            return false;
        }
        return true;
    }

    private void checkData() {
        synchronized (synchronizeConnection) {
            try {
                Connection connection = this.connection;
                if (connection != null && connection.getIsLoggedIn()) {
                    checkDataCounter++;
                    if (checkDataCounter > 60 || forceCheckDataJob != null) {
                        checkDataCounter = 0;
                        forceCheckDataJob = null;
                    }
                    if (!checkWebSocketConnection() || checkDataCounter == 0) {
                        refreshData();
                    }
                }
                logger.debug("checkData {} finished", getThing().getUID().getAsString());
            } catch (JsonSyntaxException e) {
                logger.debug("checkData fails", e);
            } catch (Exception e) { // this handler can be removed later, if we know that nothing else can fail.
                logger.error("checkData fails with unexpected error", e);
            }
        }
    }

    private void refreshNotifications() {
        Connection currentConnection = this.connection;
        if (currentConnection == null) {
            return;
        }
        if (!currentConnection.getIsLoggedIn()) {
            return;
        }

        ZonedDateTime timeStamp = ZonedDateTime.now();
        try {
            List<JsonNotificationResponse> notifications = currentConnection.notifications();
            ZonedDateTime timeStampNow = ZonedDateTime.now();
            echoHandlers
                    .forEach(echoHandler -> echoHandler.updateNotifications(timeStamp, timeStampNow, notifications));
        } catch (ConnectionException e) {
            logger.debug("refreshNotifications failed", e);
        }
    }

    private void refreshData() {
        synchronized (synchronizeConnection) {
            try {
                logger.debug("refreshing data {}", getThing().getUID().getAsString());

                // check if logged in
                Connection currentConnection = connection;
                if (currentConnection == null || !currentConnection.getIsLoggedIn()) {
                    return;
                }

                // get all devices registered in the account
                updateDeviceList();
                updateSmartHomeDeviceList(false);
                getFlashBriefingHandlers();

                JsonDocument deviceNotificationStates = currentConnection
                        .alexaGetRequest("/api/device-notification-state");
                JsonDocument ascendingAlarmModels = currentConnection.alexaGetRequest("/api/ascending-alarm");

                JsonBluetoothStates states = null;
                List<JsonMusicProvider> musicProviders = null;
                if (currentConnection.getIsLoggedIn()) {

                    // update bluetooth states
                    states = currentConnection.getBluetoothConnectionStates();

                    // update music providers
                    if (currentConnection.getIsLoggedIn()) {
                        try {
                            musicProviders = currentConnection.getMusicProviders();
                        } catch (JsonSyntaxException e) {
                            logger.debug("Update music provider failed", e);
                        }
                    }
                }
                // forward device information to echo handler
                for (EchoHandler child : echoHandlers) {
                    Device device = findDeviceJson(child.findSerialNumber());

                    List<JsonNotificationSound> notificationSounds = List.of();
                    JsonPlaylists playlists = null;
                    if (device != null && currentConnection.getIsLoggedIn()) {
                        // update notification sounds
                        try {
                            notificationSounds = currentConnection.getNotificationSounds(device);
                        } catch (JsonSyntaxException | ConnectionException e) {
                            logger.debug("Update notification sounds failed", e);
                        }
                        // update playlists
                        try {
                            playlists = currentConnection.getPlaylists(device);
                        } catch (JsonSyntaxException | ConnectionException e) {
                            logger.debug("Update playlist failed", e);
                        }
                    }

                    BluetoothState state = null;
                    if (states != null) {
                        state = states.findStateByDevice(device);
                    }
                    Integer deviceNotificationVolume = null;
                    Boolean ascendingAlarmEnabled = null;
                    if (device != null) {
                        final String serialNumber = device.serialNumber;
                        if (serialNumber != null) {
                            ascendingAlarmEnabled = ascendingAlarmModels.getFirst(
                                    "$.ascendingAlarmModelList[?].ascendingAlarmEnabled", Boolean.class,
                                    where("deviceSerialNumber").eq(serialNumber));
                            deviceNotificationVolume = deviceNotificationStates.getFirst(
                                    "$.deviceNotificationStates[?].volumeLevel", Integer.class,
                                    where("deviceSerialNumber").eq(serialNumber));
                        }
                    }
                    child.updateState(this, device, state, deviceNotificationVolume, ascendingAlarmEnabled, playlists,
                            notificationSounds, musicProviders);
                }

                // refresh notifications
                refreshNotifications();

                // update account state
                updateStatus(ThingStatus.ONLINE);

                logger.debug("refresh data {} finished", getThing().getUID().getAsString());
            } catch (JsonSyntaxException e) {
                logger.debug("refresh data fails", e);
            } catch (Exception e) { // this handler can be removed later, if we know that nothing else can fail.
                logger.error("refresh data fails with unexpected error", e);
            }
        }
    }

    public @Nullable Device findDeviceJson(@Nullable String serialNumber) {
        if (serialNumber == null || serialNumber.isEmpty()) {
            return null;
        }
        return this.jsonSerialNumberDeviceMapping.get(serialNumber);
    }

    public @Nullable Device findDeviceJsonBySerialOrName(@Nullable String serialOrName) {
        if (serialOrName == null || serialOrName.isEmpty()) {
            return null;
        }

        return this.jsonSerialNumberDeviceMapping.values().stream().filter(
                d -> serialOrName.equalsIgnoreCase(d.serialNumber) || serialOrName.equalsIgnoreCase(d.accountName))
                .findFirst().orElse(null);
    }

    public List<Device> updateDeviceList() {
        Connection currentConnection = connection;
        if (currentConnection == null) {
            return new ArrayList<>();
        }

        List<Device> devices = null;
        try {
            if (currentConnection.getIsLoggedIn()) {
                devices = currentConnection.getDeviceList();
            }
        } catch (ConnectionException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
        }
        if (devices != null) {
            // create new device map
            jsonSerialNumberDeviceMapping = devices.stream().filter(device -> device.serialNumber != null)
                    .collect(Collectors.toMap(d -> Objects.requireNonNull(d.serialNumber), d -> d));
            // notify flashbriefing profile handlers of changed device list
            flashBriefingProfileHandlers.forEach(h -> h.setCommandDescription(jsonSerialNumberDeviceMapping.values()));
        }

        try {
            JsonDocument document = currentConnection.alexaGetRequest("/api/wake-word?cached=true");
            for (EchoHandler echoHandler : echoHandlers) {
                String serialNumber = echoHandler.findSerialNumber();
                String wakeWord = document.getFirst(
                        "$.wakeWords[?(@.deviceSerialNumber == '" + serialNumber + "')].wakeWord", String.class);
                echoHandler.setDeviceAndUpdateThingState(this, findDeviceJson(serialNumber), wakeWord);
            }
        } catch (ConnectionException e) {
            logger.info("getting wakewords failed", e);
        }

        if (devices != null) {
            return devices;
        }
        return List.of();
    }

    public void setEnabledFlashBriefingsJson(String flashBriefingJson) {
        Connection currentConnection = connection;
        JsonFeed[] feeds = gson.fromJson(flashBriefingJson, JsonFeed[].class);
        if (currentConnection != null && feeds != null) {
            try {
                currentConnection.setEnabledFlashBriefings(Arrays.asList(feeds));
            } catch (ConnectionException e) {
                logger.warn("Set flashbriefing profile failed", e);
            }
        }
        getFlashBriefingHandlers();
    }

    public String getFlashBriefingHandlers() {
        Connection currentConnection = connection;
        if (currentConnection != null) {
            return getFlashBriefingHandlers(currentConnection);
        }
        return "";
    }

    private String getFlashBriefingHandlers(Connection currentConnection) {
        if (!flashBriefingProfileHandlers.isEmpty() || currentFlashBriefingJson.isEmpty()) {
            String flashBriefingProfiles = getFlashBriefingProfiles(currentConnection);
            if (!flashBriefingProfiles.isEmpty()) {
                this.currentFlashBriefingJson = flashBriefingProfiles;
            }
        }
        boolean flashBriefingProfileFound = false;
        for (FlashBriefingProfileHandler child : flashBriefingProfileHandlers) {
            if (child.initialize(this, currentFlashBriefingJson)) {
                flashBriefingProfileFound = true;
            }
        }
        if (flashBriefingProfileFound) {
            return "";
        }
        return this.currentFlashBriefingJson;
    }

    public @Nullable Connection findConnection() {
        return this.connection;
    }

    public String updateAndGetEnabledFlashBriefingsJson() {
        Connection currentConnection = this.connection;
        if (currentConnection == null) {
            return "";
        }
        String flashBriefingJson = getFlashBriefingProfiles(currentConnection);
        if (!flashBriefingJson.isEmpty()) {
            this.currentFlashBriefingJson = flashBriefingJson;
        }
        return this.currentFlashBriefingJson;
    }

    private String getFlashBriefingProfiles(Connection currentConnection) {
        try {
            // Make a copy and remove changeable parts
            JsonFeed[] forSerializer = currentConnection.getEnabledFlashBriefings().stream()
                    .map(source -> new JsonFeed(source.feedId, source.skillId)).toArray(JsonFeed[]::new);
            return Objects.requireNonNull(gson.toJson(forSerializer));
        } catch (JsonSyntaxException | ConnectionException e) {
            logger.warn("get flash briefing profiles fails", e);
        }
        return "";
    }

    @Override
    public void webSocketCommandReceived(JsonPushCommand pushCommand) {
        try {
            handleWebsocketCommand(pushCommand);
        } catch (Exception e) {
            // should never happen, but if the exception is going out of this function, the binding stop working.
            logger.warn("handling of websockets fails", e);
        }
    }

    void handleWebsocketCommand(JsonPushCommand pushCommand) {
        String command = pushCommand.command;
        if (command != null) {
            ScheduledFuture<?> refreshDataDelayed = this.refreshAfterCommandJob;
            switch (command) {
                case "PUSH_ACTIVITY":
                    handlePushActivity(pushCommand.payload);
                    break;
                case "PUSH_DOPPLER_CONNECTION_CHANGE":
                case "PUSH_BLUETOOTH_STATE_CHANGE":
                    if (refreshDataDelayed != null) {
                        refreshDataDelayed.cancel(false);
                    }
                    this.refreshAfterCommandJob = scheduler.schedule(this::refreshData, 700, TimeUnit.MILLISECONDS);
                    break;
                case "PUSH_NOTIFICATION_CHANGE":
                    refreshNotifications();
                    break;
                default:
                    String payload = pushCommand.payload;
                    if (payload != null && payload.startsWith("{") && payload.endsWith("}")) {
                        JsonCommandPayloadPushDevice devicePayload = Objects
                                .requireNonNull(gson.fromJson(payload, JsonCommandPayloadPushDevice.class));
                        DopplerId dopplerId = devicePayload.dopplerId;
                        if (dopplerId != null) {
                            handlePushDeviceCommand(dopplerId, command, payload);
                        }
                    }
                    break;
            }
        }
    }

    private void handlePushDeviceCommand(DopplerId dopplerId, String command, String payload) {
        EchoHandler echoHandler = findEchoHandlerBySerialNumber(dopplerId.deviceSerialNumber);
        if (echoHandler != null) {
            echoHandler.handlePushCommand(command, payload);
        }
    }

    private void handlePushActivity(@Nullable String payload) {
        if (payload == null) {
            return;
        }
        JsonDocument pushActivity = new JsonDocument(payload);
        String userId = pushActivity.get("$.key.registeredUserId", String.class);
        String entryId = pushActivity.get("$.key.entryId", String.class);

        if (userId == null || entryId == null) {
            return;
        }
        String searchKey = userId + "#" + entryId;

        Connection connection = this.connection;
        if (connection == null || !connection.getIsLoggedIn()) {
            return;
        }

        Long timestamp = pushActivity.get("$.timestamp", Long.class);
        if (timestamp != null) {
            long startTimestamp = timestamp - 30000;
            long endTimestamp = timestamp + 30000;
            List<CustomerHistoryRecord> customerHistoryRecords = connection.getActivities(startTimestamp, endTimestamp);
            for (CustomerHistoryRecord customerHistoryRecord : customerHistoryRecords) {
                String recordKey = customerHistoryRecord.recordKey;
                if (recordKey != null && searchKey.equals(recordKey)) {
                    String[] splitRecordKey = recordKey.split("#");
                    if (splitRecordKey.length >= 2) {
                        EchoHandler echoHandler = findEchoHandlerBySerialNumber(splitRecordKey[3]);
                        if (echoHandler != null) {
                            echoHandler.handlePushActivity(customerHistoryRecord);
                            break;
                        }
                    }
                }
            }
        }
    }

    private @Nullable SmartHomeBaseDevice findSmartHomeDeviceJson(SmartHomeDeviceHandler handler) {
        String id = handler.getId();
        if (!id.isEmpty()) {
            return jsonIdSmartHomeDeviceMapping.get(id);
        }
        return null;
    }

    public int getSmartHomeDevicesDiscoveryMode() {
        return handlerConfig.discoverSmartHome;
    }

    public List<SmartHomeBaseDevice> updateSmartHomeDeviceList(boolean forceUpdate) {
        Connection currentConnection = connection;
        if (currentConnection == null) {
            return Collections.emptyList();
        }

        if (!forceUpdate && smartHomeDeviceHandlers.isEmpty() && getSmartHomeDevicesDiscoveryMode() == 0) {
            return Collections.emptyList();
        }

        List<SmartHomeBaseDevice> smartHomeDevices = null;
        try {
            if (currentConnection.getIsLoggedIn()) {
                smartHomeDevices = currentConnection.getSmarthomeDeviceList();
            }
        } catch (ConnectionException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
        }
        if (smartHomeDevices != null) {
            // create new id map
            Map<String, SmartHomeBaseDevice> newJsonIdSmartHomeDeviceMapping = new HashMap<>();
            for (SmartHomeBaseDevice smartHomeDevice : smartHomeDevices) {
                String id = smartHomeDevice.findId();
                if (id != null) {
                    newJsonIdSmartHomeDeviceMapping.put(id, smartHomeDevice);
                }
            }
            jsonIdSmartHomeDeviceMapping = newJsonIdSmartHomeDeviceMapping;
        }
        // update handlers
        smartHomeDeviceHandlers
                .forEach(child -> child.setDeviceAndUpdateThingState(this, findSmartHomeDeviceJson(child)));

        return Objects.requireNonNullElse(smartHomeDevices, List.of());
    }

    public void forceDelayedSmartHomeStateUpdate(String deviceId) {
        synchronized (synchronizeSmartHomeJobScheduler) {
            requestedDeviceUpdates.add(deviceId);
            ScheduledFuture<?> refreshSmartHomeAfterCommandJob = this.refreshSmartHomeAfterCommandJob;
            if (refreshSmartHomeAfterCommandJob != null) {
                refreshSmartHomeAfterCommandJob.cancel(false);
            }
            this.refreshSmartHomeAfterCommandJob = scheduler.schedule(this::updateSmartHomeStateJob, 500,
                    TimeUnit.MILLISECONDS);
        }
    }

    private void updateSmartHomeStateJob() {
        Set<String> deviceUpdates = new HashSet<>();

        synchronized (synchronizeSmartHomeJobScheduler) {
            Connection connection = this.connection;
            if (connection == null || !connection.getIsLoggedIn()) {
                this.refreshSmartHomeAfterCommandJob = scheduler.schedule(this::updateSmartHomeStateJob, 1000,
                        TimeUnit.MILLISECONDS);
                return;
            }
            requestedDeviceUpdates.drainTo(deviceUpdates);
            this.refreshSmartHomeAfterCommandJob = null;
        }

        deviceUpdates.forEach(this::updateSmartHomeState);
    }

    private synchronized void updateSmartHomeState(@Nullable String deviceFilterId) {
        try {
            logger.trace("updateSmartHomeState started with deviceFilterId={}", deviceFilterId);
            Connection connection = this.connection;
            if (connection == null || !connection.getIsLoggedIn()) {
                return;
            }
            List<SmartHomeBaseDevice> allDevices = getLastKnownSmartHomeDevices();
            Set<SmartHomeBaseDevice> targetDevices = new HashSet<>();
            if (deviceFilterId != null) {
                allDevices.stream().filter(d -> deviceFilterId.equals(d.findId())).findFirst()
                        .ifPresent(targetDevices::add);
            } else {
                SmartHomeDeviceStateGroupUpdateCalculator smartHomeDeviceStateGroupUpdateCalculator = this.smartHomeDeviceStateGroupUpdateCalculator;
                if (smartHomeDeviceStateGroupUpdateCalculator == null) {
                    return;
                }
                if (smartHomeDeviceHandlers.isEmpty()) {
                    return;
                }
                List<JsonSmartHomeDevice> devicesToUpdate = new ArrayList<>();
                for (SmartHomeDeviceHandler device : smartHomeDeviceHandlers) {
                    String id = device.getId();
                    SmartHomeBaseDevice baseDevice = jsonIdSmartHomeDeviceMapping.get(id);
                    devicesToUpdate.addAll(SmartHomeDeviceHandler.getSupportedSmartHomeDevices(baseDevice, allDevices));
                }
                smartHomeDeviceStateGroupUpdateCalculator.removeDevicesWithNoUpdate(devicesToUpdate);
                targetDevices.addAll(devicesToUpdate);
                if (targetDevices.isEmpty()) {
                    return;
                }
            }
            Map<String, JsonArray> applianceIdToCapabilityStates = connection
                    .getSmartHomeDeviceStatesJson(targetDevices);

            for (SmartHomeDeviceHandler smartHomeDeviceHandler : smartHomeDeviceHandlers) {
                String id = smartHomeDeviceHandler.getId();
                if (requestedDeviceUpdates.contains(id)) {
                    logger.debug("Device update {} suspended", id);
                    continue;
                }
                if (deviceFilterId == null || id.equals(deviceFilterId)) {
                    smartHomeDeviceHandler.updateChannelStates(allDevices, applianceIdToCapabilityStates);
                } else {
                    logger.trace("Id {} not matching filter {}", id, deviceFilterId);
                }
            }

            logger.debug("updateSmartHomeState finished");
        } catch (JsonSyntaxException | ConnectionException e) {
            logger.debug("updateSmartHomeState fails", e);
        } catch (Exception e) { // this handler can be removed later, if we know that nothing else can fail.
            logger.warn("updateSmartHomeState fails with unexpected error", e);
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(AmazonEchoDiscovery.class, SmartHomeDevicesDiscovery.class);
    }
}
