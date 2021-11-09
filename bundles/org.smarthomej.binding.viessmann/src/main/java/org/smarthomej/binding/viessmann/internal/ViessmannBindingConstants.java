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
package org.smarthomej.binding.viessmann.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ViessmannBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class ViessmannBindingConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViessmannBindingConstants.class);

    public static final String BINDING_ID = "viessmann";
    public static final String BINDING_NAME = "Viessmann API";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");

    public static final Set<ThingTypeUID> DISCOVERABLE_DEVICE_TYPE_UIDS = Set.of(THING_TYPE_DEVICE);

    public static final String COUNT_API_CALLS = "countApiCalls";

    // References for needed API identifiers
    public static final String VIESSMANN_HOST = "api.viessmann.com";
    public static final String VIESSMANN_BASE_URL = "https://api.viessmann.com/";
    public static final String IAM_BASE_URL = "https://iam.viessmann.com/";
    public static final String VIESSMANN_AUTHORIZE_URL = IAM_BASE_URL + "idp/v2/authorize";
    public static final String VIESSMANN_TOKEN_URL = IAM_BASE_URL + "idp/v2/token";
    public static final String VIESSMANN_SCOPE = "IoT%20User%20offline_access";

    public static final int API_TIMEOUT_MS = 20000;
    public static final String PROPERTY_ID = "deviceId";

    public static final Map<String, String> FEATURES_MAP = readPropertiesFile("features.properties").entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), Map.Entry::getValue));

    public static final Map<String, String> FEATURE_DESCRIPTION_MAP = readPropertiesFile(
            "featuresDescription.properties").entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), Map.Entry::getValue));

    public static Map<String, String> readPropertiesFile(String filename) {
        InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
        if (resource == null) {
            LOGGER.warn("Could not read resource file '{}', binding will probably fail: resource is null", filename);
            return Map.of();
        }

        try {
            Properties properties = new Properties();
            properties.load(resource);
            return properties.entrySet().stream()
                    .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
        } catch (IOException e) {
            LOGGER.warn("Could not read resource file '{}', binding will probably fail: {}", filename, e.getMessage());
            return Map.of();
        }
    }
}
