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
package org.smarthomej.binding.tuya.internal.local;

import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.TCP_CONNECTION_HEARTBEAT_INTERVAL;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.TCP_CONNECTION_TIMEOUT;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.local.handlers.HeartbeatHandler;
import org.smarthomej.binding.tuya.internal.local.handlers.TuyaDecoder;
import org.smarthomej.binding.tuya.internal.local.handlers.TuyaEncoder;
import org.smarthomej.binding.tuya.internal.local.handlers.TuyaMessageHandler;
import org.smarthomej.binding.tuya.internal.local.handlers.UserEventHandler;

import com.google.gson.Gson;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * The {@link TuyaDevice} handles the device connection
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TuyaDevice implements ChannelFutureListener {
    private final Logger logger = LoggerFactory.getLogger(TuyaDevice.class);

    private final Bootstrap bootstrap = new Bootstrap();
    private final DeviceStatusListener deviceStatusListener;
    private final String deviceId;

    private final String address;
    private @Nullable Channel channel;

    public TuyaDevice(Gson gson, DeviceStatusListener deviceStatusListener, EventLoopGroup eventLoopGroup,
            String deviceId, byte[] deviceKey, String address, String protocolVersion) {
        this.address = address;
        this.deviceId = deviceId;
        this.deviceStatusListener = deviceStatusListener;
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("idleStateHandler",
                        new IdleStateHandler(TCP_CONNECTION_TIMEOUT, TCP_CONNECTION_HEARTBEAT_INTERVAL, 0));
                pipeline.addLast("messageEncoder", new TuyaEncoder(gson, deviceId, deviceKey, protocolVersion));
                pipeline.addLast("messageDecoder", new TuyaDecoder(gson, deviceId, deviceKey, protocolVersion));
                pipeline.addLast("heartbeatHandler", new HeartbeatHandler(deviceId));
                pipeline.addLast("deviceHandler", new TuyaMessageHandler(deviceId, deviceStatusListener));
                pipeline.addLast("userEventHandler", new UserEventHandler());
            }
        });
        connect();
    }

    public void connect() {
        bootstrap.connect(address, 6668).addListener(this);
    }

    private void disconnect() {
        Channel channel = this.channel;
        if (channel != null) { // if channel == null we are not connected anyway
            channel.pipeline().fireUserEventTriggered(new UserEventHandler.DisposeEvent());
            this.channel = null;
        }
    }

    public void set(Map<Integer, Object> command) {
        MessageWrapper<?> m = new MessageWrapper<>(CommandType.CONTROL, Map.of("dps", command));
        Channel channel = this.channel;
        if (channel != null) {
            channel.writeAndFlush(m);
        } else {
            logger.warn("{}: Setting {} failed. Device is not connected.", deviceId, command);
        }
    }

    public void dispose() {
        disconnect();
    }

    @Override
    public void operationComplete(@NonNullByDefault({}) ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            this.channel = channelFuture.channel();
            MessageWrapper<?> m = new MessageWrapper<>(CommandType.DP_QUERY, Map.of("dps", Map.of()));
            channelFuture.channel().writeAndFlush(m);
        } else {
            logger.debug("{}{}: Failed to connect: {}", deviceId,
                    Objects.requireNonNullElse(channelFuture.channel().remoteAddress(), ""),
                    channelFuture.cause().getMessage());
            this.channel = null;
            deviceStatusListener.connectionStatus(false);
        }
    }
}
