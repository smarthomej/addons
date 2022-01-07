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
package org.smarthomej.binding.amazonechocontrol.internal;

import static com.jayway.jsonpath.Criteria.where;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The {@link JsonDocumentTest} contains tests for the {@link JsonDocument} class
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JsonDocumentTest {
    private final String json = "{\"key\" : [1, 3, 7], \"key2\" : \"stringValue\"}";
    private final JsonDocument document = new JsonDocument(json);

    @Test
    public void integerListTest() {
        List<Integer> value = document.getList("$.key", Integer.class);
        Assertions.assertNotNull(value);
        Objects.requireNonNull(value);
        Assertions.assertEquals(3, value.size());
    }

    @Test
    public void integerListMissingTest() {
        List<Integer> value = document.getList("$.keya", Integer.class);
        Assertions.assertNull(value);
    }

    @Test
    public void stringTest() {
        String valueString = document.get("$.key2", String.class);
        Assertions.assertNotNull(valueString);
        Assertions.assertEquals("stringValue", valueString);
    }

    @Test
    public void stringMissingTest() {
        String valueString2 = document.get("$.key2a", String.class);
        Assertions.assertNull(valueString2);
    }

    @Test
    public void filterTest() {
        String json = "{\"wakeWords\":[{\"active\":true,\"deviceSerialNumber\":\"G081470503340B84\",\"deviceType\":\"A30YDR2MK8HMRV\",\"midFieldState\":null,\"wakeWord\":\"ALEXA\"},{\"active\":true,\"deviceSerialNumber\":\"G090XG2154640M15\",\"deviceType\":\"A1RABVCI4QCIKC\",\"midFieldState\":null,\"wakeWord\":\"ALEXA\"}]}";
        JsonDocument document = new JsonDocument(json);
        String serialNumber = "G081470503340B84";
        List<String> wakeword = document.getList("$.wakeWords[?].wakeWord", String.class,
                where("deviceSerialNumber").eq(serialNumber));
        Assertions.assertNotNull(wakeword);
        Objects.requireNonNull(wakeword);
        Assertions.assertEquals(1, wakeword.size());
        Assertions.assertEquals("ALEXA", wakeword.get(0));
    }

    @Test
    public void pathTest() {
        String json = "{\"wakeWords\":[{\"active\":true,\"deviceSerialNumber\":\"G081470503340B84\",\"deviceType\":\"A30YDR2MK8HMRV\",\"midFieldState\":null,\"wakeWord\":\"ALEXA\"},{\"active\":true,\"deviceSerialNumber\":\"G090XG2154640M15\",\"deviceType\":\"A1RABVCI4QCIKC\",\"midFieldState\":null,\"wakeWord\":\"ALEXA\"}]}";
        JsonDocument document = new JsonDocument(json);
        String wakewordPath = document.getFirstPath("$.wakeWords[?].wakeWord",
                where("deviceSerialNumber").eq("G081470503340B84"));
        Assertions.assertNotNull(wakewordPath);
        Objects.requireNonNull(wakewordPath);
        Assertions.assertEquals("$['wakeWords'][0]['wakeWord']", wakewordPath);
    }
}
