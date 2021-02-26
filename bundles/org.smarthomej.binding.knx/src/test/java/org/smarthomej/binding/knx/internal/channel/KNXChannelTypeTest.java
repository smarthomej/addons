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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class KNXChannelTypeTest {

    private KNXChannelType ct;

    @BeforeEach
    public void setup() {
        ct = new MyKNXChannelType("");
    }

    @Test
    public void testParse_withDPT_multiple_withRead() {
        ChannelConfiguration res = ct.parse("5.001:<1/3/22+0/3/22+<0/8/15");

        assertEquals("5.001", res.getDPT());
        assertEquals("1/3/22", res.getMainGA().getGA());
        assertTrue(res.getMainGA().isRead());
        assertEquals(3, res.getListenGAs().size());
        assertEquals(2, res.getReadGAs().size());
    }

    @Test
    public void testParse_withDPT_multiple_withoutRead() {
        ChannelConfiguration res = ct.parse("5.001:1/3/22+0/3/22+0/8/15");

        assertEquals("5.001", res.getDPT());
        assertEquals("1/3/22", res.getMainGA().getGA());
        assertFalse(res.getMainGA().isRead());
        assertEquals(3, res.getListenGAs().size());
        assertEquals(0, res.getReadGAs().size());
    }

    @Test
    public void testParse_withoutDPT_single_withoutRead() {
        ChannelConfiguration res = ct.parse("1/3/22");

        assertNull(res.getDPT());
        assertEquals("1/3/22", res.getMainGA().getGA());
        assertFalse(res.getMainGA().isRead());
        assertEquals(1, res.getListenGAs().size());
        assertEquals(0, res.getReadGAs().size());
    }

    @Test
    public void testParse_withoutDPT_single_witRead() {
        ChannelConfiguration res = ct.parse("<1/3/22");

        assertNull(res.getDPT());
        assertEquals("1/3/22", res.getMainGA().getGA());
        assertTrue(res.getMainGA().isRead());
        assertEquals(1, res.getListenGAs().size());
        assertEquals(1, res.getReadGAs().size());
    }

    @Test
    public void testParse_twoLevel() {
        ChannelConfiguration res = ct.parse("5.001:<3/1024+<4/1025");
        assertEquals("3/1024", res.getMainGA().getGA());
        assertEquals(2, res.getListenGAs().size());
        assertEquals(2, res.getReadGAs().size());
    }

    @Test
    public void testParse_freeLevel() {
        ChannelConfiguration res = ct.parse("5.001:<4610+<4611");
        assertEquals("4610", res.getMainGA().getGA());
        assertEquals(2, res.getListenGAs().size());
        assertEquals(2, res.getReadGAs().size());
    }

    private static class MyKNXChannelType extends KNXChannelType {
        public MyKNXChannelType(String channelTypeID) {
            super(channelTypeID);
        }

        @Override
        protected @NonNull Set<@NonNull String> getAllGAKeys() {
            return Collections.emptySet();
        }

        @Override
        protected @NonNull String getDefaultDPT(@NonNull String gaConfigKey) {
            return "";
        }
    }
}
