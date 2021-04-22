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
package org.smarthomej.binding.telenot.internal.protocol;

import java.util.HashMap;
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
    SEND_NORM, // ACK Message
    CONF_ACK, // CONF_ACK Message
    MP, // Meldebereiche
    SB, // Sicherungsbereiche
    SYS_EXT_ARMED, // system sytem externally armed
    SYS_INT_ARMED, // system sytem internally armed
    SYS_DISARMED, // system sytem disarmed
    ALARM,
    INTRUSION, //
    BATTERY_MALFUNCTION, //
    POWER_OUTAGE, //
    OPTICAL_FLASHER_MALFUNCTION, //
    HORN_1_MALFUNCTION, //
    HORN_2_MALFUNCTION, //
    RESTART, //
    USED_INPUTS,
    USED_OUTPUTS,
    USED_CONTACTS_INFO,
    USED_OUTPUT_CONTACTS_INFO,
    USED_SB_CONTACTS_INFO,
    USED_MB_CONTACTS_INFO,
    INVALID; // invalid message

    /** hash map from protocol message heading to type */
    private static Map<String, TelenotMsgType> startToMsgType = new HashMap<>();

    static {
        // info message "system intrusion detection" ;"682c2c68730205020100100123"
        // info message "system intrusion cleared" ;"682c2c687302050201001001a3"
        startToMsgType.put("682c2c687302050201001001", TelenotMsgType.INTRUSION);

        // System detailss

        // info message "battery malfunction" ;"681a1a68730205020000140133"
        // info message "battery malfunction cleared" ;"681a1a687302050200001401b3"
        startToMsgType.put("681a1a687302050200001401", TelenotMsgType.BATTERY_MALFUNCTION);

        // info message "power outage" -> Stromausfall ;"681a1a68730205020000150132"
        // info message "power outage cleared" ;"681a1a687302050200001501b2"
        startToMsgType.put("681a1a687302050200001501", TelenotMsgType.POWER_OUTAGE);

        // info message "optical flasher malfunction" ;"681a1a68730205020000130130"
        // info message "optical flasher malfunction cleared" ";681a1a687302050200001301b0"
        startToMsgType.put("681a1a687302050200001301", TelenotMsgType.OPTICAL_FLASHER_MALFUNCTION);

        // info message "acoustic alarm horn 1 malfunction" ;"681a1a68730205020000110130"
        // info message "acoustic alarm horn 1 malfunction cleared" ;"681a1a687302050200001101b0"
        startToMsgType.put("681a1a687302050200001101", TelenotMsgType.HORN_1_MALFUNCTION);

        // info message "acoustic alarm horn 2 malfunction" ;"681a1a68730205020000120130"
        // info message "acoustic alarm horn 2 malfunction cleared" ;"681a1a687302050200001201b0"
        startToMsgType.put("681a1a687302050200001201", TelenotMsgType.HORN_2_MALFUNCTION);
    }

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
            mt = startToMsgType.get(s.substring(0, 24));
        }

        String regEX;
        if (mt == null) {
            regEX = "^6802026840024216(.*)";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.SEND_NORM;
            }
            regEX = "^6802026800020216(.*)";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.CONF_ACK;
            }
            regEX = "^68\\w\\w\\w\\w687302\\w\\w2400000001(.*)16$";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.MP;
            }
            regEX = "^68\\w\\w\\w\\w687302\\w\\w2400050002(.*)16$";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.SB;
            }
            regEX = "^682c2c6873020502\\w\\w\\w\\w\\w\\w01(22|a2)(.*)$";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.ALARM;
            }
            regEX = "^682c2c6873020502\\w\\w\\w\\w\\w\\w0161(.*)$";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.SYS_EXT_ARMED;
            }
            regEX = "^682c2c6873020502\\w\\w\\w\\w\\w\\w0162(.*)$";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.SYS_INT_ARMED;
            }
            regEX = "^682c2c6873020502\\w\\w\\w\\w\\w\\w01e1(.*)$";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.SYS_DISARMED;
            }
            regEX = "^68\\w\\w\\w\\w687302\\w\\w2400000071(.*)16$";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.USED_INPUTS;
            }
            regEX = "^68\\w\\w\\w\\w687302\\w\\w2400050072(.*)16$";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.USED_OUTPUTS;
            }
            regEX = "^68\\w\\w\\w\\w687302\\w\\w0c(.*)16$";
            if (s.matches(regEX)) {
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
            regEX = "^68\\w\\w\\w\\w687302\\w\\w\\w\\w\\w\\wffff0153(.*)16$";
            if (s.matches(regEX)) {
                mt = TelenotMsgType.RESTART;
            }
        }

        if (mt == null) {
            mt = TelenotMsgType.INVALID;
        }

        return mt;
    }
}
