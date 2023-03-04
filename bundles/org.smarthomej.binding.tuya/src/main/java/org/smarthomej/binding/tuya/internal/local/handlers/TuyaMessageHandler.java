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
package org.smarthomej.binding.tuya.internal.local.handlers;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.local.CommandType;
import org.smarthomej.binding.tuya.internal.local.DeviceStatusListener;
import org.smarthomej.binding.tuya.internal.local.MessageWrapper;
import org.smarthomej.binding.tuya.internal.local.TuyaDevice;
import org.smarthomej.binding.tuya.internal.local.dto.TcpStatusPayload;
import org.smarthomej.binding.tuya.internal.util.CryptoUtil;

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
    private final TuyaDevice.KeyStore keyStore;

    public TuyaMessageHandler(String deviceId, TuyaDevice.KeyStore keyStore,
            DeviceStatusListener deviceStatusListener) {
        this.deviceId = deviceId;
        this.keyStore = keyStore;
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
    @SuppressWarnings("unchecked")
    public void channelRead(@NonNullByDefault({}) ChannelHandlerContext ctx, @NonNullByDefault({}) Object msg)
            throws Exception {
        if (msg instanceof MessageWrapper<?>) {
            MessageWrapper<?> m = (MessageWrapper<?>) msg;
            if (m.commandType == CommandType.DP_QUERY || m.commandType == CommandType.STATUS) {
                Map<Integer, Object> stateMap = null;
                if (m.content instanceof TcpStatusPayload) {
                    TcpStatusPayload payload = (TcpStatusPayload) Objects.requireNonNull(m.content);
                    stateMap = payload.protocol == 4 ? payload.data.dps : payload.dps;
                }

                if (stateMap != null && !stateMap.isEmpty()) {
                    deviceStatusListener.processDeviceStatus(stateMap);
                }
            } else if (m.commandType == CommandType.DP_QUERY_NOT_SUPPORTED) {
                deviceStatusListener.processDeviceStatus(Map.of());
            } else if (m.commandType == CommandType.SESS_KEY_NEG_RESPONSE) {
                byte[] localKeyHmac = CryptoUtil.hmac(keyStore.getRandom(), keyStore.getDeviceKey());
                byte[] localKeyExpectedHmac = Arrays.copyOfRange((byte[]) m.content, 16, 16 + 32);

                if (!Arrays.equals(localKeyHmac, localKeyExpectedHmac)) {
                    logger.warn(
                            "{}{}: Session key negotiation failed during Hmac validation: calculated {}, expected {}",
                            deviceId, Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""),
                            localKeyHmac != null ? HexUtils.bytesToHex(localKeyHmac) : "<null>",
                            HexUtils.bytesToHex(localKeyExpectedHmac));
                    return;
                }

                byte[] remoteKey = Arrays.copyOf((byte[]) m.content, 16);
                byte[] remoteKeyHmac = CryptoUtil.hmac(remoteKey, keyStore.getDeviceKey());
                MessageWrapper<?> response = new MessageWrapper<>(CommandType.SESS_KEY_NEG_FINISH, remoteKeyHmac);

                ctx.channel().writeAndFlush(response);

                byte[] sessionKey = CryptoUtil.generateSessionKey(keyStore.getRandom(), remoteKey,
                        keyStore.getDeviceKey());
                if (sessionKey == null) {
                    logger.warn("{}{}: Session key negotiation failed because session key is null.", deviceId,
                            Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""));
                    return;
                }
                keyStore.setSessionKey(sessionKey);
            }
        }
    }
}
