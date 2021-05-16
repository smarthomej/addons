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
package org.smarthomej.binding.knx.internal.dpt;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;

/**
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
@NonNullByDefault
public class KNXCoreTypeMapperTest {

    @Test
    public void testToDPTValueTrailingZeroesStrippedOff() {
        assertEquals("3", new KNXCoreTypeMapper().toDPTValue(new DecimalType("3"), "17.001"));
        assertEquals("3", new KNXCoreTypeMapper().toDPTValue(new DecimalType("3.0"), "17.001"));
    }

    @Test
    public void testToDPTValueDecimalType() {
        assertEquals("23.1", new KNXCoreTypeMapper().toDPTValue(new DecimalType("23.1"), "9.001"));
    }

    @Test
    public void testToDPTValueQuantityType() {
        assertEquals("23.1", new KNXCoreTypeMapper().toDPTValue(new QuantityType<>("23.1 °C"), "9.001"));
    }
}
