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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.local.dto.DeviceInfo;
import org.smarthomej.binding.tuya.internal.local.handlers.DatagramToByteBufDecoder;
import org.smarthomej.binding.tuya.internal.local.handlers.DiscoveryMessageHandler;
import org.smarthomej.binding.tuya.internal.local.handlers.TuyaDecoder;
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
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * The {@link UdpDiscoveryListener} handles UDP device discovery message
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UdpDiscoveryListener implements ChannelFutureListener {
    private static final byte[] TUYA_UDP_KEY = HexUtils.hexToBytes(CryptoUtil.md5("yGAdlopoPVldABfn"));

    private final Logger logger = LoggerFactory.getLogger(UdpDiscoveryListener.class);

    private final Gson gson = new Gson();

    private final Map<String, DeviceInfo> deviceInfos = new HashMap<>();
    private final Map<String, DeviceInfoSubscriber> deviceListeners = new HashMap<>();

    private @NonNullByDefault({}) Channel encryptedChannel;
    private @NonNullByDefault({}) Channel rawChannel;
    private final EventLoopGroup group;
    private boolean deactivate = false;

    public UdpDiscoveryListener(EventLoopGroup group) {
        this.group = group;
        activate();
    }

    private void activate() {
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("udpDecoder", new DatagramToByteBufDecoder());
                            pipeline.addLast("messageDecoder", new TuyaDecoder(gson, "udpListener",
                                    new TuyaDevice.KeyStore(TUYA_UDP_KEY), ProtocolVersion.V3_1));
                            pipeline.addLast("discoveryHandler",
                                    new DiscoveryMessageHandler(deviceInfos, deviceListeners));
                            pipeline.addLast("userEventHandler", new UserEventHandler("udpListener"));
                        }
                    });

            ChannelFuture futureEncrypted = b.bind(6667).addListener(this).sync();
            encryptedChannel = futureEncrypted.channel();

            ChannelFuture futureRaw = b.bind(6666).addListener(this).sync();
            rawChannel = futureRaw.channel();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public void deactivate() {
        deactivate = true;
        encryptedChannel.pipeline().fireUserEventTriggered(new UserEventHandler.DisposeEvent());
        rawChannel.pipeline().fireUserEventTriggered(new UserEventHandler.DisposeEvent());
        try {
            encryptedChannel.closeFuture().sync();
            rawChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public void registerListener(String deviceId, DeviceInfoSubscriber subscriber) {
        if (deviceListeners.put(deviceId, subscriber) != null) {
            logger.warn("Registered a second listener for '{}'.", deviceId);
        }
        DeviceInfo deviceInfo = deviceInfos.get(deviceId);
        if (deviceInfo != null) {
            subscriber.deviceInfoChanged(deviceInfo);
        }
    }

    public void unregisterListener(DeviceInfoSubscriber deviceInfoSubscriber) {
        if (!deviceListeners.entrySet().removeIf(e -> deviceInfoSubscriber.equals(e.getValue()))) {
            logger.warn("Tried to unregister a listener for '{}' but no registration found.", deviceInfoSubscriber);
        }
    }

    @Override
    public void operationComplete(@NonNullByDefault({}) ChannelFuture channelFuture) throws Exception {
        if (!channelFuture.isSuccess() && !deactivate) {
            // if we are not disposing, restart listener after an error
            deactivate();
            activate();
        }
    }
}
