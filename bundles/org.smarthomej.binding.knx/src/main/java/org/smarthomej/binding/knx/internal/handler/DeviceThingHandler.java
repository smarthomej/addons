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
package org.smarthomej.binding.knx.internal.handler;

import static org.smarthomej.binding.knx.internal.KNXBindingConstants.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.cache.ExpiringCacheMap;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.knx.internal.KNXBindingConstants;
import org.smarthomej.binding.knx.internal.channel.KNXChannel;
import org.smarthomej.binding.knx.internal.channel.KNXChannelFactory;
import org.smarthomej.binding.knx.internal.client.AbstractKNXClient;
import org.smarthomej.binding.knx.internal.client.DeviceInspector;
import org.smarthomej.binding.knx.internal.client.InboundSpec;
import org.smarthomej.binding.knx.internal.client.KNXClient;
import org.smarthomej.binding.knx.internal.client.OutboundSpec;
import org.smarthomej.binding.knx.internal.config.DeviceConfig;
import org.smarthomej.binding.knx.internal.dpt.DPTUtil;
import org.smarthomej.binding.knx.internal.dpt.ValueDecoder;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.datapoint.CommandDP;
import tuwien.auto.calimero.datapoint.Datapoint;

/**
 * The {@link DeviceThingHandler} is responsible for handling commands and state updates sent to and received from the
 * bus and updating the channels correspondingly.
 *
 * @author Simon Kaufmann - Initial contribution and API
 * @author Jan N. Klug - Refactored for performance
 */
@NonNullByDefault
public class DeviceThingHandler extends BaseThingHandler implements GroupAddressListener {
    private static final int INITIAL_PING_DELAY = 5;
    private final Logger logger = LoggerFactory.getLogger(DeviceThingHandler.class);

    private final Set<GroupAddress> groupAddresses = ConcurrentHashMap.newKeySet();
    private final ExpiringCacheMap<GroupAddress, @Nullable Boolean> groupAddressesWriteBlocked = new ExpiringCacheMap<>(
            Duration.ofMillis(1000));
    private final Map<GroupAddress, OutboundSpec> groupAddressesRespondingSpec = new ConcurrentHashMap<>();
    private final Map<GroupAddress, ScheduledFuture<?>> readFutures = new ConcurrentHashMap<>();
    private final Map<ChannelUID, ScheduledFuture<?>> channelFutures = new ConcurrentHashMap<>();
    private final Map<ChannelUID, KNXChannel> knxChannels = new ConcurrentHashMap<>();
    private final Random random = new Random();
    protected @Nullable IndividualAddress address;
    private int readInterval;
    private @Nullable ScheduledFuture<?> descriptionJob;
    private boolean filledDescription = false;
    private @Nullable ScheduledFuture<?> pollingJob;

    public DeviceThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        attachToClient();

        DeviceConfig config = getConfigAs(DeviceConfig.class);
        readInterval = config.getReadInterval();

        // gather all GAs from channel configurations and create channels
        getThing().getChannels().forEach(channel -> {
            KNXChannel knxChannel = KNXChannelFactory.createKnxChannel(channel);
            knxChannels.put(channel.getUID(), knxChannel);
            groupAddresses.addAll(knxChannel.getAllGroupAddresses());
        });
    }

    @Override
    public void dispose() {
        for (ChannelUID channelUID : channelFutures.keySet()) {
            channelFutures.computeIfPresent(channelUID, (k, v) -> {
                v.cancel(true);
                return null;
            });
        }

        groupAddresses.clear();
        groupAddressesWriteBlocked.clear();
        groupAddressesRespondingSpec.clear();
        knxChannels.clear();

        detachFromClient();
    }

    protected void cancelReadFutures() {
        for (GroupAddress groupAddress : readFutures.keySet()) {
            readFutures.computeIfPresent(groupAddress, (k, v) -> {
                v.cancel(true);
                return null;
            });
        }
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        KNXChannel knxChannel = knxChannels.get(channelUID);
        if (knxChannel == null) {
            logger.warn("Channel '{}' received a channel linked event, but no KNXChannel found", channelUID);
            return;
        }
        if (!knxChannel.isControl()) {
            scheduleRead(knxChannel);
        }
    }

    protected void scheduleReadJobs() {
        cancelReadFutures();
        for (KNXChannel knxChannel : knxChannels.values()) {
            if (isLinked(knxChannel.getChannelUID()) && !knxChannel.isControl()) {
                scheduleRead(knxChannel);
            }
        }
    }

    private void scheduleRead(KNXChannel knxChannel) {
        List<InboundSpec> readSpecs = knxChannel.getReadSpec();
        for (InboundSpec readSpec : readSpecs) {
            readSpec.getGroupAddresses().forEach(ga -> scheduleReadJob(ga, readSpec.getDPT()));
        }
    }

    private void scheduleReadJob(GroupAddress groupAddress, String dpt) {
        if (readInterval > 0) {
            ScheduledFuture<?> future = readFutures.get(groupAddress);
            if (future == null || future.isDone() || future.isCancelled()) {
                future = getScheduler().scheduleWithFixedDelay(() -> readDatapoint(groupAddress, dpt), 0, readInterval,
                        TimeUnit.SECONDS);
                readFutures.put(groupAddress, future);
            }
        } else {
            getScheduler().submit(() -> readDatapoint(groupAddress, dpt));
        }
    }

    private void readDatapoint(GroupAddress groupAddress, String dpt) {
        if (getClient().isConnected()) {
            if (DPTUtil.getAllowedTypes(dpt).isEmpty()) {
                logger.warn("DPT '{}' is not supported by the KNX binding", dpt);
                return;
            }
            Datapoint datapoint = new CommandDP(groupAddress, getThing().getUID().toString(), 0, dpt);
            getClient().readDatapoint(datapoint);
        }
    }

    @Override
    public boolean listensTo(GroupAddress destination) {
        return groupAddresses.contains(destination);
    }

    /** Handling commands triggered from openHAB */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("Handling command '{}' for channel '{}'", command, channelUID);
        KNXChannel knxChannel = knxChannels.get(channelUID);
        if (knxChannel == null) {
            logger.warn("Channel '{}' received command, but no KNXChannel found", channelUID);
            return;
        }
        if (command instanceof RefreshType && !knxChannel.isControl()) {
            logger.debug("Refreshing channel '{}'", channelUID);
            scheduleRead(knxChannel);
        } else {
            if (CHANNEL_RESET.equals(channelUID.getId())) {
                if (address != null) {
                    restart();
                }
            } else {
                try {
                    OutboundSpec commandSpec = knxChannel.getCommandSpec(command);
                    // only send GroupValueWrite to KNX if GA is not blocked once
                    if (commandSpec != null) {
                        GroupAddress destination = commandSpec.getGroupAddress();
                        if (knxChannel.isControl()) {
                            // always remember, otherwise we might send an old state
                            groupAddressesRespondingSpec.put(destination, commandSpec);
                        }
                        if (groupAddressesWriteBlocked.get(destination) != null) {
                            logger.debug("Write to {} blocked for 1s/one call after read.", destination);
                            groupAddressesWriteBlocked.invalidate(destination);
                        } else {
                            getClient().writeToKNX(commandSpec);
                        }
                    } else {
                        logger.debug(
                                "None of the configured GAs on channel '{}' could handle the command '{}' of type '{}'",
                                channelUID, command, command.getClass().getSimpleName());
                    }
                } catch (KNXException e) {
                    logger.warn("An error occurred while handling command '{}' on channel '{}': {}", command,
                            channelUID, e.getMessage());
                }
            }
        }
    }

    /** KNXIO */
    private void sendGroupValueResponse(ChannelUID channelUID, GroupAddress destination) {
        KNXChannel knxChannel = knxChannels.get(channelUID);
        if (knxChannel == null) {
            return;
        }
        Set<GroupAddress> rsa = knxChannel.getWriteAddresses();
        if (!rsa.isEmpty()) {
            logger.trace("onGroupRead size '{}'", rsa.size());
            OutboundSpec os = groupAddressesRespondingSpec.get(destination);
            if (os != null) {
                logger.trace("onGroupRead respondToKNX '{}'", os.getGroupAddress());
                /* KNXIO: sending real "GroupValueResponse" to the KNX bus. */
                try {
                    getClient().respondToKNX(os);
                } catch (KNXException e) {
                    logger.warn("An error occurred on channel {}: {}", channelUID, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * KNXIO, extended with the ability to respond on "GroupValueRead" telegrams with "GroupValueResponse" telegram
     */
    @Override
    public void onGroupRead(AbstractKNXClient client, IndividualAddress source, GroupAddress destination, byte[] asdu) {
        logger.trace("onGroupRead Thing '{}' received a GroupValueRead telegram from '{}' for destination '{}'",
                getThing().getUID(), source, destination);
        for (KNXChannel knxChannel : knxChannels.values()) {
            if (knxChannel.isControl()) {
                OutboundSpec responseSpec = knxChannel.getResponseSpec(destination, RefreshType.REFRESH);
                if (responseSpec != null) {
                    logger.trace("onGroupRead isControl -> postCommand");
                    // This event should be sent to KNX as GroupValueResponse immediately.
                    sendGroupValueResponse(knxChannel.getChannelUID(), destination);

                    // block write attempts for 1s or 1 request to prevent loops
                    if (!groupAddressesWriteBlocked.containsKey(destination)) {
                        groupAddressesWriteBlocked.put(destination, () -> null);
                    }
                    groupAddressesWriteBlocked.putValue(destination, true);

                    // Send REFRESH to openHAB to get this event for scripting with postCommand
                    // and remember to ignore/block this REFRESH to be sent back to KNX as GroupValueWrite after
                    // postCommand is done!
                    postCommand(knxChannel.getChannelUID(), RefreshType.REFRESH);
                }
            }
        }
    }

    @Override
    public void onGroupReadResponse(AbstractKNXClient client, IndividualAddress source, GroupAddress destination,
            byte[] asdu) {
        // GroupValueResponses are treated the same as GroupValueWrite telegrams
        logger.trace("onGroupReadResponse Thing '{}' processes a GroupValueResponse telegram for destination '{}'",
                getThing().getUID(), destination);
        onGroupWrite(client, source, destination, asdu);
    }

    /**
     * KNXIO, here value changes are set, coming from KNX OR openHAB.
     */
    @Override
    public void onGroupWrite(AbstractKNXClient client, IndividualAddress source, GroupAddress destination,
            byte[] asdu) {
        logger.debug("onGroupWrite Thing '{}' received a GroupValueWrite telegram from '{}' for destination '{}'",
                getThing().getUID(), source, destination);

        for (KNXChannel knxChannel : knxChannels.values()) {
            InboundSpec listenSpec = knxChannel.getListenSpec(destination);
            if (listenSpec != null) {
                logger.trace(
                        "onGroupWrite Thing '{}' processes a GroupValueWrite telegram for destination '{}' for channel '{}'",
                        getThing().getUID(), destination, knxChannel.getChannelUID());
                /**
                 * Remember current KNXIO outboundSpec only if it is a control channel.
                 */
                if (knxChannel.isControl()) {
                    logger.trace("onGroupWrite isControl");
                    Type value = ValueDecoder.decode(listenSpec.getDPT(), asdu, knxChannel.preferredType());
                    if (value != null) {
                        OutboundSpec commandSpec = knxChannel.getCommandSpec(value);
                        if (commandSpec != null) {
                            groupAddressesRespondingSpec.put(destination, commandSpec);
                        }
                    }
                }
                processDataReceived(destination, asdu, listenSpec, knxChannel);
            }
        }
    }

    private void processDataReceived(GroupAddress destination, byte[] asdu, InboundSpec listenSpec,
            KNXChannel knxChannel) {
        if (DPTUtil.getAllowedTypes(listenSpec.getDPT()).isEmpty()) {
            logger.warn("DPT '{}' is not supported by the KNX binding.", listenSpec.getDPT());
            return;
        }

        Type value = ValueDecoder.decode(listenSpec.getDPT(), asdu, knxChannel.preferredType());
        if (value != null) {
            if (knxChannel.isControl()) {
                ChannelUID channelUID = knxChannel.getChannelUID();
                int frequency;
                if (KNXBindingConstants.CHANNEL_DIMMER_CONTROL.equals(knxChannel.getChannelType())) {
                    // if we have a dimmer control channel, check if a frequency is defined
                    Channel channel = getThing().getChannel(channelUID);
                    if (channel == null) {
                        logger.warn("Failed to find channel for ChannelUID '{}'", channelUID);
                        return;
                    }
                    frequency = ((BigDecimal) Objects.requireNonNullElse(
                            channel.getConfiguration().get(KNXBindingConstants.REPEAT_FREQUENCY), BigDecimal.ZERO))
                                    .intValue();
                } else {
                    // disable dimming by binding
                    frequency = 0;
                }
                if ((value instanceof UnDefType || value instanceof IncreaseDecreaseType) && frequency > 0) {
                    // continuous dimming by the binding
                    // cancel a running scheduler before adding a new (and only add if not UnDefType)
                    ScheduledFuture<?> oldFuture = channelFutures.remove(channelUID);
                    if (oldFuture != null) {
                        oldFuture.cancel(true);
                    }
                    if (value instanceof IncreaseDecreaseType) {
                        channelFutures.put(channelUID, scheduler.scheduleWithFixedDelay(
                                () -> postCommand(channelUID, (Command) value), 0, frequency, TimeUnit.MILLISECONDS));
                    }
                } else {
                    if (value instanceof Command) {
                        logger.trace("processDataReceived postCommand new value '{}' for GA '{}'", asdu, address);
                        postCommand(channelUID, (Command) value);
                    }
                }
            } else {
                if (value instanceof State && !(value instanceof UnDefType)) {
                    updateState(knxChannel.getChannelUID(), (State) value);
                }
            }
        } else {
            logger.debug(
                    "Ignoring KNX bus data for channel '{}': couldn't transform to any Type (GA='{}', DPT='{}', data='{}')",
                    knxChannel.getChannelUID(), destination, listenSpec.getDPT(), HexUtils.bytesToHex(asdu));
        }
    }

    protected final ScheduledExecutorService getScheduler() {
        return getBridgeHandler().getScheduler();
    }

    protected final ScheduledExecutorService getBackgroundScheduler() {
        return getBridgeHandler().getBackgroundScheduler();
    }

    protected final KNXBridgeBaseThingHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            KNXBridgeBaseThingHandler handler = (KNXBridgeBaseThingHandler) bridge.getHandler();
            if (handler != null) {
                return handler;
            }
        }
        throw new IllegalStateException("The bridge must not be null and must be initialized");
    }

    protected final KNXClient getClient() {
        return getBridgeHandler().getClient();
    }

    protected final boolean describeDevice(@Nullable IndividualAddress address) {
        if (address == null) {
            return false;
        }
        DeviceInspector inspector = new DeviceInspector(getClient().getDeviceInfoClient(), address);
        DeviceInspector.Result result = inspector.readDeviceInfo();
        if (result != null) {
            Map<String, String> properties = editProperties();
            properties.putAll(result.getProperties());
            updateProperties(properties);
            return true;
        }
        return false;
    }

    protected final void restart() {
        if (address != null) {
            getClient().restartNetworkDevice(address);
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            attachToClient();
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            detachFromClient();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    private void pollDeviceStatus() {
        try {
            if (address != null && getClient().isConnected()) {
                logger.debug("Polling individual address '{}'", address);
                boolean isReachable = getClient().isReachable(address);
                if (isReachable) {
                    updateStatus(ThingStatus.ONLINE);
                    DeviceConfig config = getConfigAs(DeviceConfig.class);
                    if (!filledDescription && config.getFetch()) {
                        Future<?> descriptionJob = this.descriptionJob;
                        if (descriptionJob == null || descriptionJob.isCancelled()) {
                            long initialDelay = Math.round(config.getPingInterval() * random.nextFloat());
                            this.descriptionJob = getBackgroundScheduler().schedule(() -> {
                                filledDescription = describeDevice(address);
                            }, initialDelay, TimeUnit.SECONDS);
                        }
                    }
                } else {
                    updateStatus(ThingStatus.OFFLINE);
                }
            }
        } catch (KNXException e) {
            logger.debug("An error occurred while testing the reachability of a thing '{}'", getThing().getUID(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
        }
    }

    protected void attachToClient() {
        if (!getClient().isConnected()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        DeviceConfig config = getConfigAs(DeviceConfig.class);
        try {
            String configAddress = config.getAddress();
            if (configAddress != null && !configAddress.isEmpty()) {
                updateStatus(ThingStatus.UNKNOWN);
                address = new IndividualAddress(config.getAddress());

                long pingInterval = config.getPingInterval();
                long initialPingDelay = Math.round(INITIAL_PING_DELAY * random.nextFloat());

                ScheduledFuture<?> pollingJob = this.pollingJob;
                if ((pollingJob == null || pollingJob.isCancelled())) {
                    logger.debug("'{}' will be polled every {}s", getThing().getUID(), pingInterval);
                    this.pollingJob = getBackgroundScheduler().scheduleWithFixedDelay(this::pollDeviceStatus,
                            initialPingDelay, pingInterval, TimeUnit.SECONDS);
                }
            } else {
                updateStatus(ThingStatus.ONLINE);
            }
        } catch (KNXFormatException e) {
            logger.debug("An exception occurred while setting the individual address '{}'", config.getAddress(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getLocalizedMessage());
        }
        getClient().registerGroupAddressListener(this);
        scheduleReadJobs();
    }

    protected void detachFromClient() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null) {
            pollingJob.cancel(true);
            this.pollingJob = null;
        }

        ScheduledFuture<?> descriptionJob = this.descriptionJob;
        if (descriptionJob != null) {
            descriptionJob.cancel(true);
            this.descriptionJob = null;
        }
        cancelReadFutures();
        Bridge bridge = getBridge();
        if (bridge != null) {
            KNXBridgeBaseThingHandler handler = (KNXBridgeBaseThingHandler) bridge.getHandler();
            if (handler != null) {
                handler.getClient().unregisterGroupAddressListener(this);
            }
        }
    }
}
