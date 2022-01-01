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
package org.smarthomej.binding.amazonechocontrol.internal.websocket;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link WebsocketException} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class WebsocketException extends Exception {
    private static final long serialVersionUID = 1L;

    public WebsocketException(String message) {
        super(message);
    }

    public WebsocketException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
