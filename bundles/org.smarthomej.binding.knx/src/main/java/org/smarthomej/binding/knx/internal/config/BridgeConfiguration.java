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

/**
 * {@link org.smarthomej.binding.knx.internal.handler.KNXBridgeBaseThingHandler} configuration
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
@NonNullByDefault
public class BridgeConfiguration {
    private int autoReconnectPeriod;
    private int readingPause = 50;
    private int readRetriesLimit = 3;
    private int responseTimeout = 10;

    public int getAutoReconnectPeriod() {
        return autoReconnectPeriod;
    }

    public int getReadingPause() {
        return readingPause;
    }

    public int getReadRetriesLimit() {
        return readRetriesLimit;
    }

    public int getResponseTimeout() {
        return responseTimeout;
    }

    public void setAutoReconnectPeriod(int period) {
        autoReconnectPeriod = period;
    }
}
