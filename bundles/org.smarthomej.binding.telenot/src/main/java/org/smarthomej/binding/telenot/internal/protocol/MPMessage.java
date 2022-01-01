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

/**
 * The {@link MPMessage} class represents a parsed MP message.
 * *
 * 
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class MPMessage extends TelenotMessage {

    /** Address number */
    public final int address;

    /** Message data */
    public final int data;

    public MPMessage(String message) throws IllegalArgumentException {
        super(message);

        String parts[] = message.split(",");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid number of parts in MP message");
        }

        try {
            address = Integer.parseInt(parts[0]);
            data = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("MP message contains invalid number: " + e.getMessage());
        }

        if ((data & ~0x1) != 0) {
            throw new IllegalArgumentException("MP status should only be 0 or 1");
        }
    }
}
