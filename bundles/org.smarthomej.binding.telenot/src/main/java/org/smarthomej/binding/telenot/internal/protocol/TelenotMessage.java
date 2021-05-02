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
 * Superclass for all Telenot protocol message types.
 * 
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public abstract class TelenotMessage {

    /** string containing the original unparsed message */
    public final String message;

    public TelenotMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
