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
 * The {@link ViessmannCommunicationException} is thrown during GET or POST requests.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ViessmannCommunicationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param message Viessmann error message
     */
    public ViessmannCommunicationException(String message) {
        super(message);
    }
}
