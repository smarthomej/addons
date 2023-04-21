/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.knx.internal.dpt;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.QuantityType;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 *
 */
@NonNullByDefault
public class DPTTest {

    @Test
    public void testToDPTValueTrailingZeroesStrippedOff() {
        assertEquals("3", ValueEncoder.encode(new DecimalType("3"), "17.001"));
        assertEquals("3", ValueEncoder.encode(new DecimalType("3.0"), "17.001"));
    }

    @Test
    public void testToDPTValueDecimalType() {
        assertEquals("23.1", ValueEncoder.encode(new DecimalType("23.1"), "9.001"));
    }

    @Test
    public void testToDPTValueQuantityType() {
        assertEquals("23.1", ValueEncoder.encode(new QuantityType<>("23.1 °C"), "9.001"));
    }

    @Test
    public void dpt232RgbValue() {
        // input data
        byte[] data = new byte[] { 123, 45, 67 };

        // this is the old implementation
        String value = "r:123 g:45 b:67";
        int r = Integer.parseInt(value.split(" ")[0].split(":")[1]);
        int g = Integer.parseInt(value.split(" ")[1].split(":")[1]);
        int b = Integer.parseInt(value.split(" ")[2].split(":")[1]);
        HSBType expected = HSBType.fromRGB(r, g, b);

        assertEquals(expected, ValueDecoder.decode("232.600", data, HSBType.class));
    }

    @Test
    public void dpt232HsbValue() {
        // input data
        byte[] data = new byte[] { 123, 45, 67 };

        HSBType hsbType = (HSBType) ValueDecoder.decode("232.60000", data, HSBType.class);

        Assertions.assertNotNull(hsbType);
        assertEquals(173.6, hsbType.getHue().doubleValue(), 0.1);
        assertEquals(17.6, hsbType.getSaturation().doubleValue(), 0.1);
        assertEquals(26.3, hsbType.getBrightness().doubleValue(), 0.1);
    }

    @Test
    public void dpt252EncoderTest() {
        // input data
        byte[] data = new byte[] { 0x26, 0x2b, 0x31, 0x00, 0x00, 0x0e };
        HSBType hsbType = (HSBType) ValueDecoder.decode("251.600", data, HSBType.class);

        assertNotNull(hsbType);
        assertEquals(207, hsbType.getHue().doubleValue(), 0.1);
        assertEquals(23, hsbType.getSaturation().doubleValue(), 0.1);
        assertEquals(19, hsbType.getBrightness().doubleValue(), 0.1);
    }

    @SuppressWarnings("unused")
    private static Stream<String> unitProvider() {
        return DPTUnits.getAllUnitStrings();
    }

    @ParameterizedTest
    @MethodSource("unitProvider")
    public void unitsValid(String unit) {
        String valueStr = "1 " + unit;
        QuantityType<?> value = new QuantityType<>(valueStr);
        Assertions.assertNotNull(value);
    }

    private static Stream<String> rgbValueProvider() {
        return Stream.of("r:0 g:0 b:0", "r:255 g:255 b:255");
    }

    @ParameterizedTest
    @MethodSource("rgbValueProvider")
    public void rgbTest(String value) {
        Assertions.assertNotNull(ValueDecoder.decode("232.600", value.getBytes(StandardCharsets.UTF_8), HSBType.class));
    }
}
