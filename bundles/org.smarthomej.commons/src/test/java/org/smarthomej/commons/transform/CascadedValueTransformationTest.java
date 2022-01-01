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
package org.smarthomej.commons.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;

/**
 * The {@link CascadedValueTransformationTest} contains tests for the {@link
 * org.smarthomej.commons.transform.CascadedValueTransformation}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CascadedValueTransformationTest {
    private static final String T1_NAME = "TRANSFORM1";
    private static final String T1_PATTERN = "T1Pattern";
    private static final String T1_INPUT = "T1Input";
    private static final String T1_RESULT = "T1Result";

    private static final String T2_NAME = "TRANSFORM2";
    private static final String T2_PATTERN = "T2Pattern";
    private static final String T2_INPUT = T1_RESULT;
    private static final String T2_RESULT = "T2Result";

    @Mock
    private @NonNullByDefault({}) TransformationService transformationService1;

    @Mock
    private @NonNullByDefault({}) TransformationService transformationService2;

    private @NonNullByDefault({}) AutoCloseable closeable;

    private @NonNullByDefault({}) Map<String, TransformationService> serviceProvider;

    @BeforeEach
    public void init() throws TransformationException {
        closeable = MockitoAnnotations.openMocks(this);
        Mockito.when(transformationService1.transform(eq(T1_PATTERN), eq(T1_INPUT))).thenAnswer(answer -> T1_RESULT);
        Mockito.when(transformationService2.transform(eq(T2_PATTERN), eq(T1_INPUT))).thenAnswer(answer -> T2_RESULT);
        Mockito.when(transformationService2.transform(eq(T2_PATTERN), eq(T2_INPUT))).thenAnswer(answer -> T2_RESULT);

        serviceProvider = Map.of("TRANSFORM1", transformationService1, "TRANSFORM2", transformationService2);
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void testMissingTransformation() {
        String pattern = "TRANSFORM:pattern";

        CascadedValueTransformation transformation = new CascadedValueTransformation(pattern, serviceProvider::get);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertNull(result);
    }

    @Test
    public void testSingleTransformation() {
        String pattern = T1_NAME + ":" + T1_PATTERN;

        CascadedValueTransformation transformation = new CascadedValueTransformation(pattern, serviceProvider::get);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T1_RESULT, result);
    }

    @Test
    public void testInvalidFirstTransformation() {
        String pattern = T1_NAME + "X:" + T1_PATTERN + "∩" + T2_NAME + ":" + T2_PATTERN;

        CascadedValueTransformation transformation = new CascadedValueTransformation(pattern, serviceProvider::get);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertNull(result);
    }

    @Test
    public void testInvalidSecondTransformation() {
        String pattern = T1_NAME + ":" + T1_PATTERN + "∩" + T2_NAME + "X:" + T2_PATTERN;

        CascadedValueTransformation transformation = new CascadedValueTransformation(pattern, serviceProvider::get);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertNull(result);
    }

    @Test
    public void testDoubleTransformationWithoutSpaces() {
        String pattern = T1_NAME + ":" + T1_PATTERN + "∩" + T2_NAME + ":" + T2_PATTERN;

        CascadedValueTransformation transformation = new CascadedValueTransformation(pattern, serviceProvider::get);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }

    @Test
    public void testDoubleTransformationWithSpaces() {
        String pattern = " " + T1_NAME + " : " + T1_PATTERN + " ∩ " + T2_NAME + " : " + T2_PATTERN + " ";

        CascadedValueTransformation transformation = new CascadedValueTransformation(pattern, serviceProvider::get);
        String result = transformation.apply(T1_INPUT).orElse(null);

        assertEquals(T2_RESULT, result);
    }
}
