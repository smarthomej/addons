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
package org.smarthomej.binding.tcpudp.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.generic.ChannelHandler;
import org.openhab.core.thing.binding.generic.ChannelHandlerContent;
import org.openhab.core.thing.binding.generic.ChannelMode;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tcpudp.internal.config.ClientConfiguration;
import org.smarthomej.binding.tcpudp.internal.config.TcpUdpChannelConfig;
import org.smarthomej.commons.SimpleDynamicStateDescriptionProvider;

/**
 * The {@link ClientThingHandler} is the thing handler for client type things
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ClientThingHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(ClientThingHandler.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("SHJ-tcpudp");

    private final SimpleDynamicStateDescriptionProvider dynamicStateDescriptionProvider;
    private final Map<ChannelUID, ChannelHandler> channels = new HashMap<>();
    private final Map<ChannelUID, String> readCommands = new HashMap<>();

    private Function<String, Optional<ChannelHandlerContent>> doSyncRequest = this::doTcpSyncRequest;
    private final ChannelHandlerFactory channelHandlerFactory;
    private @Nullable ScheduledFuture<?> refreshJob = null;

    protected ClientConfiguration config = new ClientConfiguration();

    public ClientThingHandler(Thing thing, SimpleDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        super(thing);
        this.dynamicStateDescriptionProvider = dynamicStateDescriptionProvider;

        channelHandlerFactory = new ChannelHandlerFactory(this::updateState, this::postCommand, null);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        ChannelHandler itemValueConverter = channels.get(channelUID);
        if (itemValueConverter == null) {
            logger.warn("Cannot find channel implementation for channel {}.", channelUID);
            return;
        }

        if (command instanceof RefreshType) {
            String stateContent = readCommands.get(channelUID);
            if (stateContent != null) {
                // return fast in handleCommand
                scheduler.execute(() -> refreshChannel(channelUID, stateContent));
            } else {
                logger.warn("Could not find stateContent for channel, '{}', REFRESH command failed.", channelUID);
            }
        } else {
            try {
                itemValueConverter.send(command);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to convert command '{}' to channel '{}' for sending", command, channelUID);
            } catch (IllegalStateException e) {
                logger.debug("Writing to read-only channel {} not permitted", channelUID);
            }
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(ClientConfiguration.class);

        if (config.host.isEmpty() || config.port == 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Parameter host must not be empty and port must not be 0!");
            return;
        }

        // set methods depending on thing-type
        if (config.protocol == ClientConfiguration.Protocol.UDP) {
            doSyncRequest = this::doUdpSyncRequest;
            channelHandlerFactory.setSendValue(this::doUdpAsyncSend);
            logger.debug("Configured '{}' for UDP connections.", thing.getUID());
        } else if (config.protocol == ClientConfiguration.Protocol.TCP) {
            doSyncRequest = this::doTcpSyncRequest;
            channelHandlerFactory.setSendValue(this::doTcpAsyncSend);
            logger.debug("Configured '{}' for TCP connections.", thing.getUID());
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Protocol for connection not set!");
            return;
        }

        thing.getChannels().forEach(this::createChannel);

        if (channels.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No channels defined.");
            return;
        }

        stopRefresh();
        refreshJob = scheduler.scheduleWithFixedDelay(() -> readCommands.forEach(this::refreshChannel), 0,
                config.refresh, TimeUnit.SECONDS);

        updateStatus(ThingStatus.UNKNOWN);
    }

    private void stopRefresh() {
        ScheduledFuture<?> refreshJob = this.refreshJob;
        if (refreshJob != null) {
            refreshJob.cancel(true);
            this.refreshJob = null;
        }
    }

    public void dispose() {
        stopRefresh();

        channels.clear();
        readCommands.clear();

        // remove state descriptions
        dynamicStateDescriptionProvider.removeDescriptionsForThing(thing.getUID());

        super.dispose();
    }

    private void refreshChannel(ChannelUID channelUID, String stateContent) {
        logger.trace("Refreshing '{}' with command '{}'", channelUID, stateContent);
        ChannelHandler channelHandler = channels.get(channelUID);

        if (channelHandler == null) {
            logger.warn("Failed to refresh '{}': itemValueConverter not found.", channelUID);
            return;
        }

        doSyncRequest.apply(stateContent).ifPresent(channelHandler::process);
    }

    private void createChannel(Channel channel) {
        ChannelUID channelUID = channel.getUID();
        TcpUdpChannelConfig channelConfig = channel.getConfiguration().as(TcpUdpChannelConfig.class);
        String acceptedItemType = channel.getAcceptedItemType();

        channelHandlerFactory.create(channelUID, acceptedItemType, channelConfig).ifPresent(itemValueConverter -> {
            if (channelConfig.mode == ChannelMode.READONLY || channelConfig.mode == ChannelMode.READWRITE) {
                if (channelConfig.stateContent.isEmpty()) {
                    logger.warn(
                            "Empty stateContent configured for channel '{}' with capability 'read'. State updates are disabled.",
                            channelUID);
                } else {
                    if (!readCommands.containsValue(channelConfig.stateContent)) {
                        readCommands.put(channelUID, channelConfig.stateContent);
                    } else {
                        logger.warn(
                                "'{}' is configured as 'stateContent' for more than one channel. Ignoring for channel '{}'.",
                                channelConfig.stateContent, channelUID);
                        return;
                    }
                }
            }

            channels.put(channelUID, itemValueConverter);

            StateDescription stateDescription = StateDescriptionFragmentBuilder.create()
                    .withReadOnly(channelConfig.mode == ChannelMode.READONLY).build().toStateDescription();
            if (stateDescription != null) {
                // if the state description is not available, we don't need to add it
                dynamicStateDescriptionProvider.setDescription(channelUID, stateDescription);
            }
        });
    }

    private String getEncoding() {
        return Objects.requireNonNullElse(config.encoding, StandardCharsets.UTF_8.name());
    }

    protected void doTcpAsyncSend(String command) {
        scheduler.execute(() -> {
            try (Socket socket = new Socket(config.host, config.port); OutputStream out = socket.getOutputStream()) {
                socket.setSoTimeout(config.timeout);
                out.write(command.getBytes(getEncoding()));
                out.flush();

                updateStatus(ThingStatus.ONLINE);
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                logger.debug("Failed sending '{}' in thing '{}': {}", command, thing.getUID(), e.getMessage());
            }
        });
    }

    protected Optional<ChannelHandlerContent> doTcpSyncRequest(String request) {
        try (Socket socket = new Socket(config.host, config.port);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                ByteArrayOutputStream outputByteArrayStream = new ByteArrayOutputStream()) {
            socket.setSoTimeout(config.timeout);
            out.write(request.getBytes(getEncoding()));
            out.flush();
            byte[] buffer = new byte[config.bufferSize];
            int len;

            do {
                len = in.read(buffer);
                if (len != -1) {
                    outputByteArrayStream.write(buffer, 0, len);
                }
                if (len < buffer.length) {
                    Thread.sleep(100);
                }
            } while (in.available() > 0);

            outputByteArrayStream.flush();

            ChannelHandlerContent contentWrapper = new ChannelHandlerContent(outputByteArrayStream.toByteArray(),
                    Objects.requireNonNullElse(config.encoding, StandardCharsets.UTF_8.name()), null);

            updateStatus(ThingStatus.ONLINE);
            return Optional.of(contentWrapper);
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            logger.debug("Failed to request '{}' in thing '{}': {}", request, thing.getUID(), e.getMessage());
        }

        return Optional.empty();
    }

    protected void doUdpAsyncSend(String command) {
        scheduler.execute(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(config.timeout);
                InetAddress inetAddress = InetAddress.getByName(config.host);

                byte[] buffer = command.getBytes(getEncoding());
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, config.port);

                socket.send(packet);
                updateStatus(ThingStatus.ONLINE);
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                logger.debug("Failed sending '{}' in thing '{}': {}", command, thing.getUID(), e.getMessage());
            }
        });
    }

    protected Optional<ChannelHandlerContent> doUdpSyncRequest(String request) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(config.timeout);
            InetAddress inetAddress = InetAddress.getByName(config.host);

            byte[] buffer = request.getBytes(getEncoding());
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, config.port);
            socket.send(packet);

            byte[] receiveBuffer = new byte[config.bufferSize];
            packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(packet);

            ChannelHandlerContent contentWrapper = new ChannelHandlerContent(
                    Arrays.copyOf(packet.getData(), packet.getLength()), getEncoding(), null);

            updateStatus(ThingStatus.ONLINE);
            return Optional.of(contentWrapper);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            logger.debug("Failed to request '{}' in thing '{}': {}", request, thing.getUID(), e.getMessage());
        }

        return Optional.empty();
    }
}
