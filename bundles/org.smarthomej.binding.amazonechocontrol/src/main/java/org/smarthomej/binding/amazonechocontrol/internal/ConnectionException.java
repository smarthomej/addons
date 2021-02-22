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
package org.smarthomej.binding.amazonechocontrol.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ConnectionException} is used for errors in the connection to the amazon server
 *
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class ConnectionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConnectionException(String message) {
        super(message);
    }
}
