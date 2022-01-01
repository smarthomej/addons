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
package org.smarthomej.transform.math.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;

/**
 * Unit test for {@link DivideTransformationService}.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Jan N. Klug - adapted to DivideTransformation
 */
@NonNullByDefault
class DivideTransformationServiceTest {
    private final TransformationService subject = new DivideTransformationService();

    @Test
    public void testTransform() throws TransformationException {
        String result = subject.transform("-20", "-2000");

        assertEquals("100", result);
    }

    @Test
    public void testTransformInsideString() throws TransformationException {
        String result = subject.transform("60", "90 watts");

        assertEquals("1.5 watts", result);
    }

    @Test
    public void testTransformInvalidSource() {
        assertThrows(TransformationException.class, () -> subject.transform("20", "*"));
    }

    @Test
    public void testTransformInvalidFunction() {
        assertThrows(TransformationException.class, () -> subject.transform("*", "90"));
    }

    @Test
    public void testTransformDivideByZero() {
        assertThrows(TransformationException.class, () -> subject.transform("0", "1"));
    }
}
