/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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
package org.smarthomej.transform.format.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.transform.TransformationException;
import org.osgi.framework.BundleContext;

/**
 * @author Gaël L'hopital - Initial contribution
 * @author Jan N. Klug - adapted to FormatTransformation
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@NonNullByDefault
public class FormatTransformationServiceTest {

    private static final String SOURCE_TEST = "TESTSTRING";
    private static final String IDENTITY_FILENAME = "identity.format";
    private static final String XML_FILENAME = "xml.format";
    private static final String DOUBLE_FILENAME = "double.format";
    private static final String RESOURCE_PATH = "src" + File.separator + "test" + File.separator + "resources"
            + File.separator;
    private @Mock @NonNullByDefault({}) BundleContext bundleContext;
    private @NonNullByDefault({}) TestableFormatTransformationService processor;

    private class TestableFormatTransformationService extends FormatTransformationService {
        @Override
        protected String getSourcePath() {
            return RESOURCE_PATH + super.getSourcePath();
        }

        @Override
        protected Locale getLocale() {
            return Locale.US;
        }

        @Override
        public void activate(BundleContext context) {
            super.activate(context);
        }

        @Override
        public void deactivate() {
            super.deactivate();
        }
    }

    @BeforeEach
    public void setUp() {
        processor = new TestableFormatTransformationService();
        processor.activate(bundleContext);
    }

    @AfterEach
    public void tearDown() {
        processor.deactivate();
    }

    @Test
    public void testIdentityTransformation() throws TransformationException {
        String transformedResponse = processor.transform(IDENTITY_FILENAME, SOURCE_TEST);
        assertEquals(SOURCE_TEST, transformedResponse);
    }

    @Test
    public void testDoubleTransformation() throws TransformationException {
        String transformedResponse = processor.transform(DOUBLE_FILENAME, SOURCE_TEST);
        assertEquals(SOURCE_TEST + " - " + SOURCE_TEST, transformedResponse);
    }

    @Test
    public void testXmlTransformation() throws IOException, TransformationException {
        // Test complex transformation
        String expectedXml = new Scanner(new File(RESOURCE_PATH + "xml.expected")).useDelimiter("\\Z").next();
        String transformedResponse = processor.transform(XML_FILENAME, SOURCE_TEST);
        assertEquals(expectedXml, transformedResponse);
    }
}
