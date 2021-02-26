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

import java.math.BigDecimal;

import org.smarthomej.binding.knx.internal.handler.KNXBridgeBaseThingHandler;

/**
 * {@link KNXBridgeBaseThingHandler} configuration
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public class BridgeConfiguration {
    private int autoReconnectPeriod;
    private BigDecimal readingPause;
    private BigDecimal readRetriesLimit;
    private BigDecimal responseTimeout;

    public int getAutoReconnectPeriod() {
        return autoReconnectPeriod;
    }

    public BigDecimal getReadingPause() {
        return readingPause;
    }

    public BigDecimal getReadRetriesLimit() {
        return readRetriesLimit;
    }

    public BigDecimal getResponseTimeout() {
        return responseTimeout;
    }

    public void setAutoReconnectPeriod(int period) {
        autoReconnectPeriod = period;
    }
}
