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
package org.smarthomej.binding.tuya.internal.util;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;

/**
 * The {@link ConversionUtil} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ConversionUtil {

    private ConversionUtil() {
        // prevent instantiation
    }

    public static HSBType hexColorDecode(String hexColor) {
        double h = Integer.parseInt(hexColor.substring(0, 4), 16);
        double s = Integer.parseInt(hexColor.substring(4, 8), 16) / 10.0;
        double b = Integer.parseInt(hexColor.substring(8, 12), 16) / 10.0;
        if (h == 360) {
            h = 0;
        }

        return new HSBType(new DecimalType(h), new PercentType(new BigDecimal(s)), new PercentType(new BigDecimal(b)));
    }

    public static String hexColorEncode(HSBType hsb) {
        return String.format("%04x%04x%04x", hsb.getHue().intValue(), (int) (hsb.getSaturation().doubleValue() * 10),
                (int) (hsb.getBrightness().doubleValue() * 10));
    }

    public static PercentType brightnessDecode(double value, double min, double max) {
        if (value <= 0) {
            return PercentType.ZERO;
        } else if (value >= max) {
            return PercentType.HUNDRED;
        } else {
            return new PercentType(new BigDecimal(100.0 * value / (max - min)));
        }
    }

    public static int brightnessEncode(PercentType value, double min, double max) {
        return (int) Math.round(value.doubleValue() * (max - min) / 100.0);
    }

    public static double coerceToRange(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }
}
