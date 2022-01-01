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

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.local.CommandType;
import org.smarthomej.binding.tuya.internal.local.MessageWrapper;
import org.smarthomej.binding.tuya.internal.local.dto.DiscoveryMessage;
import org.smarthomej.binding.tuya.internal.local.dto.TcpPayload;
import org.smarthomej.binding.tuya.internal.util.CryptoUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * The {@link TuyaDecoder} is a Netty Decoder for encoding Tuya Local messages
 *
 * Parts of this code are inspired by the TuyAPI project (see notice file)
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TuyaDecoder extends ByteToMessageDecoder {
    private static final Type INTEGER_OBJECT_MAP_TYPE = TypeToken
            .getParameterized(Map.class, Integer.class, Object.class).getType();

    private final Logger logger = LoggerFactory.getLogger(TuyaDecoder.class);

    private final byte[] key;
    private final String version;
    private final Gson gson;
    private final String deviceId;

    public TuyaDecoder(Gson gson, String deviceId, byte[] key, String version) {
        this.gson = gson;
        this.key = key;
        this.version = version;
        this.deviceId = deviceId;
    }

    @Override
    protected void decode(@NonNullByDefault({}) ChannelHandlerContext ctx, @NonNullByDefault({}) ByteBuf in,
            @NonNullByDefault({}) List<Object> out) throws Exception {
        if (in.readableBytes() < 24) {
            // minimum packet size is 16 bytes header + 8 bytes suffix
            return;
        }

        // we need to take a copy first so the buffer stays intact if we exit early
        ByteBuf inCopy = in.copy();
        byte[] bytes = new byte[inCopy.readableBytes()];
        inCopy.readBytes(bytes);
        inCopy.release();

        if (logger.isTraceEnabled()) {
            logger.trace("{}{}: Received encoded '{}'", deviceId,
                    Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""), HexUtils.bytesToHex(bytes));
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int prefix = buffer.getInt();
        int sequenceNumber = buffer.getInt();
        CommandType commandType = CommandType.fromCode(buffer.getInt());
        int payloadLength = buffer.getInt();

        //
        if (buffer.limit() < payloadLength + 16) {
            // there are less bytes than needed, exit early
            logger.trace("Did not receive enough bytes from '{}', exiting early", deviceId);
            return;
        } else {
            // we have enough bytes, skip them from the input buffer and proceed processing
            in.skipBytes(payloadLength + 16);
        }

        int returnCode = buffer.getInt();

        byte[] payload;
        if ((returnCode & 0xffffff00) != 0) {
            // rewind if no return code is present
            buffer.position(buffer.position() - 4);
            payload = new byte[payloadLength - 8];
        } else {
            payload = new byte[payloadLength - 8 - 4];
        }

        buffer.get(payload);
        int crc = buffer.getInt();
        // header + payload without suffix and checksum
        int calculatedCrc = CryptoUtil.calculateChecksum(bytes, 0, 16 + payloadLength - 8);
        if (calculatedCrc != crc) {
            logger.warn("Checksum failed for message from '{}': calculated {}, found {}", deviceId, calculatedCrc, crc);
            return;
        }

        int suffix = buffer.getInt();
        if (prefix != 0x000055aa || suffix != 0x0000aa55) {
            logger.warn("Prefix or suffix invalid for message from '{}'.", deviceId);
            return;
        }

        if (Arrays.equals(Arrays.copyOfRange(payload, 0, version.length()), version.getBytes(StandardCharsets.UTF_8))) {
            if ("3.3".equals(version)) {
                // Remove 3.3 header
                payload = Arrays.copyOfRange(payload, 15, payload.length);
            } else {
                payload = Base64.getDecoder().decode(Arrays.copyOfRange(payload, 19, payload.length - 4));
            }
        }

        MessageWrapper<?> m;
        if (CommandType.UDP.equals(commandType)) {
            // UDP is unencrpyted
            m = new MessageWrapper<>(commandType, new String(payload));
        } else {
            String decodedMessage = CryptoUtil.decryptAesEcb(payload, key);
            if (decodedMessage == null) {
                return;
            }
            if (CommandType.STATUS.equals(commandType) || CommandType.DP_QUERY.equals(commandType)) {
                Type type = TypeToken.getParameterized(TcpPayload.class, INTEGER_OBJECT_MAP_TYPE).getType();
                m = new MessageWrapper<>(commandType,
                        Objects.requireNonNull((TcpPayload<?>) gson.fromJson(decodedMessage, type)).dps);
            } else if (CommandType.UDP_NEW.equals(commandType)) {
                m = new MessageWrapper<>(commandType,
                        Objects.requireNonNull(gson.fromJson(decodedMessage, DiscoveryMessage.class)));
            } else {
                m = new MessageWrapper<>(commandType, decodedMessage);
            }
        }

        logger.debug("{}{}: Received {}", deviceId, Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""), m);
        out.add(m);
    }
}
