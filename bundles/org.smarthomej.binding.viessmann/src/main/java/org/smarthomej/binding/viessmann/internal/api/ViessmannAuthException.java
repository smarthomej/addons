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
package org.smarthomej.binding.viessmann.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ViessmannAuthException} is thrown during the authorization process.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class ViessmannAuthException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param message Viessmann error message
     */
    public ViessmannAuthException(String message) {
        super(message);
    }
}
