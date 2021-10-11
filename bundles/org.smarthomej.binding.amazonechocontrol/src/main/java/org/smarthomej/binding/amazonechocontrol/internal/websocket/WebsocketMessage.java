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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonPushCommand;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link WebsocketMessage} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class WebsocketMessage {
    private final Logger logger = LoggerFactory.getLogger(WebsocketMessage.class);

    public final String service;
    public final WebsocketMessageContent content = new WebsocketMessageContent();
    public final String contentTune;
    public final String messageType;
    public final long channel;
    public final long checksum;
    public final long messageId;
    public final String moreFlag;
    public final long seq;

    public WebsocketMessage(byte[] bytes, Gson gson) {
        Buffer buffer = new Buffer(bytes);

        service = new String(bytes, bytes.length - 4, 4, StandardCharsets.UTF_8);

        if ("TUNE".equals(service)) {
            checksum = buffer.getNextInt4();
            int contentLength = (int) buffer.getNextInt4();
            contentTune = buffer.getNextString(contentLength - buffer.getIndex() - 4);
            channel = 0;
            messageId = 0;
            seq = 0;
            messageType = "";
            moreFlag = "";
        } else if ("FABE".equals(service)) {
            contentTune = "";
            messageType = buffer.getNextString(3, false);
            channel = buffer.getNextInt4b();
            messageId = buffer.getNextInt4b();
            moreFlag = buffer.getNextString(1, false);
            seq = buffer.getNextInt4b();
            checksum = buffer.getNextInt4b();
            long contentLength = buffer.getNextInt4b(); // currently unused
            content.messageType = buffer.getNextString(3);

            if (channel == 0x361) { // GW_HANDSHAKE_CHANNEL
                if (content.messageType.equals("ACK")) {
                    int length = (int) buffer.getNextInt4();
                    content.protocolVersion = buffer.getNextString(length);
                    length = (int) buffer.getNextInt4();
                    content.connectionUUID = buffer.getNextString(length);
                    content.established = buffer.getNextInt4();
                    content.timestampINI = buffer.getNextInt8();
                    content.timestampACK = buffer.getNextInt8();
                }
            } else if (channel == 0x362) { // GW_CHANNEL
                if (content.messageType.equals("GWM")) {
                    content.subMessageType = buffer.getNextString(3);
                    content.channel = buffer.getNextInt4();

                    if (content.channel == 0xb479) { // DEE_WEBSITE_MESSAGING
                        int length = (int) buffer.getNextInt4();
                        content.destinationIdentityUrn = buffer.getNextString(length);
                        length = (int) buffer.getNextInt4();
                        String idData = buffer.getNextString(length);

                        String[] idDataElements = idData.split(" ", 2);
                        content.deviceIdentityUrn = idDataElements[0];
                        String payload = null;
                        if (idDataElements.length == 2) {
                            payload = idDataElements[1];
                        }
                        if (payload == null) {
                            int index = buffer.getIndex();
                            payload = new String(bytes, index, bytes.length - 4 - index);
                        }
                        if (!payload.isEmpty()) {
                            try {
                                content.pushCommand = gson.fromJson(payload, JsonPushCommand.class);
                            } catch (JsonSyntaxException e) {
                                logger.info("Parsing json failed, illegal JSON: {}", payload, e);
                            }
                        }
                        content.payload = payload;
                    }
                }
            } else if (channel == 0x65) { // CHANNEL_FOR_HEARTBEAT
                content.payloadData = Arrays.copyOfRange(bytes, buffer.getIndex() - 1, bytes.length - 4);
            }
        } else {
            throw new IllegalArgumentException("Not a valid websocket message");
        }
    }

    static class Buffer {
        private final byte[] buffer;
        private int index = 0;
        private final ByteBuffer byteBuffer;

        public Buffer(byte[] buffer) {
            this.buffer = buffer;
            this.byteBuffer = ByteBuffer.wrap(buffer);
        }

        public String getNextString(int length) {
            return getNextString(length, true);
        }

        public String getNextString(int length, boolean delimiter) {
            if (index + length <= buffer.length) {
                String string = new String(buffer, index, length, StandardCharsets.UTF_8);
                index = index + length + (delimiter ? 1 : 0); // including one delimiter
                return string;
            }
            throw new IllegalStateException("No more bytes left");
        }

        /**
         * get 4-byte integer
         *
         * @return the next 4-byte integer as long
         */
        public long getNextInt4() {
            return getNextInt(8); // each byte has two hex characters
        }

        public long getNextInt4b() {
            index += 4;
            return byteBuffer.getInt(index - 4);
        }

        /**
         * get 8-byte integer
         *
         * @return the next 8-byte integer as long
         */
        public long getNextInt8() {
            return getNextInt(16); // each byte has two hex characters
        }

        public int getIndex() {
            return index;
        }

        private long getNextInt(int length) {
            String str = getNextString(length + 2); // including prefix "0x"
            if (str.startsWith("0x")) {
                return Long.parseLong(str.substring(2), 16);
            }
            throw new NumberFormatException();
        }
    }
}
