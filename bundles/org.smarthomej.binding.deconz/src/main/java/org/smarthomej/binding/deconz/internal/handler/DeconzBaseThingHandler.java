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
package org.smarthomej.binding.deconz.internal.handler;

import static org.smarthomej.binding.deconz.internal.BindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.deconz.internal.Util;
import org.smarthomej.binding.deconz.internal.dto.DeconzBaseMessage;
import org.smarthomej.binding.deconz.internal.netutils.WebSocketConnection;
import org.smarthomej.binding.deconz.internal.netutils.WebSocketMessageListener;
import org.smarthomej.binding.deconz.internal.types.ResourceType;
import org.smarthomej.commons.UpdatingBaseThingHandler;

import com.google.gson.Gson;

/**
 * This base thing doesn't establish any connections, that is done by the bridge Thing.
 *
 * It waits for the bridge to come online, grab the websocket connection and bridge configuration
 * and registers to the websocket connection as a listener.
 **
 * @author David Graeff - Initial contribution
 * @author Jan N. Klug - Refactored to abstract class
 */
@NonNullByDefault
public abstract class DeconzBaseThingHandler extends UpdatingBaseThingHandler implements WebSocketMessageListener {
    private final Logger logger = LoggerFactory.getLogger(DeconzBaseThingHandler.class);
    protected final ResourceType resourceType;
    protected ThingConfig config = new ThingConfig();
    protected final Gson gson;

    private @Nullable ScheduledFuture<?> initializationJob;
    private @Nullable ScheduledFuture<?> lastSeenPollingJob;
    protected @Nullable WebSocketConnection connection;

    public DeconzBaseThingHandler(Thing thing, Gson gson, ResourceType resourceType) {
        super(thing);
        this.gson = gson;
        this.resourceType = resourceType;
    }

    /**
     * Stops the initialization request
     */
    private void stopInitializationJob() {
        ScheduledFuture<?> future = initializationJob;
        if (future != null) {
            future.cancel(true);
            initializationJob = null;
        }
    }

    /**
     * Stops the last_seen polling
     */
    private void stopLastSeenPollingJob() {
        ScheduledFuture<?> future = lastSeenPollingJob;
        if (future != null) {
            future.cancel(true);
            lastSeenPollingJob = null;
        }
    }

    private void registerListener() {
        WebSocketConnection conn = connection;
        if (conn != null) {
            conn.registerListener(resourceType, config.id, this);
        }
    }

    private void unregisterListener() {
        WebSocketConnection conn = connection;
        if (conn != null) {
            conn.unregisterListener(resourceType, config.id);
        }
    }

    private @Nullable DeconzBridgeHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return null;
        }
        return (DeconzBridgeHandler) bridge.getHandler();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (config.id.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "ID not set");
            return;
        }

        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            // the bridge is ONLINE, we can communicate with the gateway, so we update the connection parameters and
            // register the listener
            DeconzBridgeHandler bridgeHandler = getBridgeHandler();
            if (bridgeHandler == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                return;
            }

            final WebSocketConnection webSocketConnection = bridgeHandler.getWebSocketConnection();
            this.connection = webSocketConnection;

            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE);

            // Real-time data
            registerListener();

            // get initial values
            requestState(this::processStateResponse);
        } else {
            // if the bridge is not ONLINE, we assume communication is not possible, so we unregister the listener and
            // set the thing status to OFFLINE
            unregisterListener();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    /**
     * processes a newly received (initial) state response
     *
     * MUST set the thing status!
     *
     * @param stateResponse
     */
    protected abstract void processStateResponse(DeconzBaseMessage stateResponse);

    /**
     * Perform a request to the REST API for retrieving the full state with all data and configuration.
     */
    protected void requestState(Consumer<DeconzBaseMessage> processor) {
        DeconzBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            bridgeHandler.getBridgeFullState()
                    .thenAccept(f -> f.map(s -> s.getMessage(resourceType, config.id)).ifPresentOrElse(message -> {
                        logger.trace("{} processing {}", thing.getUID(), message);
                        processor.accept(message);
                    }, () -> {
                        if (initializationJob != null) {
                            stopInitializationJob();
                            initializationJob = scheduler.schedule(() -> requestState(this::processStateResponse), 10,
                                    TimeUnit.SECONDS);
                        }
                    }));
        }
    }

    /**
     * create a channel on the current thing
     *
     * @param thingBuilder a ThingBuilder instance for this thing
     * @param channelId the channel id
     * @param kind the channel kind (STATE or TRIGGER)
     * @return true if the thing was modified
     */
    protected boolean createChannel(ThingBuilder thingBuilder, String channelId, ChannelKind kind) {
        if (thing.getChannel(channelId) != null) {
            // channel already exists, no update necessary
            return false;
        }

        ChannelUID channelUID = new ChannelUID(thing.getUID(), channelId);
        ChannelTypeUID channelTypeUID;
        switch (channelId) {
            case CHANNEL_BATTERY_LEVEL:
                channelTypeUID = new ChannelTypeUID("system:battery-level");
                break;
            case CHANNEL_BATTERY_LOW:
                channelTypeUID = new ChannelTypeUID("system:low-battery");
                break;
            default:
                channelTypeUID = new ChannelTypeUID(BINDING_ID, channelId);
                break;
        }

        ThingHandlerCallback callback = getCallback();
        if (callback != null) {
            Channel channel = callback.createChannelBuilder(channelUID, channelTypeUID).withKind(kind).build();
            thingBuilder.withChannel(channel);
            logger.trace("Added '{}' to thing '{}'", channelId, thing.getUID());

            return true;
        }

        logger.warn("Could not create channel '{}' for thing '{}'", channelUID, thing.getUID());
        return false;
    }

    /**
     * check if we need to add a last seen channel (called from processStateResponse only)
     *
     * @param thingBuilder a ThingBuilder instance for this thing
     * @param lastSeen the lastSeen string of a deconz message
     * @return true if the thing was modified
     */
    protected boolean checkLastSeen(ThingBuilder thingBuilder, @Nullable String lastSeen) {
        // "Last seen" is the last "ping" from the device, whereas "last update" is the last status changed.
        // For example, for a fire sensor, the device pings regularly, without necessarily updating channels.
        // So to monitor a sensor is still alive, the "last seen" is necessary.
        // Because "last seen" is never updated by the WebSocket API we have to
        // manually poll it after the defined time if supported by the device
        stopLastSeenPollingJob();
        boolean thingEdited = false;
        if (lastSeen != null && config.lastSeenPolling > 0) {
            thingEdited = createChannel(thingBuilder, CHANNEL_LAST_SEEN, ChannelKind.STATE);
            updateState(CHANNEL_LAST_SEEN, Util.convertTimestampToDateTime(lastSeen));
            lastSeenPollingJob = scheduler.schedule(() -> requestState(this::processLastSeen), config.lastSeenPolling,
                    TimeUnit.MINUTES);
            logger.trace("lastSeen polling enabled for thing {} with interval of {} minutes", thing.getUID(),
                    config.lastSeenPolling);
        } else if (thing.getChannel(CHANNEL_LAST_SEEN) != null) {
            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNEL_LAST_SEEN));
            thingEdited = true;
        }

        return thingEdited;
    }

    private void processLastSeen(DeconzBaseMessage stateResponse) {
        String lastSeen = stateResponse.lastseen;
        if (lastSeen != null) {
            updateState(CHANNEL_LAST_SEEN, Util.convertTimestampToDateTime(lastSeen));
        }
    }

    /**
     * sends a command to the bridge with the default command URL
     *
     * @param object must be serializable and contain the command
     * @param originalCommand the original openHAB command (used for logging purposes)
     * @param channelUID the channel that this command was send to (used for logging purposes)
     * @param acceptProcessing additional processing after the command was successfully send (might be null)
     */
    protected void sendCommand(@Nullable Object object, Command originalCommand, ChannelUID channelUID,
            @Nullable Runnable acceptProcessing) {
        sendCommand(object, originalCommand, channelUID, resourceType.getCommandUrl(), acceptProcessing);
    }

    /**
     * sends a command to the bridge with a caller-defined command URL
     *
     * @param object must be serializable and contain the command
     * @param originalCommand the original openHAB command (used for logging purposes)
     * @param channelUID the channel that this command was send to (used for logging purposes)
     * @param commandUrl the command URL
     * @param acceptProcessing additional processing after the command was successfully send (might be null)
     */
    protected void sendCommand(@Nullable Object object, Command originalCommand, ChannelUID channelUID,
            String commandUrl, @Nullable Runnable acceptProcessing) {
        DeconzBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }
        String endpoint = Stream.of(resourceType.getIdentifier(), config.id, commandUrl)
                .collect(Collectors.joining("/"));

        bridgeHandler.sendObject(endpoint, object, HttpMethod.PUT).thenAccept(v -> {
            if (acceptProcessing != null) {
                acceptProcessing.run();
            }
            if (v.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                logger.warn("Sending command {} to channel {} failed: {} - {}", originalCommand, channelUID,
                        v.getResponseCode(), v.getBody());
            } else {
                logger.trace("Result code={}, body={}", v.getResponseCode(), v.getBody());
            }
        }).exceptionally(e -> {
            logger.warn("Sending command {} to channel {} failed: {} - {}", originalCommand, channelUID, e.getClass(),
                    e.getMessage());
            return null;
        });
    }

    public void doNetwork(@Nullable Object object, String commandUrl, HttpMethod httpMethod,
            @Nullable Consumer<String> acceptProcessing) {
        DeconzBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }
        String endpoint = Stream.of(resourceType.getIdentifier(), config.id, commandUrl)
                .collect(Collectors.joining("/"));

        bridgeHandler.sendObject(endpoint, object, httpMethod).thenAccept(v -> {
            if (v.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                logger.warn("Sending {} via {} to {} failed: {} - {}", object, httpMethod, commandUrl,
                        v.getResponseCode(), v.getBody());
            } else {
                logger.trace("Result code={}, body={}", v.getResponseCode(), v.getBody());
                if (acceptProcessing != null) {
                    acceptProcessing.accept(v.getBody());
                }
            }
        }).exceptionally(e -> {
            logger.warn("Sending {} via {} to {} failed: {} - {}", object, httpMethod, commandUrl, e.getClass(),
                    e.getMessage());
            return null;
        });
    }

    @Override
    public void dispose() {
        stopInitializationJob();
        stopLastSeenPollingJob();
        unregisterListener();
        super.dispose();
    }

    @Override
    public void initialize() {
        config = getConfigAs(ThingConfig.class);

        Bridge bridge = getBridge();
        if (bridge != null) {
            bridgeStatusChanged(bridge.getStatusInfo());
        }
    }
}
