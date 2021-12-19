/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.amazonechocontrol.internal.channelhandler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.smarthomej.binding.amazonechocontrol.internal.ConnectionException;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonDevices.Device;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link ChannelHandlerAnnouncement} is responsible for the announcement
 * channel
 *
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class ChannelHandlerAnnouncement extends ChannelHandler {

    private static final String CHANNEL_NAME = "announcement";

    public ChannelHandlerAnnouncement(AmazonHandlerCallback callback, Gson gson) {
        super(callback, gson);
    }

    @Override
    public boolean tryHandleCommand(Device device, Connection connection, String channelId, Command command)
            throws ConnectionException {
        try {
            if (channelId.equals(CHANNEL_NAME)) {
                if (command instanceof StringType) {
                    String commandValue = command.toFullString();
                    String body = commandValue;
                    String title = null;
                    String speak = commandValue;
                    Integer volume = null;
                    if (commandValue.startsWith("{") && commandValue.endsWith("}")) {
                        try {
                            AnnouncementRequestJson request = parseJson(commandValue, AnnouncementRequestJson.class);
                            if (request != null) {
                                speak = request.speak;
                                if (speak == null || speak.length() == 0) {
                                    speak = "."; // blank generates a beep
                                }
                                volume = request.volume;
                                title = request.title;
                                body = request.body;
                                if (body == null) {
                                    body = speak;
                                }
                                Boolean sound = request.sound;
                                if (sound != null) {
                                    if (!sound && !speak.startsWith("<speak>")) {
                                        speak = "<speak>" + speak + "</speak>";
                                    }
                                    if (sound && speak.startsWith("<speak>")) {
                                        body = "Error: The combination of sound and speak in SSML syntax is not allowed";
                                        title = "Error";
                                        speak = "<speak><lang xml:lang=\"en-UK\">Error: The combination of sound and speak in <prosody rate=\"x-slow\"><say-as interpret-as=\"characters\">SSML</say-as></prosody> syntax is not allowed</lang></speak>";
                                    }
                                }
                                if ("<speak> </speak>".equals(speak)) {
                                    volume = -1; // Do not change volume
                                }
                            }
                        } catch (JsonSyntaxException e) {
                            body = "Invalid Json." + e.getLocalizedMessage();
                            title = "Error";
                            speak = "<speak><lang xml:lang=\"en-US\">" + body + "</lang></speak>";
                            body = e.getLocalizedMessage();
                        }
                    }
                    callback.startAnnouncement(device, speak, Objects.requireNonNullElse(body, ""), title, volume);
                }
                refreshChannel();
            }
        } catch (IOException | URISyntaxException e) {
            throw new ConnectionException(e.getMessage(), e);
        }
        return false;
    }

    private void refreshChannel() {
        callback.updateChannelState(CHANNEL_NAME, new StringType(""));
    }

    private static class AnnouncementRequestJson {
        public @Nullable Boolean sound;
        public @Nullable String title;
        public @Nullable String body;
        public @Nullable String speak;
        public @Nullable Integer volume;
    }
}
