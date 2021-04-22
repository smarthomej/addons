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
 * The {@link SBMessage} class represents a parsed SB message.
 * *
 * 
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class SBMessage extends TelenotMessage {

    /** Address number */
    public final int address;

    public final int disarmed;
    public final int internallyArmed;
    public final int externallyArmed;
    public final int alarm;
    public final int malfunction;
    public final int readyToArmInternally;
    public final int readyToArmExternally;
    public final int statusInternalSignalHorn;

    public SBMessage(String message) throws IllegalArgumentException {
        super(message);

        List<String> parts = splitMsg(message);

        if (parts.size() != 9) {
            throw new IllegalArgumentException("Invalid number of parts in SB message");
        }

        try {
            address = Integer.parseInt(parts.get(0));
            disarmed = Integer.parseInt(parts.get(1));
            internallyArmed = Integer.parseInt(parts.get(2));
            externallyArmed = Integer.parseInt(parts.get(3));
            alarm = Integer.parseInt(parts.get(4));
            malfunction = Integer.parseInt(parts.get(5));
            readyToArmInternally = Integer.parseInt(parts.get(6));
            readyToArmExternally = Integer.parseInt(parts.get(7));
            statusInternalSignalHorn = Integer.parseInt(parts.get(8));

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SB message contains invalid number: " + e.getMessage(), e);
        }
    }
}
