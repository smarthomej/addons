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
 * The {@link UsedContactMessage} class represents a parsed UsedContact message.
 * *
 * 
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class UsedContactMessage extends TelenotMessage {

    /** Address number */
    public final String address;

    /** Message data */
    public final int data;

    public UsedContactMessage(String message) throws IllegalArgumentException {
        super(message);

        String parts[] = message.split(",");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid number of parts in UsedContact message");
        }

        try {
            address = parts[0];
            data = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("UsedContact message contains invalid number: " + e.getMessage(), e);
        }
    }
}
