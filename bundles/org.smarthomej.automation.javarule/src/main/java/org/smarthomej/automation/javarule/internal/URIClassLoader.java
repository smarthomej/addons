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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link URIClassLoader} is a classloader that handles URIs
 *
 * @author Jan N. Klug - Initial contribution
 */
public class URIClassLoader extends URLClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(URIClassLoader.class);

    public URIClassLoader(List<URI> uris, @Nullable ClassLoader parent) {
        super(uris.stream().map(uri -> {
            try {
                return uri.toURL();
            } catch (MalformedURLException e) {
                logger.warn("Skipping '{}', failed to convert URI to URL: {}", uri, e.getMessage());
                return null;
            }
        }).filter(Objects::nonNull).map(Objects::requireNonNull).toArray(URL[]::new), parent);
    }
}
