/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IrUtils} is a support class for decode/encode infra-red codes
 * <p>
 * Based on https://github.com/jasonacox/tinytuya/blob/master/tinytuya/Contrib/IRRemoteControlDevice.py
 *
 * @author Dmitry P. (d51x) - Initial contribution
 */
public class IrUtils {
    private static final Logger logger = LoggerFactory.getLogger(IrUtils.class);

    private IrUtils() {
    }

    /**
     * Convert Base64 code format from Tuya to nec-format.
     *
     * @param base64Code the base64 code format from Tuya
     * @return the bec-format code
     */
    public static String base64ToNec(String base64Code) {
        ArrayList<Integer> pulses = base64ToPulse(base64Code);
        return pulsesToNec(pulses).get(0);
    }

    private static ArrayList<Integer> base64ToPulse(String base64Code) {
        ArrayList<Integer> pulses = new ArrayList<>();
        String key = (base64Code.length() % 4 == 1 && base64Code.startsWith("1")) ? base64Code.substring(1)
                : base64Code;
        byte[] raw_bytes = Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8));

        int i = 0;
        while (i < raw_bytes.length) {
            int word = ((raw_bytes[i] & 0xFF) + (raw_bytes[i + 1] & 0xFF) * 256) & 0xFFFF;
            pulses.add(word);
            i += 2;
        }
        return pulses;
    }

    private static ArrayList<Long> pulsesToWidthEncoded(ArrayList<Integer> pulses, Integer start_mark,
            Integer start_space, Integer pulse_threshold, Integer space_threshold) {
        ArrayList<Long> ret = new ArrayList<>();
        if (pulses.size() < 68) {
            // logger.warn("Length of pulses must be a multiple of 68! (2 start + 64 data + 2 trailing)");
            return null;
        }

        if (pulse_threshold == null && space_threshold == null) {
            // logger.error("pulse_threshold and/or space_threshold must be supplied!");
            return null;
        }

        if (start_mark != null) {
            while (pulses.size() >= 68
                    && (pulses.get(0) < (start_mark * 0.75) || pulses.get(0) > (start_mark * 1.25))) {
                pulses.remove(0);
            }

            while (pulses.size() >= 68) {
                if (pulses.get(0) < start_mark * 0.75 || pulses.get(0) > start_mark * 1.25) {
                    // logger.error("The start mark is not the correct length");
                    return null;
                }

                if (start_space != null
                        && (pulses.get(1) < (start_space * 0.75) || pulses.get(1) > (start_space * 1.25))) {
                    // logger.error("The start space is not the correct length");
                    return null;
                }

                // remove two first elements
                pulses.remove(0);
                pulses.remove(0);

                Integer res = 0;
                long x = 0L;

                for (int i = 31; i >= 0; i--) {
                    Integer pulse_match = null;
                    Integer space_match = null;

                    if (pulse_threshold != null) {
                        pulse_match = pulses.get(0) >= pulse_threshold ? 1 : 0;
                    }
                    if (space_threshold != null) {
                        space_match = pulses.get(1) >= space_threshold ? 1 : 0;
                    }

                    if (pulse_match != null && space_match != null) {
                        if (!pulse_match.equals(space_match)) {
                            // logger.error("Both 'pulse_threshold' and 'space_threshold' are supplied and bit {}
                            // conflicts with both!", i);
                            return null;
                        }
                        res = space_match;
                    } else if (pulse_match == null) {
                        res = space_match;
                    } else {
                        res = pulse_match;
                    }

                    if (res != null) {
                        x |= (long) (res) << i;
                    }

                    // remove two first elements
                    pulses.remove(0);
                    pulses.remove(0);
                }

                // remove two first elements
                // pulses.remove(0);
                // pulses.remove(0);

                if (!ret.contains(x)) {
                    ret.add(x);
                }
            }
        }

        return ret;
    }

    private static ArrayList<Long> widthEncodedToPulses(long data, PulseParams param) {
        ArrayList<Long> pulses = new ArrayList<>();
        pulses.add(param.startMark);
        pulses.add(param.startSpace);

        for (int i = 31; i >= 0; i--) {
            if ((data & (1 << i)) > 0L) {
                pulses.add(param.pulseOne);
                pulses.add(param.spaceOne);
            } else {
                pulses.add(param.pulseZero);
                pulses.add(param.spaceZero);
            }
        }
        pulses.add(param.trailingPulse);
        pulses.add(param.trailingSpace);
        return pulses;
    }

    private static long mirrorBits(long data, int bits) {
        int shift = bits - 1;
        long out = 0;

        for (int i = 0; i < bits; i++) {
            if ((data & (1L << i)) > 0L) {
                out |= 1L << shift;
            }
            shift -= 1;
        }
        return out & 0xFF;
    }

    private static List<String> pulsesToNec(ArrayList<Integer> pulses) {
        List<String> ret = new ArrayList<>();
        ArrayList<Long> res = pulsesToWidthEncoded(pulses, 9000, null, null, 1125);

        for (Long code : res) {
            long addr = mirrorBits((code >> 24) & 0xFF, 8);
            long addr_not = mirrorBits((code >> 16) & 0xFF, 8);
            long data = mirrorBits((code >> 8) & 0xFF, 8);
            long data_not = mirrorBits(code & 0xFF, 8);

            if (addr != (addr_not ^ 0xFF)) {
                addr = (addr << 8) | addr_not;
            }
            String d = String.format(
                    "{ \"type\": \"nec\", \"uint32\": %d, \"address\": None, \"data\": None, \"hex\": \"%08X\" }", code,
                    code);
            if (data == (data_not ^ 0xFF)) {
                d = String.format(
                        "{ \"type\": \"nec\", \"uint32\": %d, \"address\": %d, \"data\": %d, \"hex\": \"%08X\" }", code,
                        addr, data, code);
            }
            ret.add(d);
        }
        return ret;
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static ArrayList<Long> necToPulses(long address, Long data) {
        Long newAddress, newData;
        if (data == null) {
            newAddress = address;
        } else {
            if (address < 256) {
                newAddress = mirrorBits(address, 8);
                newAddress = (newAddress << 8) | (newAddress ^ 0xFF);
            } else {
                newAddress = (mirrorBits((address >> 8) & 0xFF, 8) << 8) | mirrorBits(address & 0xFF, 8);
            }
            newData = mirrorBits(data, 8);
            newData = (newData << 8) | (newData & 0xFF);
            newAddress = (newAddress << 16) | newData;
        }

        return widthEncodedToPulses(newAddress, new PulseParams());
    }

    private static String pulsesToBase64(ArrayList<Long> pulses) {
        byte[] bytes = new byte[pulses.size() * 2];

        final Integer[] i = { 0 };

        pulses.forEach(p -> {
            int val = p.shortValue();
            bytes[i[0]] = (byte) (val & 0xFF);
            bytes[i[0] + 1] = (byte) ((val >> 8) & 0xFF);
            i[0] = i[0] + 2;
        });

        return new String(Base64.getEncoder().encode(bytes));
    }

    /**
     * Convert Nec-format code to base64-format code from Tuya
     *
     * @param code nec-format code
     * @return the string
     */
    public static String necToBase64(long code) {
        ArrayList<Long> pulses = necToPulses(code, null);
        return pulsesToBase64(pulses);
    }

    private static class PulseParams {
        /**
         * The Start mark.
         */
        public long startMark = 9000;
        /**
         * The Start space.
         */
        public long startSpace = 4500;
        /**
         * The Pulse one.
         */
        public long pulseOne = 563;
        /**
         * The Pulse zero.
         */
        public long pulseZero = 563;
        /**
         * The Space one.
         */
        public long spaceOne = 1688;
        /**
         * The Space zero.
         */
        public long spaceZero = 563;
        /**
         * The Trailing pulse.
         */
        public long trailingPulse = 563;
        /**
         * The Trailing space.
         */
        public long trailingSpace = 30000;
    }
}
