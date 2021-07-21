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
package org.smarthomej.automation.javarule.internal;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.tools.SimpleJavaFileObject;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link JavaRuleSimpleJavaFileObject} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JavaRuleSimpleJavaFileObject extends SimpleJavaFileObject {
    /**
     * Construct a SimpleJavaFileObject of the given kind and with the
     * given URI.
     *
     * @param uri the URI for this file object
     * @param kind the kind of this file object
     */
    public JavaRuleSimpleJavaFileObject(URI uri, Kind kind) {
        super(uri, kind);
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return uri.toURL().openStream();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return new FileOutputStream(Path.of(uri).toFile());
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            result.write(openInputStream().readAllBytes());
            return result.toString(StandardCharsets.UTF_8);
        }
    }
}
