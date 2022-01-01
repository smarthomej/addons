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
package org.smarthomej.binding.tcpudp.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link TcpUdpBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TcpUdpBindingConstants {

    public static final String BINDING_ID = "tcpudp";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_UID_CLIENT = new ThingTypeUID(BINDING_ID, "client");
    public static final ThingTypeUID THING_TYPE_UID_RECEIVER = new ThingTypeUID(BINDING_ID, "receiver");
}
