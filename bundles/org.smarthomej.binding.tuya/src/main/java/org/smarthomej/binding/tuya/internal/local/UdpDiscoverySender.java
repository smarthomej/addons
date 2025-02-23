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

import static org.smarthomej.binding.tuya.internal.local.CommandType.REQ_DEVINFO;

import java.util.Map;

import io.netty.channel.*;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.local.handlers.TuyaEncoder;
import org.smarthomej.binding.tuya.internal.local.handlers.UdpBroadcastHandler;
import org.smarthomej.binding.tuya.internal.util.CryptoUtil;

import com.google.gson.Gson;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.smarthomej.binding.tuya.internal.util.NetworkUtil;

/**
 * The {@link UdpDiscoverySender} sends device v3.5 discovery UDP broadcast message
 *
 * @author Andriy Yemets - Initial contribution
 */
@NonNullByDefault
public class UdpDiscoverySender {
    private static final byte[] TUYA_UDP_KEY = HexUtils.hexToBytes(CryptoUtil.md5("yGAdlopoPVldABfn"));

    private final Logger logger = LoggerFactory.getLogger(UdpDiscoverySender.class);

    private final Gson gson = new Gson();
    private final NioEventLoopGroup group;

    private final String broadcastAddress = "255.255.255.255";
    private final int broadcastPort = 7000;

    public UdpDiscoverySender() {
        this.group = new NioEventLoopGroup();
    }

    public void sendMessage() {
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("broadcastHandler", new UdpBroadcastHandler(broadcastAddress, broadcastPort));
                            pipeline.addLast("messageEncoder", new TuyaEncoder(gson));
                        }
                    });

            ChannelFuture futureChannel = b.bind(0).sync();
            Channel broadcastChannel = futureChannel.channel();
            broadcastChannel.attr(TuyaDevice.DEVICE_ID_ATTR).set("udpDiscoverySender");
            broadcastChannel.attr(TuyaDevice.PROTOCOL_ATTR).set(ProtocolVersion.V3_5);
            broadcastChannel.attr(TuyaDevice.SESSION_KEY_ATTR).set(TUYA_UDP_KEY);

            MessageWrapper<?> m = new MessageWrapper<>(REQ_DEVINFO, Map.of("from", "app", "ip", NetworkUtil.getLocalIPAddress()));
            broadcastChannel.writeAndFlush(m).sync();
            broadcastChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            logger.warn("UDP Discovery sender interrupted. {}", e.getMessage());
        } finally {
            group.shutdownGracefully();
        }
    }
}
