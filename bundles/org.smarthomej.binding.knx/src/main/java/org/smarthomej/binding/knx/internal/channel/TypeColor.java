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
package org.smarthomej.binding.knx.internal.channel;

import static org.smarthomej.binding.knx.internal.KNXBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Channel;

import tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.DPTXlatorRGB;

/**
 * color channel type description
 *
 * @author Helmut Lehmeyer - initial contribution
 *
 */
@NonNullByDefault
class TypeColor extends KNXChannel {
    public static final Set<String> SUPPORTED_CHANNEL_TYPES = Set.of(CHANNEL_COLOR, CHANNEL_COLOR_CONTROL);

    TypeColor(Channel channel) {
        super(Set.of(SWITCH_GA, POSITION_GA, INCREASE_DECREASE_GA, HSB_GA), channel);
    }

    @Override
    protected String getDefaultDPT(String gaConfigKey) {
        if (gaConfigKey.equals(HSB_GA)) {
            return DPTXlatorRGB.DPT_RGB.getID();
        }
        if (gaConfigKey.equals(INCREASE_DECREASE_GA)) {
            return DPTXlator3BitControlled.DPT_CONTROL_DIMMING.getID();
        }
        if (gaConfigKey.equals(SWITCH_GA)) {
            return DPTXlatorBoolean.DPT_SWITCH.getID();
        }
        if (gaConfigKey.equals(POSITION_GA)) {
            return DPTXlator8BitUnsigned.DPT_SCALING.getID();
        }
        throw new IllegalArgumentException("GA configuration '" + gaConfigKey + "' is not supported");
    }
}
