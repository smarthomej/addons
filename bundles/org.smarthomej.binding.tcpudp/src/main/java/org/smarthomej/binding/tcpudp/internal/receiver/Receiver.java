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
 * The {@link Receiver} is an interface for TCP and UDP Receivers
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface Receiver extends Runnable {
    /**
     * stops this receiver by closing the socket and preventing re-connection
     */
    void stop();

    /**
     * The {@link ReceiverListener} is an interface for TCP and UDP receiver listeners for reporting connection state
     * and received data
     *
     */
    interface ReceiverListener {
        /**
         * report the connection state to the thing handler
         *
         * @param state true if successfully installed, false if failed
         * @param message optional message (only used for failed connections)
         */
        void reportConnectionState(boolean state, @Nullable String message);

        /**
         * report a received value the the listener
         *
         * @param sender String containing the IP address and port of the client that send the data
         * @param content a byte array with the received data
         */
        void onReceive(String sender, byte[] content);
    }
}
