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
package org.smarthomej.binding.tr064.internal.phonebook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * The {@link Tr064PhonebookImplTest} class implements test cases for the {@link Tr064PhonebookImpl} class
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class Tr064PhonebookImplTest {
    @Mock
    private @NonNullByDefault({}) HttpClient httpClient;

    // key -> input, value -> output
    public static Collection<Map.Entry<String, String>> phoneNumbers() {
        return List.of( //
                Map.entry("**820", "**820"), //
                Map.entry("49200123456", "49200123456"), //
                Map.entry("+49-200-123456", "+49200123456"), //
                Map.entry("49 (200) 123456", "49200123456"), //
                Map.entry("+49 200/123456", "+49200123456"));
    }

    @ParameterizedTest
    @MethodSource("phoneNumbers")
    public void testNormalization(Map.Entry<String, String> input) {
        when(httpClient.newRequest((String) any())).thenThrow(new IllegalArgumentException("testing"));
        Tr064PhonebookImpl testPhonebook = new Tr064PhonebookImpl(httpClient, "", 0);
        Assertions.assertEquals(input.getValue(), testPhonebook.normalizeNumber(input.getKey()));
    }
}
