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
package org.smarthomej.io.repomanager.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RepoManagerConstants} provides constants used in the bundle
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class RepoManagerConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepoManagerConstants.class);

    // feature bundle artifacts coordinates
    public static final String KARAF_FEATURE_GROUP_ID = "org.smarthomej.addons.features.karaf";
    public static final String KARAF_FEATURE_ARTIFACT_ID = "org.smarthomej.addons.features.karaf.smarthomej-addons";

    // repositories
    public static final String SNAPSHOT_REPO_ID = "@snapshots@id=smarthomej-snapshot";
    public static final String SNAPSHOT_REPO_URL = "https://oss.sonatype.org/content/repositories/snapshots";
    public static final String RELEASE_REPO_ID = "@id=smarthomej-release";
    public static final String RELEASE_REPO_URL = "https://repo1.maven.org/maven2";

    // version strings
    public static final String OPENHAB_CORE_VERSION = FrameworkUtil.getBundle(org.openhab.core.OpenHAB.class)
            .getVersion().toString();
    public static final Pattern COMPATIBLE_VERSION = getCompatibleVersion();

    /**
     * load a compatibility list from the bundle
     *
     * @return a {@link Pattern} that matches all compatible versions (match all if fails)
     */
    private static Pattern getCompatibleVersion() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            LOGGER.warn("Could not load compatibility data: resource is null.");
            return Pattern.compile(".*");
        }
        try (InputStream inputStream = classLoader.getResourceAsStream("compatibility.lst")) {
            if (inputStream == null) {
                throw new IOException("File not found");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return Objects.requireNonNull(
                        reader.lines().map(s -> s.split("=")).filter(v -> Pattern.matches(v[0], OPENHAB_CORE_VERSION))
                                .map(v -> Pattern.compile(v[1])).findAny().orElse(Pattern.compile(".*")));
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read compatibility data: {}", e.getMessage());
            return Pattern.compile(".*");
        }
    }
}
