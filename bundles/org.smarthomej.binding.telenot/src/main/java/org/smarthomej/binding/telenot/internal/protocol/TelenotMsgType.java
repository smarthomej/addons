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
package org.smarthomej.binding.telenot.internal.protocol;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The various message types that come from the GMS interface
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public enum TelenotMsgType {
    SEND_NORM,
    CONF_ACK,
    MP,
    SB,
    SYS_EXT_ARMED,
    SYS_INT_ARMED,
    SYS_DISARMED,
    ALARM,
    INTRUSION,
    BATTERY_MALFUNCTION,
    POWER_OUTAGE,
    OPTICAL_FLASHER_MALFUNCTION,
    HORN_1_MALFUNCTION,
    HORN_2_MALFUNCTION,
    COM_FAULT,
    RESTART,
    USED_INPUTS,
    USED_OUTPUTS,
    USED_CONTACTS_INFO,
    USED_OUTPUT_CONTACTS_INFO,
    USED_SB_CONTACTS_INFO,
    USED_MB_CONTACTS_INFO,
    UNKNOWN,
    INVALID,
    NOT_USED_CONTACT;

    /** hash map from protocol message heading to type */
    private static final Map<String, TelenotMsgType> START_TO_MSG_TYPE = Map.of("682C2C687302050201001001",
            TelenotMsgType.INTRUSION, "681A1A687302050200001401", TelenotMsgType.BATTERY_MALFUNCTION,
            "681A1A687302050200001501", TelenotMsgType.POWER_OUTAGE, "681A1A687302050200001301",
            TelenotMsgType.OPTICAL_FLASHER_MALFUNCTION, "681A1A687302050200001101", TelenotMsgType.HORN_1_MALFUNCTION,
            "681A1A687302050200001201", TelenotMsgType.HORN_2_MALFUNCTION, "681A1A687302050200001701",
            TelenotMsgType.COM_FAULT, "68060668730202110019A116", TelenotMsgType.NOT_USED_CONTACT);

    /**
     * Extract message type from message. Relies on static map startToMsgType.
     *
     * @param s message string
     * @return message type
     */
    public static TelenotMsgType getMsgType(@Nullable String s) {
        TelenotMsgType mt = null;
        if (s == null || s.length() < 4) {
            return TelenotMsgType.INVALID;
        }

        if (s.length() > 16) {
            mt = START_TO_MSG_TYPE.get(s.substring(0, 24));
        }

        if (mt == null) {
            if (s.matches("^6802026840024216(.*)")) {
                mt = TelenotMsgType.SEND_NORM;
            }
            if (s.matches("^6802026800020216(.*)")) {
                mt = TelenotMsgType.CONF_ACK;
            }
            if (s.matches("^68\\w\\w\\w\\w687302\\w\\w2400000001(.*)16$")) {
                mt = TelenotMsgType.MP;
            }
            if (s.matches("^68\\w\\w\\w\\w687302\\w\\w2400050002(.*)16$")) {
                mt = TelenotMsgType.SB;
            }
            if (s.matches("^682[C|c]2[C|c]6873020502\\w\\w\\w\\w\\w\\w01(22|[A|a]2)(.*)$")) {
                mt = TelenotMsgType.ALARM;
            }
            if (s.matches("^682[C|c]2[C|c]6873020502\\w\\w\\w\\w\\w\\w0161(.*)$")) {
                mt = TelenotMsgType.SYS_EXT_ARMED;
            }
            if (s.matches("^682[C|c]2[C|c]6873020502\\w\\w\\w\\w\\w\\w0162(.*)$")) {
                mt = TelenotMsgType.SYS_INT_ARMED;
            }
            if (s.matches("^682[C|c]2[C|c]6873020502\\w\\w\\w\\w\\w\\w01[E|e]1(.*)$")) {
                mt = TelenotMsgType.SYS_DISARMED;
            }
            if (s.matches("^68\\w\\w\\w\\w687302\\w\\w2400000071(.*)16$")) {
                mt = TelenotMsgType.USED_INPUTS;
            }
            if (s.matches("^68\\w\\w\\w\\w687302\\w\\w2400050072(.*)16$")) {
                mt = TelenotMsgType.USED_OUTPUTS;
            }
            if (s.matches("^68\\w\\w\\w\\w687302\\w\\w0[C|c](.*)16$")) {
                int address = Integer.parseInt(s.substring(18, 22), 16);
                if (address >= 0 && address <= 1279) {
                    mt = TelenotMsgType.USED_CONTACTS_INFO;
                } else if (address >= 1280 && address <= 1327) {
                    mt = TelenotMsgType.USED_OUTPUT_CONTACTS_INFO;
                } else if (address >= 1328 && address <= 1391) {
                    mt = TelenotMsgType.USED_SB_CONTACTS_INFO;
                } else if (address >= 1392 && address <= 1519) {
                    mt = TelenotMsgType.USED_MB_CONTACTS_INFO;
                }
            }
            if (s.matches("^68\\w\\w\\w\\w687302\\w\\w\\w\\w\\w\\w([F|f]{4})0153(.*)16$")) {
                mt = TelenotMsgType.RESTART;
            }
        }

        if (mt == null) {
            if (s.matches("^6868(.*)16")) {
                mt = TelenotMsgType.UNKNOWN;
            } else {
                mt = TelenotMsgType.INVALID;
            }
        }

        return mt;
    }
}
