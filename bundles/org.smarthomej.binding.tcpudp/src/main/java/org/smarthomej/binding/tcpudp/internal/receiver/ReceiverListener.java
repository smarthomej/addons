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
package org.smarthomej.binding.tcpudp.internal.receiver;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ReceiverListener} is an interface for TCP and UDP receiver connection listeners
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ReceiverListener {
    /**
     * report the connection state to the thing handler
     * 
     * @param state true if successfully installed, false if failed
     * @param message optional message (only used for failed connections)
     */
    void reportConnectionState(boolean state, @Nullable String message);

    void onReceive(String sender, byte[] content);
}
