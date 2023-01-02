/**
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
package org.smarthomej.transform.chain.internal.test;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
import org.smarthomej.commons.transform.CascadedValueTransformation;
import org.smarthomej.commons.transform.NoOpValueTransformation;
import org.smarthomej.commons.transform.ValueTransformation;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link TestValueTransformationProvider} is a test transformation provider providing "APPEND", "DUPLICATE" and
 * "FAIL" transformations
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TestValueTransformationProvider implements ValueTransformationProvider {
    private Map<String, TransformationService> transformationServiceMap = Map.of("FAIL",
            new FailTransformationService(), "APPEND", new AppendTransformationService(), "DUPLICATE",
            new DuplicateTransformationService());

    @Override
    public ValueTransformation getValueTransformation(@Nullable String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return NoOpValueTransformation.getInstance();
        }

        return new CascadedValueTransformation(pattern, transformationServiceMap::get);
    }

    private static class FailTransformationService implements TransformationService {

        @Override
        public @Nullable String transform(String s, String s1) throws TransformationException {
            return null;
        }
    }

    private static class AppendTransformationService implements TransformationService {

        @Override
        public @Nullable String transform(String s, String s1) throws TransformationException {
            return s1 + s;
        }
    }

    private static class DuplicateTransformationService implements TransformationService {

        @Override
        public @Nullable String transform(String s, String s1) throws TransformationException {
            return s1 + s1;
        }
    }
}
