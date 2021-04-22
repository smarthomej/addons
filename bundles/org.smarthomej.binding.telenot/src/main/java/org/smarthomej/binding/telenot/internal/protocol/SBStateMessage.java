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
package org.smarthomej.binding.telenot.internal.protocol;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DateTimeType;

/**
 * The {@link SBStateMessage} class represents a parsed SB message.
 * *
 * 
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class SBStateMessage extends TelenotMessage {

    /** Address number */
    public final int address;
    public final String messagetype;
    public final DateTimeType date;
    public final String contact;
    public final boolean alarmSetClear;

    public SBStateMessage(String message) throws IllegalArgumentException {
        super(message);
        StringBuilder strBuilder = new StringBuilder();

        String parts[] = message.split(":");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Multiple colons found in emaState Message");
        }

        String msg = parts[1];
        int ad = 1;

        switch (parts[0]) {
            case "SYS_EXT_ARMED":
            case "SYS_INT_ARMED":
            case "SYS_DISARMED":
                Integer a = Integer.parseInt(msg.substring(18, 22), 16);
                double b = a - 1327;
                double d = Math.ceil(b / 8);
                ad = (int) d;
                break;
            case "ALARM":
                ad = Integer.parseInt(msg.substring(16, 18), 16);
                break;
        }

        int year = Integer.parseInt(msg.substring(30, 32), 16);
        strBuilder.append("20");
        strBuilder.append(String.valueOf(year));
        strBuilder.append("-");

        int month = Integer.parseInt(msg.substring(34, 36), 16);
        if (month < 10) {
            strBuilder.append("0");
        }
        strBuilder.append(String.valueOf(month));
        strBuilder.append("-");

        int day = Integer.parseInt(msg.substring(36, 38), 16);
        if (day < 10) {
            strBuilder.append("0");
        }
        strBuilder.append(String.valueOf(day));
        strBuilder.append("T");

        int hour = Integer.parseInt(msg.substring(38, 40), 16);
        if (hour < 10) {
            strBuilder.append("0");
        }
        strBuilder.append(String.valueOf(hour));
        strBuilder.append(":");

        int min = Integer.parseInt(msg.substring(40, 42), 16);
        if (min < 10) {
            strBuilder.append("0");
        }
        strBuilder.append(String.valueOf(min));
        strBuilder.append(":");

        int sec = Integer.parseInt(msg.substring(42, 44), 16);
        if (sec < 10) {
            strBuilder.append("0");
        }
        strBuilder.append(String.valueOf(sec));
        String strDate = strBuilder.toString();

        strBuilder.setLength(0);

        String contacthex = msg.substring(48, 82);
        int contactHexLen = contacthex.length();
        int b = 0;
        while (b < contactHexLen) {
            Integer charcode = Integer.parseInt(contacthex.substring(b, b + 2), 16);
            strBuilder.append(String.valueOf(Character.toChars(charcode)));
            b = b + 2;
        }
        String strcontact = strBuilder.toString();

        boolean bool = false;
        if (Integer.parseInt(msg.substring(24, 26), 16) == 34) {
            bool = true;
        }

        try {
            address = ad;
            messagetype = parts[0];
            date = DateTimeType.valueOf(strDate);
            contact = strcontact;
            alarmSetClear = bool;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(" SBState message contains invalid number: " + e.getMessage(), e);
        }
    }
}
