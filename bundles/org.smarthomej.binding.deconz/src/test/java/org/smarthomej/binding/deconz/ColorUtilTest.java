/**
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
package org.smarthomej.binding.deconz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.library.types.HSBType;
import org.smarthomej.binding.deconz.internal.ColorUtil;

/**
 * The {@link ColorUtilTest} is a test class for the color conversion
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ColorUtilTest {
    private static Stream<Arguments> colors() {
        return List.of(HSBType.BLACK, HSBType.BLUE, HSBType.GREEN, HSBType.RED, HSBType.WHITE,
                HSBType.fromRGB(127, 94, 19)).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("colors")
    public void inversionTest(HSBType hsb) {
        HSBType hsb2 = ColorUtil.xyToHsv(ColorUtil.hsbToXY(hsb));

        double deltaHue = Math.abs(hsb.getHue().doubleValue() - hsb2.getHue().doubleValue());
        deltaHue = deltaHue > 180.0 ? Math.abs(deltaHue - 360) : deltaHue; // if deltaHue > 180, the "other direction"
                                                                           // is shorter
        double deltaSat = Math.abs(hsb.getSaturation().doubleValue() - hsb2.getSaturation().doubleValue());
        double deltaBri = Math.abs(hsb.getBrightness().doubleValue() - hsb2.getBrightness().doubleValue());

        assertTrue(deltaHue < 5.0);
        assertTrue(deltaSat <= 1.0);
        assertTrue(deltaBri <= 1.0);
    }
}
