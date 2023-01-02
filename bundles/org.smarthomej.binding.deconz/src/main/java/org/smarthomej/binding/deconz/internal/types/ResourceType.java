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
package org.smarthomej.binding.deconz.internal.types;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.deconz.internal.dto.DeconzBaseMessage;
import org.smarthomej.binding.deconz.internal.dto.GroupMessage;
import org.smarthomej.binding.deconz.internal.dto.LightMessage;
import org.smarthomej.binding.deconz.internal.dto.SensorMessage;

/**
 * The {@link ResourceType} defines an enum for websocket messages
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public enum ResourceType {
    GROUPS("groups", "action", GroupMessage.class),
    LIGHTS("lights", "state", LightMessage.class),
    SENSORS("sensors", "config", SensorMessage.class),
    SCENES("scenes", "", DeconzBaseMessage.class),
    UNKNOWN("", "", null);

    private static final Map<String, ResourceType> MAPPING = Arrays.stream(ResourceType.values())
            .collect(Collectors.toMap(v -> v.identifier, v -> v));
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceType.class);

    private String identifier;
    private String commandUrl;
    private @Nullable Class<? extends DeconzBaseMessage> expectedMessageType;

    ResourceType(String identifier, String commandUrl,
            @Nullable Class<? extends DeconzBaseMessage> expectedMessageType) {
        this.identifier = identifier;
        this.commandUrl = commandUrl;
        this.expectedMessageType = expectedMessageType;
    }

    /**
     * get the identifier string of this resource type
     *
     * @return
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * get the commandUrl part for this resource type
     *
     * @return
     */
    public String getCommandUrl() {
        return commandUrl;
    }

    /**
     * get the expected message type for this resource type
     *
     * @return
     */
    public @Nullable Class<? extends DeconzBaseMessage> getExpectedMessageType() {
        return expectedMessageType;
    }

    /**
     * get the resource type from a string
     *
     * @param s the string
     * @return the corresponding resource type (or UNKNOWN)
     */
    public static ResourceType fromString(String s) {
        ResourceType lightType = MAPPING.getOrDefault(s, UNKNOWN);
        if (lightType == UNKNOWN) {
            LOGGER.debug("Unknown resource type '{}' found. This should be reported.", s);
        }
        return lightType;
    }
}
