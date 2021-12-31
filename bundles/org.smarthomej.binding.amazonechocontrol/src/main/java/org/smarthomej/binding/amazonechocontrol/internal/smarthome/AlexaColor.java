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
package org.smarthomej.binding.amazonechocontrol.internal.smarthome;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.HSBType;

/**
 * The {@link AlexaColor} defines the Alexa color names
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AlexaColor {
    public static final List<AlexaColor> ALEXA_COLORS = List.of( //
            new AlexaColor("white", new HSBType("0,0,100")), //
            new AlexaColor("red", new HSBType("0,100,100")), //
            new AlexaColor("crimson", new HSBType("348,90,100")), //
            new AlexaColor("salmon", new HSBType("16,52,100")), //
            new AlexaColor("orange", new HSBType("38,100,100")), //
            new AlexaColor("gold", new HSBType("49,100,100")), //
            new AlexaColor("yellow", new HSBType("60,100,100")), //
            new AlexaColor("green", new HSBType("120,100,100")), //
            new AlexaColor("turquoise", new HSBType("173,72,100")), //
            new AlexaColor("cyan", new HSBType("180,100,100")), //
            new AlexaColor("sky_blue", new HSBType("197,42,100")), //
            new AlexaColor("blue", new HSBType("240,100,100")), //
            new AlexaColor("purple", new HSBType("276,86,100")), //
            new AlexaColor("magenta", new HSBType("300,100,100")), //
            new AlexaColor("pink", new HSBType("348,25,100")), //
            new AlexaColor("lavender", new HSBType("255,50,100")));

    public final String colorName;
    final HSBType value;
    private final double[] lab;

    public AlexaColor(String colorName, HSBType value) {
        this.colorName = colorName;
        this.value = value;
        this.lab = getLabFromHSB(value);
    }

    /**
     * get the closest Alexa color
     *
     * @param value a given HSB color
     * @return the name of the closest pre-defined Alexa color
     */
    public static String getClosestColorName(HSBType value) {
        double[] lab = getLabFromHSB(value);
        String colorName = "";
        double smallestDistance = Double.MAX_VALUE;
        for (AlexaColor color : ALEXA_COLORS) {
            double distance = color.getEuclideanDistance(lab);
            if (distance < smallestDistance) {
                colorName = color.colorName;
                smallestDistance = distance;
            }
        }
        return colorName;
    }

    private double getEuclideanDistance(double[] value) {
        double deltaL = value[0] - lab[0];
        double deltaA = value[1] - lab[1];
        double deltaB = value[2] - lab[2];

        return Math.sqrt(deltaL * deltaL + deltaA * deltaA + deltaB * deltaB);
    }

    private static double[] getLabFromHSB(HSBType value) {
        double r = value.getRed().doubleValue() / 100.0;
        double g = value.getGreen().doubleValue() / 100.0;
        double b = value.getBlue().doubleValue() / 100.0;

        // D65, 10 degree
        double xn = 94.811;
        double yn = 100.0;
        double zn = 107.304;

        double x = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b;
        double y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b;
        double z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b;

        double ls = 116.0 * labRoot(y / yn) - 16.0;
        double as = 500.0 * (labRoot(x / xn) - labRoot(y / yn));
        double bs = 200.0 * (labRoot(y / yn) - labRoot(z / zn));

        return new double[] { ls, as, bs };
    }

    private static double labRoot(double value) {
        if (value < 216.0 / 24389.0) {
            return (1.0 / 116.0) * ((24389.0 / 27.0) * value + 16.0);
        } else {
            return Math.pow(value, 1.0 / 3.0);
        }
    }
}
