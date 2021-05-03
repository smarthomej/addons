/**
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
package org.smarthomej.binding.tcpudp.internal;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tcpudp.internal.config.ReceiverConfiguration;
import org.smarthomej.binding.tcpudp.internal.config.TcpUdpChannelConfig;
import org.smarthomej.binding.tcpudp.internal.receiver.Receiver;
import org.smarthomej.binding.tcpudp.internal.receiver.ReceiverListener;
import org.smarthomej.binding.tcpudp.internal.receiver.TcpReceiver;
import org.smarthomej.binding.tcpudp.internal.receiver.UdpReceiver;
import org.smarthomej.commons.itemvalueconverter.ContentWrapper;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link ReceiverThingHandler} is a teh thing handler for receiver type things
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ReceiverThingHandler extends BaseTcpUdpThingHandler implements ReceiverListener {
    private final Logger logger = LoggerFactory.getLogger(ReceiverThingHandler.class);

    private final Set<Receiver.ContentListener> contentListeners = new HashSet<>();
    private final Map<ChannelUID, State> stateCache = new ConcurrentHashMap<>();

    private @Nullable Future<?> refreshJob;
    private @Nullable Receiver receiver;

    protected ReceiverConfiguration config = new ReceiverConfiguration();

    public ReceiverThingHandler(Thing thing, ValueTransformationProvider valueTransformationProvider) {
        super(thing, valueTransformationProvider);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            State state = stateCache.getOrDefault(channelUID, UnDefType.UNDEF);
            updateState(channelUID, state);
        } else {
            logger.debug("Writing to read-only channel {} not permitted", channelUID);
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(ReceiverConfiguration.class);

        if (config.port == 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Parameter port must not be 0!");
            return;
        }

        Receiver receiver;
        // set methods depending on thing-type
        if (config.protocol == ReceiverConfiguration.Protocol.UDP) {
            logger.debug("Configured '{}' for UDP connections.", thing.getUID());
            receiver = new UdpReceiver(this, config.localAddress, config.port, config.bufferSize);
        } else if (config.protocol == ReceiverConfiguration.Protocol.TCP) {
            logger.debug("Configured '{}' for TCP connections.", thing.getUID());
            receiver = new TcpReceiver(this, config.localAddress, config.port, config.bufferSize);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Protocol for connection not set!");
            return;
        }

        thing.getChannels().forEach(channel -> {
            TcpUdpChannelConfig channelConfig = channel.getConfiguration().as(TcpUdpChannelConfig.class);
            createItemValueConverter(channel.getUID(), channel.getAcceptedItemType(), channelConfig)
                    .ifPresent(itemValueConverter -> contentListeners
                            .add(new Receiver.ContentListener(itemValueConverter, channelConfig.addressFilter)));
        });

        if (contentListeners.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No channels defined.");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);

        this.receiver = receiver;
        this.refreshJob = scheduler.submit(receiver);
    }

    @Override
    protected void updateState(ChannelUID channelUID, State state) {
        // cache the value to report it on refresh
        stateCache.put(channelUID, state);
        super.updateState(channelUID, state);
    }

    public void dispose() {
        Receiver receiver = this.receiver;
        if (receiver != null) {
            receiver.stop();
        }
        Future<?> refreshJob = this.refreshJob;
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
        contentListeners.clear();
        stateCache.clear();
        super.dispose();
    }

    private String getEncoding() {
        return Objects.requireNonNullElse(config.encoding, StandardCharsets.UTF_8.name());
    }

    @Override
    public void onReceive(String sender, byte[] content) {
        ContentWrapper contentWrapper = new ContentWrapper(content, getEncoding(), null);

        contentListeners.stream().filter(listener -> listener.addressFilter.matcher(sender).matches())
                .forEach(listener -> listener.itemValueConverter.process(contentWrapper));
    }

    @Override
    public void reportConnectionState(boolean state, @Nullable String message) {
        if (state) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, message);
        }
    }
}
