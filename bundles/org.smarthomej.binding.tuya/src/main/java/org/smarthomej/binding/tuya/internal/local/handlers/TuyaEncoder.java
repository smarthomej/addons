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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.jose4j.base64url.Base64;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tuya.internal.local.CommandType;
import org.smarthomej.binding.tuya.internal.local.MessageWrapper;
import org.smarthomej.binding.tuya.internal.util.CryptoUtil;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * The {@link TuyaEncoder} is a Netty Encoder for encoding Tuya Local messages
 *
 * Parts of this code are inspired by the TuyAPI project (see notice file)
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TuyaEncoder extends MessageToByteEncoder<MessageWrapper<?>> {
    private final Logger logger = LoggerFactory.getLogger(TuyaEncoder.class);

    private final byte[] key;
    private final String version;
    private final String deviceId;
    private final Gson gson;

    private int sequenceNo = 0;

    public TuyaEncoder(Gson gson, String deviceId, byte[] key, String version) {
        this.gson = gson;
        this.deviceId = deviceId;
        this.key = key;
        this.version = version;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encode(@NonNullByDefault({}) ChannelHandlerContext ctx, MessageWrapper<?> msg,
            @NonNullByDefault({}) ByteBuf out) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("devId", deviceId);
        payload.put("gwId", deviceId);
        payload.put("uid", deviceId);
        payload.put("t", System.currentTimeMillis() / 1000);
        Map<String, Object> content = (Map<String, Object>) msg.content;
        if (content != null) {
            payload.putAll(content);
        }

        logger.debug("{}{}: Sending {}, payload {}", deviceId,
                Objects.requireNonNullElse(ctx.channel().remoteAddress(), ""), msg.commandType, payload);

        String json = gson.toJson(payload);
        byte[] payloadBytes = json.getBytes(StandardCharsets.UTF_8);

        if ("3.3".equals(version)) {
            // Always encrypted
            payloadBytes = CryptoUtil.encryptAesEcb(payloadBytes, key);
            if (payloadBytes == null) {
                return;
            }

            if (msg.commandType != CommandType.DP_QUERY && msg.commandType != CommandType.DP_REFRESH) {
                // Add 3.3 header
                ByteBuffer buffer = ByteBuffer.allocate(payloadBytes.length + 15);
                buffer.put("3.3".getBytes(StandardCharsets.UTF_8));
                buffer.position(15);
                buffer.put(payloadBytes);
                payloadBytes = buffer.array();
            }
        } else if (CommandType.CONTROL.equals(msg.commandType)) {
            // Protocol 3.1 and below, only encrypt data if necessary
            byte[] encryptedPayload = CryptoUtil.encryptAesEcb(payloadBytes, key);
            if (encryptedPayload == null) {
                return;
            }
            String payloadStr = Base64.encode(encryptedPayload);
            String hash = CryptoUtil.md5("data=" + payloadStr + "||lpv=" + version + "||" + new String(key));

            // Create byte buffer from hex data
            payloadBytes = (version + hash.substring(8, 24) + payloadStr).getBytes(StandardCharsets.UTF_8);
        }

        // Allocate buffer with room for payload + 24 bytes for
        // prefix, sequence, command, length, crc, and suffix
        ByteBuffer buffer = ByteBuffer.allocate(payloadBytes.length + 24);

        // Add prefix, command, and length
        buffer.putInt(0x000055AA);
        buffer.putInt(++sequenceNo);
        buffer.putInt(msg.commandType.getCode());
        buffer.putInt(payloadBytes.length + 8);
        buffer.put(payloadBytes);

        int calculatedCrc = CryptoUtil.calculateChecksum(buffer.array(), 0, payloadBytes.length + 16);
        buffer.putInt(calculatedCrc);
        buffer.putInt(0x0000AA55);

        if (logger.isTraceEnabled()) {
            logger.trace("{}{}: Sending encoded '{}'", deviceId, ctx.channel().remoteAddress(),
                    HexUtils.bytesToHex(buffer.array()));
        }

        out.writeBytes(buffer.array());
    }
}
