/**
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
package org.smarthomej.binding.tuya.internal.local.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TcpPayload} encapsulates the payload of a TCP status message
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TcpPayload<T> {
    public String devId;
    public String gwId;
    public String uid;
    public long t = System.currentTimeMillis() / 1000;
    public T dps;

    public TcpPayload(String deviceId, T dps) {
        this.devId = deviceId;
        this.gwId = deviceId;
        this.uid = deviceId;
        this.dps = dps;
    }
}
