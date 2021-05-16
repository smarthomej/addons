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
package org.smarthomej.binding.math.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;

/**
 * Unit test for {@link MultiplyTransformationService}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
class MultiplyTransformationServiceTest {
    private final TransformationService subject = new MultiplyTransformationService();

    @Test
    public void testTransform() throws TransformationException {
        String result = subject.transform("20", "100");

        assertEquals("2000", result);
    }

    @Test
    public void testTransformInsideString() throws TransformationException {
        String result = subject.transform("-20", "-0.5 watt");

        assertEquals("10.0 watt", result);
    }

    @Test
    public void testTransformInvalidSource() {
        assertThrows(TransformationException.class, () -> subject.transform("20", "*"));
    }

    @Test
    public void testTransformInvalidFunction() {
        assertThrows(TransformationException.class, () -> subject.transform("*", "90"));
    }
}
