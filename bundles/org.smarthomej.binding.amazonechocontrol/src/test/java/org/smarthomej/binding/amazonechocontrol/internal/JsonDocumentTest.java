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
import java.util.Map;
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
    private final String json = "{\"store\":{\"book\":[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99},{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99},{\"category\":\"fiction\",\"author\":\"J. R. R. Tolkien\",\"title\":\"The Lord of the Rings\",\"isbn\":\"0-395-19395-8\",\"price\":22.99}],\"bicycle\":{\"color\":\"red\",\"price\":19.95}},\"expensive\":10}";

    private final JsonDocument document = new JsonDocument(json);

    @Test
    public void doubleListTest() {
        List<Double> value = document.getList("$.store.book[*].price", Double.class);
        Assertions.assertNotNull(value);
        Objects.requireNonNull(value);
        Assertions.assertEquals(4, value.size());
    }

    @Test
    public void integerListMissingTest() {
        List<String> value = document.getList("$.store.displays", String.class);
        Assertions.assertNull(value);
    }

    @Test
    public void singleValueTest() {
        Integer value = document.get("$.expensive", Integer.class);
        Assertions.assertNotNull(value);
        Assertions.assertEquals(10, value);
    }

    @Test
    public void singleValueMissingTest() {
        Integer value = document.get("$.explosive", Integer.class);
        Assertions.assertNull(value);
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
        String authorPath = document.getFirstPath("$.store.book[*].author");
        Assertions.assertNotNull(authorPath);
        Objects.requireNonNull(authorPath);
        Assertions.assertEquals("$['store']['book'][0]['author']", authorPath);
    }

    @Test
    public void mapTest() {
        List<Map<String, Object>> maps = document.getMapList("$.store.book[*]['author', 'title']");
        Assertions.assertNotNull(maps);
        Objects.requireNonNull(maps);
        Assertions.assertEquals(4, maps.size());
        Assertions.assertEquals("Nigel Rees", maps.get(0).get("author"));
        Assertions.assertEquals("Sword of Honour", maps.get(1).get("title"));
    }
}
