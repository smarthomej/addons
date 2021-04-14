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
package org.smarthomej.binding.onewire.internal.device;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link OwSensorType} defines all known sensor types
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public enum OwSensorType {
    DS1420,
    DS18S20,
    DS18B20,
    DS1822,
    DS1923,
    DS2401,
    DS2405,
    DS2406,
    DS2408,
    DS2409,
    DS2413,
    DS2423,
    DS2431,
    DS2438,
    MS_TC,
    MS_TH,
    MS_TL,
    MS_TH_S,
    MS_TV,
    AMS,
    AMS_S,
    BAE,
    BAE0910,
    BAE0911,
    BMS,
    BMS_S,
    EDS,
    EDS0064,
    EDS0065,
    EDS0066,
    EDS0067,
    EDS0068,
    UNKNOWN;

    public static @Nullable OwSensorType fromString(@Nullable String value) {
        return Arrays.stream(values()).filter(v -> v.name().equals(value)).findAny().orElse(null);
    }
}
