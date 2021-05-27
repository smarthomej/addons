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
package org.smarthomej.binding.knx.internal.handler;

import static org.smarthomej.binding.knx.internal.KNXBindingConstants.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.knx.internal.KNXBindingConstants;
import org.smarthomej.binding.knx.internal.channel.KNXChannelType;
import org.smarthomej.binding.knx.internal.channel.KNXChannelTypes;
import org.smarthomej.binding.knx.internal.client.AbstractKNXClient;
import org.smarthomej.binding.knx.internal.client.InboundSpec;
import org.smarthomej.binding.knx.internal.client.OutboundSpec;
import org.smarthomej.binding.knx.internal.config.DeviceConfig;
import org.smarthomej.binding.knx.internal.dpt.KNXCoreTypeMapper;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.datapoint.CommandDP;
import tuwien.auto.calimero.datapoint.Datapoint;

/**
 * The {@link DeviceThingHandler} is responsible for handling commands and state updates sent to and received from the
 * bus and updating the channels correspondingly.
 *
 * @author Simon Kaufmann - Initial contribution and API
 */
@NonNullByDefault
public class DeviceThingHandler extends AbstractKNXThingHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceThingHandler.class);

    private final Set<GroupAddress> groupAddresses = ConcurrentHashMap.newKeySet();
    private final Set<GroupAddress> groupAddressesWriteBlockedOnce = ConcurrentHashMap.newKeySet();
    private final Set<OutboundSpec> groupAddressesRespondingSpec = ConcurrentHashMap.newKeySet();
    private final Map<GroupAddress, ScheduledFuture<?>> readFutures = new ConcurrentHashMap<>();
    private final Map<ChannelUID, ScheduledFuture<?>> channelFutures = new ConcurrentHashMap<>();
    private final Map<ChannelUID, KNXChannel> knxchannels = new ConcurrentHashMap<>();
    private int readInterval;

    public DeviceThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        super.initialize();
        DeviceConfig config = getConfigAs(DeviceConfig.class);
        readInterval = config.getReadInterval();

        // gather all GAs from channel configurations
        getThing().getChannels().forEach(channel -> {
            knxchannels.put(channel.getUID(), new KNXChannel(channel));
            applyChannelFunction(channel.getUID(), (knxChannel) -> groupAddresses
                    .addAll(knxChannel.channelType.getAllGroupAddresses(knxChannel.configuration)));
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
        groupAddressesWriteBlockedOnce.clear();
        groupAddressesRespondingSpec.clear();
        knxchannels.clear();

        super.dispose();
    }

    @Override
    protected void cancelReadFutures() {
        for (GroupAddress groupAddress : readFutures.keySet()) {
            readFutures.computeIfPresent(groupAddress, (k, v) -> {
                v.cancel(true);
                return null;
            });
        }
    }

    @FunctionalInterface
    private interface ChannelFunction {
        void apply(KNXChannel knxChannel) throws KNXException;
    }

    private void applyChannelFunction(ChannelUID channelUID, ChannelFunction function) {
        KNXChannel channel = knxchannels.get(channelUID);
        if (channel == null) {
            logger.warn("Channel '{}' does not exist", channelUID);
            return;
        }
        try {
            function.apply(channel);
        } catch (KNXException e) {
            logger.warn("An error occurred on channel {}: {}", channelUID, e.getMessage(), e);
        }
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        if (!isControl(channelUID)) {
            applyChannelFunction(channelUID, this::scheduleRead);
        }
    }

    @Override
    protected void scheduleReadJobs() {
        cancelReadFutures();
        for (Channel channel : getThing().getChannels()) {
            ChannelUID channelUID = channel.getUID();
            if (isLinked(channelUID) && !isControl(channelUID)) {
                applyChannelFunction(channelUID, this::scheduleRead);
            }
        }
    }

    private void scheduleRead(KNXChannel knxChannel) {
        List<InboundSpec> readSpecs = knxChannel.channelType.getReadSpec(knxChannel.configuration);
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
            if (!isDPTSupported(dpt)) {
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

    /** KNXIO remember controls, removeIf may be null */
    private void rememberRespondingSpec(OutboundSpec commandSpec) {
        GroupAddress ga = commandSpec.getGroupAddress();
        if (ga != null) {
            groupAddressesRespondingSpec.removeIf(spec -> spec.matchesDestination(ga));
        }
        groupAddressesRespondingSpec.add(commandSpec);
        logger.trace("rememberRespondingSpec handled commandSpec for '{}' size '{}'", ga,
                groupAddressesRespondingSpec.size());
    }

    /** Handling commands triggered from openHAB */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("Handling command '{}' for channel '{}'", command, channelUID);
        if (command instanceof RefreshType && !isControl(channelUID)) {
            logger.debug("Refreshing channel '{}'", channelUID);
            applyChannelFunction(channelUID, this::scheduleRead);
        } else {
            if (CHANNEL_RESET.equals(channelUID.getId())) {
                if (address != null) {
                    restart();
                }
            } else {
                applyChannelFunction(channelUID, (knxChannel) -> {
                    OutboundSpec commandSpec = knxChannel.channelType.getCommandSpec(knxChannel.configuration, command);
                    // only send GroupValueWrite to KNX if GA is not blocked once
                    if (commandSpec != null && !groupAddressesWriteBlockedOnce.remove(commandSpec.getGroupAddress())) {
                        getClient().writeToKNX(commandSpec);
                        if (isControl(channelUID)) {
                            rememberRespondingSpec(commandSpec);
                        }
                    } else {
                        logger.debug(
                                "None of the configured GAs on channel '{}' could handle the command '{}' of type '{}'",
                                channelUID, command, command.getClass().getSimpleName());
                    }
                });
            }
        }
    }

    private boolean isControl(ChannelUID channelUID) {
        ChannelTypeUID channelTypeUID = getChannelTypeUID(channelUID);
        return CONTROL_CHANNEL_TYPES.contains(channelTypeUID.getId());
    }

    private ChannelTypeUID getChannelTypeUID(ChannelUID channelUID) {
        Channel channel = getThing().getChannel(channelUID.getId());
        Objects.requireNonNull(channel);
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        Objects.requireNonNull(channelTypeUID);
        return channelTypeUID;
    }

    /** KNXIO */
    private void sendGroupValueResponse(Channel channel, GroupAddress destination) {
        ChannelUID channelUID = channel.getUID();
        KNXChannel knxChannel = knxchannels.get(channelUID);
        if (knxChannel == null) {
            return;
        }
        Set<GroupAddress> rsa = knxChannel.channelType.getWriteAddresses(knxChannel.configuration);
        if (!rsa.isEmpty()) {
            logger.trace("onGroupRead size '{}'", rsa.size());
            Optional<OutboundSpec> os = groupAddressesRespondingSpec.stream()
                    .filter(spec -> spec.matchesDestination(destination)).findFirst();
            if (os.isPresent()) {
                logger.trace("onGroupRead respondToKNX '{}'", os.get().getGroupAddress());
                /* KNXIO: sending real "GroupValueResponse" to the KNX bus. */
                try {
                    getClient().respondToKNX(os.get());
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
        for (Channel channel : getThing().getChannels()) {
            ChannelUID channelUID = channel.getUID();
            if (isControl(channelUID)) {
                applyChannelFunction(channelUID, (knxChannel) -> {
                    OutboundSpec responseSpec = knxChannel.channelType.getResponseSpec(knxChannel.configuration,
                            destination, RefreshType.REFRESH);
                    if (responseSpec != null) {
                        logger.trace("onGroupRead isControl -> postCommand");
                        // This event should be sent to KNX as GroupValueResponse immediately.
                        sendGroupValueResponse(channel, destination);
                        // Send REFRESH to openHAB to get this event for scripting with postCommand
                        // and remember to ignore/block this REFRESH to be sent back to KNX as GroupValueWrite after
                        // postCommand is done!
                        groupAddressesWriteBlockedOnce.add(destination);
                        postCommand(channelUID, RefreshType.REFRESH);
                    }
                });
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

        for (Channel channel : getThing().getChannels()) {
            ChannelUID channelUID = channel.getUID();
            applyChannelFunction(channelUID, (knxChannel) -> {
                InboundSpec listenSpec = knxChannel.channelType.getListenSpec(knxChannel.configuration, destination);
                if (listenSpec != null) {
                    logger.trace(
                            "onGroupWrite Thing '{}' processes a GroupValueWrite telegram for destination '{}' for channel '{}'",
                            getThing().getUID(), destination, channelUID);
                    /**
                     * Remember current KNXIO outboundSpec only if it is a control channel.
                     */
                    if (isControl(channelUID)) {
                        logger.trace("onGroupWrite isControl");
                        Type value = KNXCoreTypeMapper.convertRawDataToType(listenSpec.getDPT(), asdu);
                        if (value != null) {
                            OutboundSpec commandSpec = knxChannel.channelType.getCommandSpec(knxChannel.configuration,
                                    value);
                            if (commandSpec != null) {
                                rememberRespondingSpec(commandSpec);
                            }
                        }
                    }
                    processDataReceived(destination, asdu, listenSpec, knxChannel);
                }
            });
        }
    }

    private void processDataReceived(GroupAddress destination, byte[] asdu, InboundSpec listenSpec,
            KNXChannel channel) {
        if (!isDPTSupported(listenSpec.getDPT())) {
            logger.warn("DPT '{}' is not supported by the KNX binding.", listenSpec.getDPT());
            return;
        }

        Type value = KNXCoreTypeMapper.convertRawDataToType(listenSpec.getDPT(), asdu);
        if (value != null) {
            if (isControl(channel.channelUID)) {
                Object repeat = channel.configuration.get(KNXBindingConstants.REPEAT_FREQUENCY);
                int frequency = repeat != null ? ((BigDecimal) repeat).intValue() : 0;
                if (KNXBindingConstants.CHANNEL_DIMMER_CONTROL.equals(getChannelTypeUID(channel.channelUID).getId())
                        && (value instanceof UnDefType || value instanceof IncreaseDecreaseType) && frequency > 0) {
                    // continuous dimming by the binding
                    if (UnDefType.UNDEF.equals(value)) {
                        channelFutures.computeIfPresent(channel.channelUID, (k, v) -> {
                            v.cancel(false);
                            return null;
                        });
                    } else if (value instanceof IncreaseDecreaseType) {
                        channelFutures.compute(channel.channelUID, (k, v) -> {
                            if (v != null) {
                                v.cancel(true);
                            }
                            return scheduler.scheduleWithFixedDelay(
                                    () -> postCommand(channel.channelUID, (Command) value), 0, frequency,
                                    TimeUnit.MILLISECONDS);
                        });
                    }
                } else {
                    if (value instanceof Command) {
                        logger.trace("processDataReceived postCommand new value '{}' for GA '{}'", asdu, address);
                        postCommand(channel.channelUID, (Command) value);
                    }
                }
            } else {
                if (value instanceof State) {
                    updateState(channel.channelUID, (State) value);
                }
            }
        } else {
            String s = asduToHex(asdu);
            logger.warn("Ignoring KNX bus data: couldn't transform to any Type (destination='{}', dpt='{}', data='{}')",
                    destination, listenSpec.getDPT(), s);
        }
    }

    private boolean isDPTSupported(String dpt) {
        return !KNXCoreTypeMapper.getAllowedTypes(dpt).isEmpty();
    }

    private static class KNXChannel {
        private final ChannelUID channelUID;
        private final KNXChannelType channelType;
        private final Configuration configuration;

        public KNXChannel(Channel channel) {
            this.channelUID = channel.getUID();
            this.channelType = KNXChannelTypes.getKnxChannelType(channel);
            this.configuration = channel.getConfiguration();
        }
    }
}
