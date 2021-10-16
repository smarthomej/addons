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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link JsonNotificationRequest} encapsulate the GSON data for a notification request
 *
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class JsonNotificationRequest {
    public String type; // "Reminder", "Alarm"
    public String status = "ON";
    public long alarmTime;
    public String originalTime;
    public String originalDate;
    public @Nullable String timeZoneId;
    public @Nullable String reminderIndex;
    public @Nullable JsonNotificationSound sound;
    public @Nullable String deviceSerialNumber;
    public @Nullable String deviceType;
    public @Nullable String recurringPattern;
    public @Nullable String reminderLabel;
    public boolean isSaveInFlight = true;
    public String id;
    public boolean isRecurring = false;
    public long createdDate;

    public JsonNotificationRequest(String type, JsonDevices.Device device, @Nullable String label,
            @Nullable JsonNotificationSound sound) {
        // set times
        this.createdDate = System.currentTimeMillis();
        // add 5 seconds, because amazon does not accept calls for times in the past (compared with the server time)
        this.alarmTime = createdDate + 5000;
        Date alarm = new Date(alarmTime);

        this.originalDate = new SimpleDateFormat("yyyy-MM-dd").format(alarm);
        this.originalTime = new SimpleDateFormat("HH:mm:ss.SSSS").format(alarm);

        // fill fields
        this.type = type;
        this.id = "create" + type;

        this.deviceSerialNumber = device.serialNumber;
        this.deviceType = device.deviceType;

        this.reminderLabel = label;
        this.sound = sound;
    }
}
