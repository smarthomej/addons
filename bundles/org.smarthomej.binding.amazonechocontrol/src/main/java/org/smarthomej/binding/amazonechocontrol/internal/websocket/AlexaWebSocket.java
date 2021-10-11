/**
 * Copyright (c) 2021 Contributors to the SmartHome/J project
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
package org.smarthomej.binding.amazonechocontrol.internal.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonPushCommand;

import com.google.gson.Gson;

/**
 * The {@link AlexaWebSocket} is a websocket implementation for Alexa connections
 *
 * @author Jan N. Klug - Initial contribution
 */
@WebSocket(maxTextMessageSize = 64 * 1024, maxBinaryMessageSize = 64 * 1024)
@NonNullByDefault
public class AlexaWebSocket {
    private static final long MAX_UNSIGNED_INT32 = 4294967295L;

    private final Logger logger = LoggerFactory.getLogger(AlexaWebSocket.class);

    private final Gson gson;
    private final WebSocketConnection webSocketConnection;
    private final WebSocketCommandHandler webSocketCommandHandler;

    int msgCounter = -1;
    int messageId;
    private @Nullable Session session;

    public AlexaWebSocket(WebSocketConnection webSocketConnection, WebSocketCommandHandler webSocketCommandHandler,
            Gson gson) throws WebsocketException {
        this.webSocketConnection = webSocketConnection;
        this.webSocketCommandHandler = webSocketCommandHandler;
        this.gson = gson;
        this.messageId = ThreadLocalRandom.current().nextInt(0, Short.MAX_VALUE);
    }

    @OnWebSocketConnect
    @SuppressWarnings("unused")
    public void onWebSocketConnect(@Nullable Session session) {
        if (session != null) {
            this.msgCounter = -1;
            this.session = session;
            webSocketConnection.onConnect();
        } else {
            logger.debug("Web Socket connect without session");
        }
    }

    @OnWebSocketMessage
    @SuppressWarnings("unused")
    public void onWebSocketBinary(byte @Nullable [] data, int offset, int len) {
        if (data == null) {
            return;
        }
        logger.debug("received: {}", new String(data));
        this.msgCounter++;

        byte[] buffer = data;
        if (offset > 0 || len != buffer.length) {
            buffer = Arrays.copyOfRange(data, offset, offset + len);
        }
        WebsocketMessage message = new WebsocketMessage(buffer, gson);

        if (this.msgCounter == 0) {
            sendMessage(
                    "0xfe88bc52 0x0000009c {\"protocolName\":\"A:F\",\"parameters\":{\"AlphaProtocolHandler.receiveWindowSize\":\"16\",\"AlphaProtocolHandler.maxFragmentSize\":\"16000\"}}TUNE");
            sendMessage(encodeGWRegister());
        } else {
            try {
                if (message.service.equals("FABE") && message.content.messageType.equals("PON")
                        && message.content.payloadData.length > 0) {
                    logger.debug("Pong received");
                    webSocketConnection.clearPongTimeoutTimer();
                } else {
                    JsonPushCommand pushCommand = message.content.pushCommand;
                    logger.trace("Message received: {}", message.content.payload);
                    if (pushCommand != null) {
                        webSocketCommandHandler.webSocketCommandReceived(pushCommand);
                    }
                }
            } catch (Exception e) {
                logger.debug("Handling of push notification failed", e);
            }
        }
    }

    @OnWebSocketMessage
    @SuppressWarnings("unused")
    public void onWebSocketText(@Nullable String message) {
        logger.trace("Received text message: '{}'", message);
    }

    @OnWebSocketClose
    @SuppressWarnings("unused")
    public void onWebSocketClose(@Nullable Session session, int code, @Nullable String reason) {
        if (session != null) {
            session.close();
        }
        logger.info("Web Socket close {}. Reason: {}", code, reason);
        webSocketConnection.close();
    }

    @OnWebSocketError
    @SuppressWarnings("unused")
    public void onWebSocketError(@Nullable Session session, @Nullable Throwable error) {
        if (session != null) {
            session.close();
        }
        logger.info("Web Socket error: {}", error == null ? "<null>" : error.getMessage());
        webSocketConnection.close();
    }

    private void sendMessage(String message) {
        sendMessage(message.getBytes(StandardCharsets.UTF_8));
    }

    private void sendMessage(byte[] buffer) {
        try {
            logger.debug("Send message with length {}: {}", buffer.length, HexUtils.bytesToHex(buffer, " "));
            Session session = this.session;
            if (session != null) {
                session.getRemote().sendBytes(ByteBuffer.wrap(buffer));
            } else {
                logger.warn("Tried to send message '{}' but session is null. Looks like a bug!", buffer);
            }
        } catch (IOException e) {
            logger.debug("Send message failed", e);
            webSocketConnection.close();
        }
    }

    public void sendPing() {
        logger.debug("Send Ping");
        webSocketConnection.initPongTimeoutTimer();
        sendMessage(encodePing());
    }

    private String encodeNumber(long val, int len) {
        return String.format("0x%0" + len + "x", val);
    }

    private long computeBits(long input) {
        long lenCounter = 32;
        long value = toUnsignedInt(input);
        while (0 != lenCounter && 0 != value) {
            value = value / 2;
            lenCounter--;
        }
        return value;
    }

    private long toUnsignedInt(long value) {
        long result = value;
        if (0 > value) {
            result = value + MAX_UNSIGNED_INT32 + 1;
        }
        return result;
    }

    private int computeChecksum(byte[] data, int exclusionStart, int exclusionEnd) {
        if (exclusionEnd < exclusionStart) {
            return 0;
        }
        long overflow;
        long sum;
        int index;
        for (overflow = 0, sum = 0, index = 0; index < data.length; index++) {
            if (index != exclusionStart) {
                sum += toUnsignedInt((data[index] & 0xFF) << ((index & 3 ^ 3) << 3));
                overflow += computeBits(sum);
                sum = toUnsignedInt((int) sum & (int) MAX_UNSIGNED_INT32);

            } else {
                index = exclusionEnd - 1;
            }
        }
        while (overflow != 0) {
            sum += overflow;
            overflow = computeBits(sum);
            sum = (int) sum & (int) MAX_UNSIGNED_INT32;
        }
        long value = toUnsignedInt(sum);
        return (int) value;
    }

    private byte[] encodeGWRegister() {
        this.messageId++;
        ByteBuffer buffer = ByteBuffer.allocate(0xe4);
        buffer.put("MSG".getBytes(StandardCharsets.UTF_8));
        buffer.putInt((int) (0x00000362 & 0xffffffffL));
        buffer.putInt((int) (messageId & 0xffffffffL));
        buffer.put((byte) 102); // 'f'
        buffer.putInt((int) (0x00000001 & 0xffffffffL));
        buffer.putInt((int) (0x00000000 & 0xffffffffL)); // checksum
        buffer.putInt((int) (0x000000e4 & 0xffffffffL)); // length#
        byte[] msg = "GWM MSG 0x0000b479 0x0000003b urn:tcomm-endpoint:device:deviceType:0:deviceSerialNumber:0 0x00000041 urn:tcomm-endpoint:service:serviceName:DeeWebsiteMessagingService {\"command\":\"REGISTER_CONNECTION\"}FABE"
                .getBytes(StandardCharsets.UTF_8);
        buffer.put(msg);

        int checksum = computeChecksum(buffer.array(), 16, 20);
        buffer.putInt(16, checksum);

        return buffer.array();
    }

    private void encode(byte[] data, long b, int offset, int len) {
        for (int index = 0; index < len; index++) {
            data[index + offset] = (byte) (b >> 8 * (len - 1 - index) & 255);
        }
    }

    private byte[] encodePing() {
        // MSG 0x00000065 0x0e414e47 f 0x00000001 0xbc2fbb5f 0x00000062
        this.messageId++;
        String msg = "MSG 0x00000065 "; // Message-type and Channel = CHANNEL_FOR_HEARTBEAT;
        msg += this.encodeNumber(this.messageId, 8) + " f 0x00000001 ";
        int checkSumStart = msg.length();
        msg += "0x00000000 "; // Checksum!
        int checkSumEnd = msg.length();
        msg += "0x00000062 "; // length content

        byte[] completeBuffer = new byte[0x62];
        byte[] startBuffer = msg.getBytes(StandardCharsets.US_ASCII);

        System.arraycopy(startBuffer, 0, completeBuffer, 0, startBuffer.length);

        byte[] header = "PIN".getBytes(StandardCharsets.US_ASCII);
        byte[] payload = "Regular".getBytes(StandardCharsets.US_ASCII); // g = h.length
        byte[] bufferPing = new byte[header.length + 4 + 8 + 4 + 2 * payload.length];
        int idx = 0;
        System.arraycopy(header, 0, bufferPing, 0, header.length);
        idx += header.length;
        encode(bufferPing, 0, idx, 4);
        idx += 4;
        encode(bufferPing, new Date().getTime(), idx, 8);
        idx += 8;
        encode(bufferPing, payload.length, idx, 4);
        idx += 4;

        for (int q = 0; q < payload.length; q++) {
            bufferPing[idx + q * 2] = (byte) 0;
            bufferPing[idx + q * 2 + 1] = payload[q];
        }
        System.arraycopy(bufferPing, 0, completeBuffer, startBuffer.length, bufferPing.length);

        byte[] buf2End = "FABE".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(buf2End, 0, completeBuffer, startBuffer.length + bufferPing.length, buf2End.length);

        int checksum = this.computeChecksum(completeBuffer, checkSumStart, checkSumEnd);
        String checksumHex = encodeNumber(checksum, 8);
        byte[] checksumBuf = checksumHex.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(checksumBuf, 0, completeBuffer, checkSumStart, checksumBuf.length);
        return completeBuffer;
    }
}
