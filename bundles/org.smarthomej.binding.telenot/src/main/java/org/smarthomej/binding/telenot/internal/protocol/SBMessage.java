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
package org.smarthomej.binding.telenot.internal.protocol;

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

    public final boolean disarmed;
    public final boolean internallyArmed;
    public final boolean externallyArmed;
    public final boolean alarm;
    public final boolean malfunction;
    public final boolean readyToArmInternally;
    public final boolean readyToArmExternally;
    public final boolean statusInternalSignalHorn;

    public SBMessage(String message) throws IllegalArgumentException {
        super(message);

        String parts[] = message.split(",");

        if (parts.length != 9) {
            throw new IllegalArgumentException("Invalid number of parts in SB message");
        }

        try {
            address = Integer.parseInt(parts[0]);
            disarmed = parts[1].equals("0") ? true : false;
            internallyArmed = parts[2].equals("0") ? true : false;
            externallyArmed = parts[3].equals("0") ? true : false;
            alarm = parts[4].equals("0") ? true : false;
            malfunction = parts[5].equals("0") ? true : false;
            readyToArmInternally = parts[6].equals("0") ? true : false;
            readyToArmExternally = parts[7].equals("0") ? true : false;
            statusInternalSignalHorn = parts[8].equals("0") ? true : false;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SB message contains invalid number: " + e.getMessage());
        }
    }
}
