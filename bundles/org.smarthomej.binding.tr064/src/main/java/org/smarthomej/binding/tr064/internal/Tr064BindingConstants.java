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
package org.smarthomej.binding.tr064.internal;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.smarthomej.binding.tr064.internal.dto.config.ChannelTypeDescription;
import org.smarthomej.binding.tr064.internal.util.Util;

/**
 * The {@link Tr064BindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Tr064BindingConstants {
    public static final String BINDING_ID = "tr064";

    public static final ThingTypeUID THING_TYPE_GENERIC = new ThingTypeUID(BINDING_ID, "generic");
    public static final ThingTypeUID THING_TYPE_FRITZBOX = new ThingTypeUID(BINDING_ID, "fritzbox");
    public static final ThingTypeUID THING_TYPE_SUBDEVICE = new ThingTypeUID(BINDING_ID, "subdevice");
    public static final ThingTypeUID THING_TYPE_SUBDEVICE_LAN = new ThingTypeUID(BINDING_ID, "subdeviceLan");

    public static final List<ChannelTypeDescription> CHANNEL_TYPES = Util.readXMLChannelConfig();
}
