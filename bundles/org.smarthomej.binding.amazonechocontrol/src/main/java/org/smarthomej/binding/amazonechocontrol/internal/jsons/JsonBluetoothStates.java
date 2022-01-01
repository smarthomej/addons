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
package org.smarthomej.binding.amazonechocontrol.internal.jsons;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonDevices.Device;

/**
 * The {@link JsonBluetoothStates} encapsulate the GSON data of bluetooth state
 *
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class JsonBluetoothStates {

    public @Nullable BluetoothState findStateByDevice(@Nullable Device device) {
        if (device == null) {
            return null;
        }
        @Nullable
        BluetoothState @Nullable [] bluetoothStates = this.bluetoothStates;
        if (bluetoothStates == null) {
            return null;
        }
        for (BluetoothState state : bluetoothStates) {
            if (state != null && Objects.equals(state.deviceSerialNumber, device.serialNumber)) {
                return state;
            }
        }
        return null;
    }

    public BluetoothState @Nullable [] bluetoothStates;

    public static class PairedDevice {
        public @Nullable String address;
        public boolean connected;
        public @Nullable String deviceClass;
        public @Nullable String friendlyName;
        public @Nullable List<String> profiles;
    }

    public static class BluetoothState {
        public @Nullable String deviceSerialNumber;
        public @Nullable String deviceType;
        public @Nullable String friendlyName;
        public boolean gadgetPaired;
        public boolean online;
        public @Nullable List<PairedDevice> pairedDeviceList;

        public List<PairedDevice> getPairedDeviceList() {
            return Objects.requireNonNullElse(pairedDeviceList, List.of());
        }
    }
}
