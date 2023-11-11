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
package org.smarthomej.binding.amazonechocontrol.internal.dto;

import org.eclipse.jdt.annotation.NonNull;
import org.smarthomej.binding.amazonechocontrol.internal.util.SerializeNull;

/**
 * The {@link DoNotDisturbDeviceStatusTO} encapsulates a command to enable/disable ascending alarms on a device
 * 
 * @author Jan N. Klug - Initial contribution
 */
public class DoNotDisturbDeviceStatusTO {
    public boolean enabled;
    public String deviceSerialNumber;
    public String deviceType;
    @SerializeNull
    public Object deviceAccountId = null;

    @Override
    public @NonNull String toString() {
        return "DoNotDisturbDeviceStatusTO{enabled=" + enabled + ", deviceSerialNumber='" + deviceSerialNumber
                + "', deviceType='" + deviceType + "', deviceAccountId=" + deviceAccountId + "}";
    }
}
