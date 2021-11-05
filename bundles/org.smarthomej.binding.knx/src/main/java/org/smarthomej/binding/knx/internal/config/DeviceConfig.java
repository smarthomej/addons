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
package org.smarthomej.binding.knx.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Configuration object for the device thing handler.
 *
 * @author Karel Goderis - Initial contribution
 * @author Simon Kaufmann - refactoring & cleanup
 */
@NonNullByDefault
public class DeviceConfig {
    private @Nullable String address;
    private boolean fetch = false;
    private int pingInterval = 600;
    private int readInterval = 0;

    public @Nullable String getAddress() {
        return address;
    }

    public boolean getFetch() {
        return fetch;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public int getReadInterval() {
        return readInterval;
    }
}
