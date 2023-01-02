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
package org.smarthomej.binding.amazonechocontrol.internal.connection;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonDevices;

/**
 * The {@link AnnouncementWrapper} is a wrapper for announcement instructions
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AnnouncementWrapper {
    private final List<JsonDevices.Device> devices = new ArrayList<>();
    private final List<@Nullable Integer> ttsVolumes = new ArrayList<>();
    private final List<@Nullable Integer> standardVolumes = new ArrayList<>();

    private final String speak;
    private final String bodyText;
    private final @Nullable String title;

    public AnnouncementWrapper(String speak, String bodyText, @Nullable String title) {
        this.speak = speak;
        this.bodyText = bodyText;
        this.title = title;
    }

    public void add(JsonDevices.Device device, @Nullable Integer ttsVolume, @Nullable Integer standardVolume) {
        devices.add(device);
        ttsVolumes.add(ttsVolume);
        standardVolumes.add(standardVolume);
    }

    public List<JsonDevices.Device> getDevices() {
        return devices;
    }

    public String getSpeak() {
        return speak;
    }

    public String getBodyText() {
        return bodyText;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public List<@Nullable Integer> getTtsVolumes() {
        return ttsVolumes;
    }

    public List<@Nullable Integer> getStandardVolumes() {
        return standardVolumes;
    }
}
