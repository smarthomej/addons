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
package org.smarthomej.binding.onewire;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.smarthomej.binding.onewire.internal.SensorId;

/**
 * Tests cases for {@link SensorId}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class SensorIdTest {

    @Test
    public void bareSensorIdConstructionTest() {
        SensorId sensorId = new SensorId("28.0123456789ab");
        assertEquals("/28.0123456789ab", sensorId.getFullPath());
        assertEquals("28.0123456789ab", sensorId.getId());
        assertEquals("28", sensorId.getFamilyId());

        sensorId = new SensorId("/28.0123456789ab");
        assertEquals("/28.0123456789ab", sensorId.getFullPath());
        assertEquals("28.0123456789ab", sensorId.getId());
        assertEquals("28", sensorId.getFamilyId());
    }

    @Test
    public void hubMainSensorIdConstructionTest() {
        SensorId sensorId = new SensorId("1F.0123456789ab/main/28.0123456789ab");
        assertEquals("/1F.0123456789ab/main/28.0123456789ab", sensorId.getFullPath());
        assertEquals("28.0123456789ab", sensorId.getId());
        assertEquals("28", sensorId.getFamilyId());

        sensorId = new SensorId("/1F.0123456789ab/main/28.0123456789ab");
        assertEquals("/1F.0123456789ab/main/28.0123456789ab", sensorId.getFullPath());
        assertEquals("28.0123456789ab", sensorId.getId());
        assertEquals("28", sensorId.getFamilyId());
    }

    @Test
    public void hubAuxSensorIdConstructionTest() {
        SensorId sensorId = new SensorId("1F.0123456789ab/aux/28.0123456789ab");
        assertEquals("/1F.0123456789ab/aux/28.0123456789ab", sensorId.getFullPath());
        assertEquals("28.0123456789ab", sensorId.getId());
        assertEquals("28", sensorId.getFamilyId());

        sensorId = new SensorId("/1F.0123456789ab/aux/28.0123456789ab");
        assertEquals("/1F.0123456789ab/aux/28.0123456789ab", sensorId.getFullPath());
        assertEquals("28.0123456789ab", sensorId.getId());
        assertEquals("28", sensorId.getFamilyId());
    }

    @Test
    public void equalsTest() {
        SensorId sensorId1 = new SensorId("1F.0123456789ab/aux/28.0123456789ab");
        SensorId sensorId2 = new SensorId("1F.0123456789ab/aux/28.0123456789ab");
        SensorId sensorId3 = new SensorId("1F.0123456789ab/aux/28.0123456789ac");

        assertTrue(sensorId1.equals(sensorId2));
        assertFalse(sensorId1.equals(sensorId3));
    }
}
