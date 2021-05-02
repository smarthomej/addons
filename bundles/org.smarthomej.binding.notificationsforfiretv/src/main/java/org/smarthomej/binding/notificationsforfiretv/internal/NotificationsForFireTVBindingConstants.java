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
package org.smarthomej.binding.notificationsforfiretv.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link NotificationsForFireTVBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Tom Blum - Initial contribution
 */
@NonNullByDefault
public class NotificationsForFireTVBindingConstants {

    private static final String BINDING_ID = "notificationsforfiretv";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_NOTIFICATION = new ThingTypeUID(BINDING_ID, "notification");
    // List of all Parameters
    public static final String TYPE = "type";
    public static final String TITLE = "title";
    public static final String MSG = "msg";
    public static final String DURATION = "duration";
    public static final String FONTSIZE = "fontsize";
    public static final String POSITION = "position";
    public static final String BKCOLOR = "bkgcolor";
    public static final String TRANSPARENCY = "transparency";
    public static final String OFFSET_X = "offset";
    public static final String OFFSET_Y = "offsety";
    public static final String APP = "app";
    public static final String FORCE = "force";
    public static final String INTERRUPT = "interrupt";
    public static final String FILENAME = "filename";
    public static final String FILENAME2 = "filename2";
}
