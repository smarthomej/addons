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
package org.smarthomej.binding.amazonechocontrol.internal.jsons;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.smarthomej.binding.amazonechocontrol.internal.connection.AnnouncementWrapper;

/**
 * The {@link JsonAnnouncementContent} encapsulate the GSON data of the sequence command AlexaAnnouncement for sending
 * announcements
 *
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class JsonAnnouncementContent {
    public String locale = "";
    public Display display;
    public Speak speak;

    public JsonAnnouncementContent(AnnouncementWrapper announcement) {
        display = new Display(announcement.getBodyText(), announcement.getTitle());
        speak = new Speak(announcement.getSpeak());
    }

    public static class Display {
        public String title;
        public String body;

        public Display(String body, @Nullable String title) {
            this.body = body;
            this.title = (title == null || title.isEmpty() ? "openHAB" : title);
        }
    }

    public static class Speak {
        public String type;
        public String value;

        public Speak(String speakText) {
            type = (speakText.startsWith("<speak>") && speakText.endsWith("</speak>")) ? "ssml" : "text";
            value = speakText;
        }
    }
}
