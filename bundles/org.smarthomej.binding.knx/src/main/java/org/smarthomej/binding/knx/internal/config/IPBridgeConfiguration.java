/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.knx.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * IP Bridge handler configuration object.
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
@NonNullByDefault
public class IPBridgeConfiguration extends BridgeConfiguration {

    private boolean useNAT = false;
    private @Nullable String type;
    private @Nullable String ipAddress;
    private int portNumber = 3671;
    private @Nullable String localIp;
    private String localSourceAddr = "0.0.0";

    public Boolean getUseNAT() {
        return useNAT;
    }

    public @Nullable String getType() {
        return type;
    }

    public @Nullable String getIpAddress() {
        return ipAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public @Nullable String getLocalIp() {
        return localIp;
    }

    public String getLocalSourceAddr() {
        return localSourceAddr;
    }
}
