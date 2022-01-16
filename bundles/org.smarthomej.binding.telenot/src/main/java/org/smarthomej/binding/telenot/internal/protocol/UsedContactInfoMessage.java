/**
 * Copyright (c) 2021-2022 Contributors to the SmartHome/J project
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
import org.smarthomej.binding.telenot.internal.TelenotMessageException;

/**
 * The {@link UsedContactInfoMessage} class represents a parsed contact info message.
 * *
 * 
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class UsedContactInfoMessage extends TelenotMessage {

    /** Address number */
    public final String address;
    public final String name;

    public UsedContactInfoMessage(String message) throws TelenotMessageException {
        super(message);
        StringBuilder strBuilder = new StringBuilder();

        String parts[] = message.split(":");

        if (parts.length != 2) {
            throw new TelenotMessageException("Multiple colons found in Used contacts info Message");
        }

        String msg = parts[1];

        String stringLen = msg.substring(12, 14);
        int stateMsgLength = Integer.parseInt(stringLen, 16) * 2;

        stringLen = msg.substring(16 + stateMsgLength, 16 + stateMsgLength + 2);
        int nameMsgLength = Integer.parseInt(stringLen, 16) * 2;
        String contactNameHex = msg.substring(20 + stateMsgLength, 20 + stateMsgLength + nameMsgLength);

        int contactHexLen = contactNameHex.length();
        int b = 0;
        while (b < contactHexLen) {
            String hex = contactNameHex.substring(b, b + 2);
            switch (hex) {
                case "E1":
                    strBuilder.append("ä");
                    break;
                case "EF":
                    strBuilder.append("ö");
                    break;
                case "F5":
                    strBuilder.append("ü");
                    break;
                default:
                    Integer charcode = Integer.parseInt(hex, 16);
                    strBuilder.append(String.valueOf(Character.toChars(charcode)));
                    break;
            }
            b = b + 2;
        }
        String strcontact = strBuilder.toString();

        try {
            address = parts[0];
            name = strcontact;
        } catch (NumberFormatException e) {
            throw new TelenotMessageException("Used contacts info message contains invalid number: " + e.getMessage());
        }
    }
}
