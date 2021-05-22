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
package org.smarthomej.binding.knx.internal.dpt;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.QuantityType;

/**
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
@NonNullByDefault
public class KNXCoreTypeMapperTest {

    @Test
    public void testToDPTValueTrailingZeroesStrippedOff() {
        assertEquals("3", KNXCoreTypeMapper.formatAsDPTString(new DecimalType("3"), "17.001"));
        assertEquals("3", KNXCoreTypeMapper.formatAsDPTString(new DecimalType("3.0"), "17.001"));
    }

    @Test
    public void testToDPTValueDecimalType() {
        assertEquals("23.1", KNXCoreTypeMapper.formatAsDPTString(new DecimalType("23.1"), "9.001"));
    }

    @Test
    public void testToDPTValueQuantityType() {
        assertEquals("23.1", KNXCoreTypeMapper.formatAsDPTString(new QuantityType<>("23.1 °C"), "9.001"));
    }

    @Test
    public void rgbValue() {
        // input data
        byte[] data = new byte[] { 123, 45, 67 };

        // this is the old implementation
        String value = "r:123 g:45 b:67";
        int r = Integer.parseInt(value.split(" ")[0].split(":")[1]);
        int g = Integer.parseInt(value.split(" ")[1].split(":")[1]);
        int b = Integer.parseInt(value.split(" ")[2].split(":")[1]);
        HSBType expected = HSBType.fromRGB(r, g, b);

        assertEquals(expected, KNXCoreTypeMapper.convertRawDataToType("232.600", data));
    }
}
