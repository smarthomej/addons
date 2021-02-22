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
package org.smarthomej.binding.deconz.internal.netutils;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.binding.deconz.internal.dto.DeconzBaseMessage;

/**
 * Informs about received messages
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface WebSocketMessageListener {
    /**
     * A new message was received
     *
     * @param sensorID The sensor ID (API endpoint)
     * @param message The received message
     */
    void messageReceived(String sensorID, DeconzBaseMessage message);
}
