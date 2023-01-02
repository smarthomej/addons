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
package org.smarthomej.transform.format.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.transform.AbstractFileTransformationService;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * The implementation of {@link TransformationService} which formats the input
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = { TransformationService.class, ConfigOptionProvider.class }, property = {
        "openhab.transform=FORMAT" })
public class FormatTransformationService extends AbstractFileTransformationService<String>
        implements ConfigOptionProvider {

    private final Logger logger = LoggerFactory.getLogger(FormatTransformationService.class);

    private static final String PROFILE_CONFIG_URI = "profile:transform:FORMAT";
    private static final String CONFIG_PARAM_FUNCTION = "function";

    @Override
    protected String internalTransform(String formatString, String source) throws TransformationException {
        try {
            String target = String.format(formatString, source);

            logger.debug("Transformation resulted in '{}'", target);
            return target;
        } catch (IllegalFormatException e) {
            throw new TransformationException("Format failed.", e);
        }
    }

    @Override
    protected String internalLoadTransform(String filename) throws TransformationException {
        try (Scanner scanner = new Scanner(new File(filename))) {
            return scanner.useDelimiter("\\Z").next();
        } catch (IOException e) {
            throw new TransformationException("An error occurred while opening file.", e);
        }
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (PROFILE_CONFIG_URI.equals(uri.toString()) && CONFIG_PARAM_FUNCTION.equals(param)) {
            File path = new File(getSourcePath());
            return Stream.of(Objects.requireNonNullElse(path.listFiles(), new File[0])).map(File::getName)
                    .filter(f -> f.endsWith(".format")).map(f -> new ParameterOption(f, f))
                    .collect(Collectors.toList());
        }

        return null;
    }
}
