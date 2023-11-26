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

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.smarthomej.binding.amazonechocontrol.internal.dto.response.BluetoothStateTO;

/**
 * The {@link BluetoothPairedDeviceTO} encapsulate a part of {@link BluetoothStateTO}
 *
 * @author Jan N. Klug - Initial contribution
 */
public class BluetoothPairedDeviceTO {
    public String address;
    public boolean connected;
    public String deviceClass;
    public String friendlyName;
    public List<String> profiles = List.of();

    @Override
    public @NonNull String toString() {
        return "BluetoothPairedDeviceTO{address='" + address + "', connected=" + connected + ", deviceClass='"
                + deviceClass + "', friendlyName='" + friendlyName + "', profiles=" + profiles + "}";
    }
}
