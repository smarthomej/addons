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
package org.smarthomej.binding.tuya.internal.local.handlers;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.local.CommandType;
import org.smarthomej.binding.tuya.internal.local.DeviceStatusListener;
import org.smarthomej.binding.tuya.internal.local.MessageWrapper;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * The {@link TuyaMessageHandler} is a Netty channel handler
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TuyaMessageHandler extends ChannelDuplexHandler {
    private final Logger logger = LoggerFactory.getLogger(TuyaMessageHandler.class);

    private final String deviceId;
    private final DeviceStatusListener deviceStatusListener;

    public TuyaMessageHandler(String deviceId, DeviceStatusListener deviceStatusListener) {
        this.deviceId = deviceId;
        this.deviceStatusListener = deviceStatusListener;
    }

    @Override
    public void channelActive(@NonNullByDefault({}) ChannelHandlerContext ctx) throws Exception {
        logger.debug("{}{}: Connection established.", deviceId,
                Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""));
        deviceStatusListener.connectionStatus(true);
    }

    @Override
    public void channelInactive(@NonNullByDefault({}) ChannelHandlerContext ctx) throws Exception {
        logger.debug("{}{}: Connection terminated.", deviceId,
                Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""));
        deviceStatusListener.connectionStatus(false);
    }

    @Override
    public void channelRead(@NonNullByDefault({}) ChannelHandlerContext ctx, @NonNullByDefault({}) Object msg)
            throws Exception {
        if (msg instanceof MessageWrapper<?>) {
            MessageWrapper<?> m = (MessageWrapper<?>) msg;
            if (CommandType.DP_QUERY.equals(m.commandType) || CommandType.STATUS.equals(m.commandType)) {
                @SuppressWarnings("unchecked")
                Map<Integer, Object> stateMap = (Map<Integer, Object>) m.content;
                if (stateMap != null) {
                    deviceStatusListener.processDeviceStatus(stateMap);
                }
            }
        }
    }
}
