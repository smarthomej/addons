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

import tuwien.auto.calimero.dptxlator.DPTXlatorString;

/**
 * string channel type description
 * 
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
class TypeString extends KNXChannel {
    public static final Set<String> SUPPORTED_CHANNEL_TYPES = Set.of(CHANNEL_STRING, CHANNEL_STRING_CONTROL);

    TypeString(Channel channel) {
        super(channel);
    }

    @Override
    protected String getDefaultDPT(String gaConfigKey) {
        return DPTXlatorString.DPT_STRING_8859_1.getID();
    }
}
