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
package org.smarthomej.binding.tuya.internal.local;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link CommandType} maps the numeric command types to an enum
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public enum CommandType {
    UDP(0),
    AP_CONFIG(1),
    ACTIVE(2),
    BIND(3),
    RENAME_GW(4),
    RENAME_DEVICE(5),
    UNBIND(6),
    CONTROL(7),
    STATUS(8),
    HEART_BEAT(9),
    DP_QUERY(10),
    QUERY_WIFI(11),
    TOKEN_BIND(12),
    CONTROL_NEW(13),
    ENABLE_WIFI(14),
    DP_QUERY_NEW(16),
    SCENE_EXECUTE(17),
    DP_REFRESH(18),
    UDP_NEW(19),
    AP_CONFIG_NEW(20),
    LAN_GW_ACTIVE(240),
    LAN_SUB_DEV_REQUEST(241),
    LAN_DELETE_SUB_DEV(242),
    LAN_REPORT_SUB_DEV(243),
    LAN_SCENE(244),
    LAN_PUBLISH_CLOUD_CONFIG(245),
    LAN_PUBLISH_APP_CONFIG(246),
    LAN_EXPORT_APP_CONFIG(247),
    LAN_PUBLISH_SCENE_PANEL(248),
    LAN_REMOVE_GW(249),
    LAN_CHECK_GW_UPDATE(250),
    LAN_GW_UPDATE(251),
    LAN_SET_GW_CHANNEL(252),
    DP_QUERY_NOT_SUPPORTED(-1); // this is an internal value

    private final int code;

    CommandType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CommandType fromCode(int code) {
        return Arrays.stream(values()).filter(t -> t.code == code).findAny().orElseThrow(IllegalArgumentException::new);
    }
}
