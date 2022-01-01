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

import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.TCP_CONNECTION_MAXIMUM_MISSED_HEARTBEATS;
import static org.smarthomej.binding.tuya.internal.TuyaBindingConstants.TCP_CONNECTION_TIMEOUT;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.local.CommandType;
import org.smarthomej.binding.tuya.internal.local.MessageWrapper;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * The {@link HeartbeatHandler} is responsible for sending and receiving heartbeat messages
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class HeartbeatHandler extends ChannelDuplexHandler {
    private final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);
    private final String deviceId;
    private int heartBeatMissed = 0;

    public HeartbeatHandler(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public void userEventTriggered(@NonNullByDefault({}) ChannelHandlerContext ctx, @NonNullByDefault({}) Object evt)
            throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (IdleState.READER_IDLE.equals(e.state())) {
                logger.warn("{}{}: Did not receive a message from for {} seconds. Connection seems to be dead.",
                        deviceId, Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""),
                        TCP_CONNECTION_TIMEOUT);
                ctx.close();
            } else if (IdleState.WRITER_IDLE.equals(e.state())) {
                heartBeatMissed++;
                if (heartBeatMissed > TCP_CONNECTION_MAXIMUM_MISSED_HEARTBEATS) {
                    logger.warn("{}{}: Missed more than {} heartbeat responses. Connection seems to be dead.", deviceId,
                            Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""),
                            TCP_CONNECTION_MAXIMUM_MISSED_HEARTBEATS);
                    ctx.close();
                } else {
                    logger.trace("{}{}: Sending ping", deviceId,
                            Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""));
                    ctx.channel().writeAndFlush(new MessageWrapper<>(CommandType.HEART_BEAT, Map.of("dps", "")));
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(@NonNullByDefault({}) ChannelHandlerContext ctx, @NonNullByDefault({}) Object msg)
            throws Exception {
        if (msg instanceof MessageWrapper<?>) {
            MessageWrapper<?> m = (MessageWrapper<?>) msg;
            if (CommandType.HEART_BEAT.equals(m.commandType)) {
                logger.trace("{}{}: Received pong", deviceId,
                        Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""));
                heartBeatMissed = 0;
                // do not forward HEART_BEAT messages
                ctx.fireChannelReadComplete();
                return;
            }
        }
        // forward to next handler
        ctx.fireChannelRead(msg);
    }
}
