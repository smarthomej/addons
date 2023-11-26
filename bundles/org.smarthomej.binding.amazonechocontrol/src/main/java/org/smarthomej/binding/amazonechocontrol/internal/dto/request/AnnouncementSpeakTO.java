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
package org.smarthomej.binding.amazonechocontrol.internal.dto.request;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The {@link AnnouncementSpeakTO} encapsulates the speak part of an announcement
 *
 * @author Jan N. Klug - Initial contribution
 */
public class AnnouncementSpeakTO {
    public String type;
    public String value;

    @Override
    public @NonNull String toString() {
        return "AnnouncementSpeakTO{type='" + type + "', value='" + value + "'}";
    }
}
