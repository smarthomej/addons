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
package org.smarthomej.binding.androiddebugbridge.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link AndroidDebugBridgeDiscoveryService} discover Android ADB Instances in the network.
 *
 * @author Miguel Alvarez - Initial contribution
 */
@NonNullByDefault
public class AndroidDebugBridgeDeviceReadException extends Exception {
    private static final long serialVersionUID = 6608406239134276287L;

    public AndroidDebugBridgeDeviceReadException(String channelId, String result, String fallback) {
        super("Device does not support " + channelId + ": " + result + " | " + fallback);
    }

    public AndroidDebugBridgeDeviceReadException(String channelId, String result) {
        super("Device does not support " + channelId + ": " + result);
    }

    public AndroidDebugBridgeDeviceReadException(String message) {
        super(message);
    }
}
