/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.deconz.internal.netutils;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Informs about the websocket connection.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public interface WebSocketConnectionListener {
    /**
     * Connection successfully established.
     */
    void webSocketConnectionEstablished();

    /**
     * Connection lost. A reconnect timer has been started.
     *
     * @param reason A reason for the disconnection
     */
    void webSocketConnectionLost(String reason);
}
