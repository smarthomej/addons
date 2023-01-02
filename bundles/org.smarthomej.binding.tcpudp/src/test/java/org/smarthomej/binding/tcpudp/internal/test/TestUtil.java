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
package org.smarthomej.binding.tcpudp.internal.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openhab.core.config.core.Configuration;

/**
 * The {@link TestUtil} includes some helper methods for testing
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TestUtil {
    public static Configuration getConfigurationFromInstance(Object o) {
        List<Field> fields = getAllFields(o.getClass());

        Map<String, Object> configMap = new HashMap<>();
        fields.forEach(f -> {
            try {
                Object value = f.get(o);
                if (value != null) {
                    configMap.put(f.getName(), value.toString());
                }
            } catch (IllegalAccessException e) {
            }
        });

        return new Configuration(configMap);
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }

        fields = fields.stream().filter(ReflectionUtils::isPublic).collect(Collectors.toList());
        return fields;
    }
}
