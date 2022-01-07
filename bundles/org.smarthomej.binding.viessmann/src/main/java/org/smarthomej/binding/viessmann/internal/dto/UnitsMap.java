/**
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
package org.smarthomej.binding.viessmann.internal.dto;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;

/**
 * This class provides the units for values depending on the viessmann unit
 * 
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class UnitsMap {
    private static final Map<String, String> UNIT_MAP = new HashMap<>();

    private UnitsMap() {
        // prevent instantiation
    }

    /**
     * get unit string for a given viessmann unit
     *
     * @param unit the viessmann unit
     * @return unit string
     */
    public static @Nullable String getUnit(String unit) {
        return UNIT_MAP.get(unit);
    }

    static {
        UNIT_MAP.put("celsius", SIUnits.CELSIUS.getSymbol());
        UNIT_MAP.put("kilowattHour", Units.KILOWATT_HOUR.toString());
        UNIT_MAP.put("percent", Units.PERCENT.toString());
        UNIT_MAP.put("minute", Units.MINUTE.toString());
        UNIT_MAP.put("hour", Units.HOUR.toString());
    }
}
