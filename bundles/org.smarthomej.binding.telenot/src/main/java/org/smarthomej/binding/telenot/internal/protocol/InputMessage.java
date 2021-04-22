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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MPMessage} class represents a parsed MP message.
 * *
 * 
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class InputMessage extends TelenotMessage {

    /** Address number */
    public final String address;

    /** Message data */
    public final int data;

    public InputMessage(String message) throws IllegalArgumentException {
        super(message);

        List<String> parts = splitMsg(message);

        if (parts.size() != 2) {
            throw new IllegalArgumentException("Invalid number of parts in MP message");
        }
        String hexAddr = Integer.toHexString(Integer.parseInt(parts.get(0)));
        hexAddr = String.format("%s" + "%0" + (4 - hexAddr.length()) + "d%s", "0x", 0, hexAddr);
        try {
            address = hexAddr;
            data = Integer.parseInt(parts.get(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("MP message contains invalid number: " + e.getMessage(), e);
        }

        if ((data & ~0x1) != 0) {
            throw new IllegalArgumentException("MP status should only be 0 or 1");
        }
    }
}
