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
package org.smarthomej.binding.tuya.internal.local;

import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.TCP_CONNECTION_HEARTBEAT_INTERVAL;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.TCP_CONNECTION_TIMEOUT;
import static org.smarthomej.binding.tuya.internal.local.CommandType.CONTROL;
import static org.smarthomej.binding.tuya.internal.local.CommandType.CONTROL_NEW;
import static org.smarthomej.binding.tuya.internal.local.CommandType.DP_QUERY;
import static org.smarthomej.binding.tuya.internal.local.CommandType.DP_REFRESH;
import static org.smarthomej.binding.tuya.internal.local.CommandType.SESS_KEY_NEG_START;
import static org.smarthomej.binding.tuya.internal.local.ProtocolVersion.V3_4;

import java.util.List;
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
import org.smarthomej.binding.tuya.internal.util.CryptoUtil;

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
    private final ProtocolVersion protocolVersion;
    private final KeyStore keyStore;
    private @Nullable Channel channel;

    public TuyaDevice(Gson gson, DeviceStatusListener deviceStatusListener, EventLoopGroup eventLoopGroup,
            String deviceId, byte[] deviceKey, String address, String protocolVersion) {
        this.address = address;
        this.deviceId = deviceId;
        this.keyStore = new KeyStore(deviceKey);
        this.deviceStatusListener = deviceStatusListener;
        this.protocolVersion = ProtocolVersion.fromString(protocolVersion);
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("idleStateHandler",
                        new IdleStateHandler(TCP_CONNECTION_TIMEOUT, TCP_CONNECTION_HEARTBEAT_INTERVAL, 0));
                pipeline.addLast("messageEncoder",
                        new TuyaEncoder(gson, deviceId, keyStore, TuyaDevice.this.protocolVersion));
                pipeline.addLast("messageDecoder",
                        new TuyaDecoder(gson, deviceId, keyStore, TuyaDevice.this.protocolVersion));
                pipeline.addLast("heartbeatHandler", new HeartbeatHandler(deviceId));
                pipeline.addLast("deviceHandler", new TuyaMessageHandler(deviceId, keyStore, deviceStatusListener));
                pipeline.addLast("userEventHandler", new UserEventHandler(deviceId));
            }
        });
        connect();
    }

    public void connect() {
        keyStore.reset(); // reset session key
        bootstrap.connect(address, 6668).addListener(this);
    }

    private void disconnect() {
        Channel channel = this.channel;
        if (channel != null) { // if channel == null we are not connected anyway
            channel.pipeline().fireUserEventTriggered(new UserEventHandler.DisposeEvent());
            this.channel = null;
        }
    }

    public void set(Map<Integer, @Nullable Object> command) {
        CommandType commandType = (protocolVersion == V3_4) ? CONTROL_NEW : CONTROL;
        MessageWrapper<?> m = new MessageWrapper<>(commandType, Map.of("dps", command));
        Channel channel = this.channel;
        if (channel != null) {
            channel.writeAndFlush(m);
        } else {
            logger.warn("{}: Setting {} failed. Device is not connected.", deviceId, command);
        }
    }

    public void requestStatus() {
        MessageWrapper<?> m = new MessageWrapper<>(DP_QUERY, Map.of("dps", Map.of()));
        Channel channel = this.channel;
        if (channel != null) {
            channel.writeAndFlush(m);
        } else {
            logger.warn("{}: Querying status failed. Device is not connected.", deviceId);
        }
    }

    public void refreshStatus() {
        MessageWrapper<?> m = new MessageWrapper<>(DP_REFRESH, Map.of("dpId", List.of(4, 5, 6, 18, 19, 20)));
        Channel channel = this.channel;
        if (channel != null) {
            channel.writeAndFlush(m);
        } else {
            logger.warn("{}: Refreshing status failed. Device is not connected.", deviceId);
        }
    }

    public void dispose() {
        disconnect();
    }

    @Override
    public void operationComplete(@NonNullByDefault({}) ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            Channel channel = channelFuture.channel();
            this.channel = channel;
            if (protocolVersion == V3_4) {
                // handshake for session key required
                MessageWrapper<?> m = new MessageWrapper<>(SESS_KEY_NEG_START, keyStore.getRandom());
                channel.writeAndFlush(m);
            } else {
                // no handshake for 3.1/3.3
                requestStatus();
            }
        } else {
            logger.debug("{}{}: Failed to connect: {}", deviceId,
                    Objects.requireNonNullElse(channelFuture.channel().remoteAddress(), ""),
                    channelFuture.cause().getMessage());
            this.channel = null;
            deviceStatusListener.connectionStatus(false);
        }
    }

    public static class KeyStore {
        private final byte[] deviceKey;
        private byte[] sessionKey;
        private byte[] random;

        public KeyStore(byte[] deviceKey) {
            this.deviceKey = deviceKey;
            this.sessionKey = deviceKey;
            this.random = CryptoUtil.generateRandom(16).clone();
        }

        public void reset() {
            this.sessionKey = this.deviceKey;
            this.random = CryptoUtil.generateRandom(16).clone();
        }

        public byte[] getDeviceKey() {
            return sessionKey;
        }

        public byte[] getSessionKey() {
            return sessionKey;
        }

        public void setSessionKey(byte[] sessionKey) {
            this.sessionKey = sessionKey;
        }

        public byte[] getRandom() {
            return random;
        }
    }
}
