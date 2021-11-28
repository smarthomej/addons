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
package org.smarthomej.transform.math.internal;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
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

    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*?(-?((0)|([1-9][0-9]*))(\\.[0-9]*)?).*?");

    @Override
    public @Nullable String transform(String valueString, String sourceString) throws TransformationException {
        BigDecimal source;
        String extractedNumericString = extractNumericString(sourceString);
        try {
            source = new BigDecimal(extractedNumericString);
        } catch (NumberFormatException e) {
            logger.warn("Input value '{}' could not converted to a valid number", extractedNumericString);
            throw new TransformationException("Math Transformation can only be used with numeric inputs");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(extractNumericString(valueString));
        } catch (NumberFormatException e) {
            logger.warn("Input value '{}' could not converted to a valid number", extractNumericString(valueString));
            throw new TransformationException("Math Transformation can only be used with numeric inputs");
        }
        try {
            String result = performCalculation(source, value).toString();
            return sourceString.replace(extractedNumericString, result);
        } catch (ArithmeticException e) {
            throw new TransformationException("ArithmeticException: " + e.getMessage());
        }
    }

    private String extractNumericString(String sourceString) throws TransformationException {
        Matcher matcher = NUMBER_PATTERN.matcher(sourceString);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new TransformationException("Math Transformation can only be used with numeric inputs");
        }
    }

    abstract BigDecimal performCalculation(BigDecimal source, BigDecimal value);
}
