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
package org.smarthomej.transform.math.internal;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for {@link TransformationService}s which applies simple math on the input.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
abstract class AbstractMathTransformationService implements TransformationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public @Nullable String transform(String valueString, String sourceString) throws TransformationException {
        if (UnDefType.NULL.toString().equals(sourceString) || UnDefType.UNDEF.toString().equals(sourceString)) {
            // UNDEF and NULL are not transformed but should not throw errors
            return sourceString;
        }

        QuantityType<?> source;
        try {
            source = new QuantityType<>(sourceString);
        } catch (IllegalArgumentException e) {
            logger.warn("Input value '{}' could not be converted to a valid number", sourceString);
            throw new TransformationException("Math Transformation can only be used with numeric inputs");
        }
        QuantityType<?> value;
        try {
            value = new QuantityType<>(valueString);
        } catch (IllegalArgumentException e) {
            logger.warn("Input value '{}' could not be converted to a valid number", valueString);
            throw new TransformationException("Math Transformation can only be used with numeric inputs");
        }
        try {
            QuantityType<?> result = performCalculation(source, value);
            return BigDecimal.ZERO.compareTo(result.toBigDecimal()) == 0 ? "0" : result.toString();
        } catch (IllegalArgumentException e) {
            throw new TransformationException("ArithmeticException: " + e.getMessage());
        }
    }

    /**
     * Perform the mathematical calculation.
     *
     * @param source the source
     * @param value the value
     * @return the result of the mathematical calculation
     * @throws IllegalArgumentException in case of invalid inputs for calculations
     */
    abstract QuantityType<?> performCalculation(QuantityType<?> source, QuantityType<?> value);
}
