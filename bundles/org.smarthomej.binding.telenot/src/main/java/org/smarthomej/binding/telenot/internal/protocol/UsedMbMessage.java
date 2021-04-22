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

/**
 * The {@link UsedMbMessage} class represents a parsed contact info message.
 * *
 * 
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class UsedMbMessage extends TelenotMessage {

    /** Address number */
    public final String address;
    public final String name;

    public UsedMbMessage(String message) throws IllegalArgumentException {
        super(message);
        StringBuilder strBuilder = new StringBuilder();

        String parts[] = message.split(":");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Multiple colons found in Used contacts info Message");
        }

        String msg = parts[1];

        String stringLen = msg.substring(12, 14);
        int stateMsgLength = Integer.parseInt(stringLen, 16) * 2;
        // String stateMsg = msg.substring(16, 16 + stateMsgLength);

        stringLen = msg.substring(16 + stateMsgLength, 16 + stateMsgLength + 2);
        int nameMsgLength = Integer.parseInt(stringLen, 16) * 2;
        String contactNameHex = msg.substring(20 + stateMsgLength, 20 + stateMsgLength + nameMsgLength);

        int contactHexLen = contactNameHex.length();
        int b = 0;
        while (b < contactHexLen) {
            Integer charcode = Integer.parseInt(contactNameHex.substring(b, b + 2), 16);
            strBuilder.append(String.valueOf(Character.toChars(charcode)));
            b = b + 2;
        }
        String strcontact = strBuilder.toString();

        try {
            address = parts[0];
            name = strcontact;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Used contacts info message contains invalid number: " + e.getMessage(),
                    e);
        }
    }
}
