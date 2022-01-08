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
package org.smarthomej.commons.util;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The {@link ResourceUtilTest} contains tests for the {@link ResourceUtil} class
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ResourceUtilTest {

    @Test
    public void getResourceStreamTest() {
        Optional<InputStream> inputStream = ResourceUtil.getResourceStream(ResourceUtilTest.class, "test.properties");

        Assertions.assertTrue(inputStream.isPresent());
    }

    @Test
    public void getResourceFileMissingTest() {
        Optional<InputStream> inputStream = ResourceUtil.getResourceStream(ResourceUtilTest.class,
                "missing.properties");

        Assertions.assertTrue(inputStream.isEmpty());
    }

    @Test
    public void readPropertiesTest() {
        Map<String, String> properties = ResourceUtil.readProperties(ResourceUtilTest.class, "test.properties");

        Assertions.assertEquals(2, properties.size());
        Assertions.assertEquals("value1", properties.get("key1"));
        Assertions.assertEquals("value2", properties.get("key2"));
    }

    @Test
    public void readPropertiesFileMissingTest() {
        Map<String, String> properties = ResourceUtil.readProperties(ResourceUtilTest.class, "missing.properties");

        Assertions.assertEquals(0, properties.size());
    }
}
