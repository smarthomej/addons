/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.amazonechocontrol.internal.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
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
    private final Logger logger = LoggerFactory.getLogger(AlexaWebSocket.class);

    private final Gson gson;
    private final WebSocketConnection webSocketConnection;
    private final WebSocketCommandHandler webSocketCommandHandler;

    boolean initialize = false;
    int messageId;
    private @Nullable Session session;

    public AlexaWebSocket(WebSocketConnection webSocketConnection, WebSocketCommandHandler webSocketCommandHandler,
            Gson gson) {
        this.webSocketConnection = webSocketConnection;
        this.webSocketCommandHandler = webSocketCommandHandler;
        this.gson = gson;
        this.messageId = ThreadLocalRandom.current().nextInt(0, Short.MAX_VALUE);
    }

    @OnWebSocketConnect
    @SuppressWarnings("unused")
    public void onWebSocketConnect(@Nullable Session session) {
        if (session != null) {
            this.initialize = true;
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
        if (logger.isDebugEnabled()) {
            logger.debug("received {} bytes: {}", data.length, printableString(data));
        }

        byte[] buffer = data;
        if (offset > 0 || len != buffer.length) {
            buffer = Arrays.copyOfRange(data, offset, offset + len);
        }
        WebsocketMessage message = new WebsocketMessage(buffer, gson);
        try {
            if (initialize) {
                initialize = false;
                sendMessage(
                        "0xfe88bc52 0x0000009c {\"protocolName\":\"A:F\",\"parameters\":{\"AlphaProtocolHandler.receiveWindowSize\":\"16\",\"AlphaProtocolHandler.maxFragmentSize\":\"16000\"}}TUNE"
                                .getBytes(StandardCharsets.UTF_8));
                Thread.sleep(40);
                sendMessage(encodeGWRegister());
                Thread.sleep(40);
                sendPing();
            } else {
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
            }
        } catch (Exception e) {
            logger.debug("Handling of push notification failed", e);
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

    private String printableString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (b < 32) {
                sb.append("\\x").append(String.format("%02x", b));
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    private void sendMessage(byte[] buffer) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Send message with length {}: {}", buffer.length, printableString(buffer));
            }
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

    /**
     * compute checksum: sum of unsigned int 32 + number of overflows
     *
     * @param data byte array of input data
     * @return the calculated checksum
     */
    private int computeChecksum(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length + (4 - data.length % 4));
        buffer.put(data).position(0);

        long sum = 0;
        while (buffer.hasRemaining()) {
            sum += buffer.getInt() & 0xffffffffL;
        }

        long overflow = sum >>> 32; // get overflow count
        sum = sum & 0xffffffffL; // coerce to unsigned int32

        while (overflow != 0) {
            sum += overflow;
            overflow = sum >>> 32;
            sum = sum & 0xffffffffL;
        }

        return (int) sum;
    }

    private ByteBuffer getMessageHeader(int packetLength, int messageType) {
        this.messageId++;

        ByteBuffer buffer = ByteBuffer.allocate(packetLength);
        buffer.put("MSG".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(messageType); // Message-type and Channel
        buffer.putInt(this.messageId);
        buffer.put((byte) 102); // 'f'
        buffer.putInt(0x00000001);
        buffer.putInt(0x00000000); // place for checksum
        buffer.putInt(packetLength); // packet length

        return buffer;
    }

    private byte[] encodeGWRegister() {
        ByteBuffer buffer = getMessageHeader(0x000000e4, 0x00000362);

        byte[] msg = "GWM MSG 0x0000b479 0x0000003b urn:tcomm-endpoint:device:deviceType:0:deviceSerialNumber:0 0x00000041 urn:tcomm-endpoint:service:serviceName:DeeWebsiteMessagingService {\"command\":\"REGISTER_CONNECTION\"}FABE"
                .getBytes(StandardCharsets.UTF_8);
        buffer.put(msg);

        int checksum = computeChecksum(buffer.array());
        buffer.putInt(16, checksum);

        return buffer.array();
    }

    private byte[] encodePing() {
        ByteBuffer buffer = getMessageHeader(0x0000003d, 0x00000065);

        buffer.put("PIN".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(0x00000000);
        buffer.putLong(System.currentTimeMillis());

        // 7-byte payload "Regular" as UTF-16BE
        buffer.putInt(0x00000007);
        buffer.put("Regular".getBytes(StandardCharsets.UTF_16BE));

        buffer.put("FABE".getBytes(StandardCharsets.UTF_8));

        int checksum = computeChecksum(buffer.array());
        buffer.putInt(16, checksum);

        return buffer.array();
    }
}
