/**
 * Copyright (c) 2021-2022 Contributors to the SmartHome/J project
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
package org.smarthomej.binding.amazonechocontrol.internal;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.Predicate;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * The {@link JsonDocument} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JsonDocument {
    private static final JsonProvider JSON_PROVIDER = new GsonJsonProvider();
    private static final MappingProvider MAPPING_PROVIDER = new GsonMappingProvider();
    private static final Configuration JSONPATH_VALUE_CONFIGURATION = Configuration.builder()
            .jsonProvider(JSON_PROVIDER).mappingProvider(MAPPING_PROVIDER)
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS).build();
    private static final Configuration JSONPATH_PATH_CONFIGURATION = Configuration.builder().jsonProvider(JSON_PROVIDER)
            .mappingProvider(MAPPING_PROVIDER)
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS, Option.AS_PATH_LIST).build();

    // @formatter:off
    private static final TypeRef<List<String>> STRING_LIST = new TypeRef<>() {};
    private static final Map<Class<?>, TypeRef<?>> LIST_TYPE_REFS = Map.ofEntries(
            Map.entry(Boolean.class, new TypeRef<List<Boolean>>() {}),
            Map.entry(Integer.class, new TypeRef<List<Integer>>() {}),
            Map.entry(String.class, STRING_LIST));
    // @formatter:on

    public static final JsonDocument EMPTY = new JsonDocument("{}");
    private final Object parsedJson;

    public JsonDocument(String json) {
        parsedJson = JSON_PROVIDER.parse(json);
    }

    public <T> @Nullable T get(String path, TypeRef<T> typeRef, Predicate... filters) {
        JsonElement object = JsonPath.compile(path, filters).read(parsedJson, JSONPATH_VALUE_CONFIGURATION);
        return MAPPING_PROVIDER.map(object, typeRef, JSONPATH_VALUE_CONFIGURATION);
    }

    public <T> @Nullable T get(String path, Class<T> clazz, Predicate... filters) {
        JsonElement object = JsonPath.compile(path, filters).read(parsedJson, JSONPATH_VALUE_CONFIGURATION);
        return MAPPING_PROVIDER.map(object, clazz, JSONPATH_VALUE_CONFIGURATION);
    }

    /**
     *
     * @param path
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable List<T> getList(String path, Class<T> clazz, Predicate... filters) {
        TypeRef<?> typeRef = LIST_TYPE_REFS.get(clazz);
        if (typeRef == null) {
            return null;
        }
        return (List<T>) get(path, typeRef, filters);
    }

    /**
     *
     * @param path
     * @param clazz
     * @return
     */
    public <T> @Nullable T getFirst(String path, Class<T> clazz, Predicate... filters) {
        List<T> list = getList(path, clazz, filters);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    public List<String> getPaths(String path, Predicate... filters) {
        JsonArray paths = JsonPath.compile(path, filters).read(parsedJson, JSONPATH_PATH_CONFIGURATION);
        return MAPPING_PROVIDER.map(paths, STRING_LIST, JSONPATH_PATH_CONFIGURATION);
    }

    public @Nullable String getFirstPath(String path, Predicate... filters) {
        List<String> paths = getPaths(path, filters);
        return paths.isEmpty() ? null : paths.get(0);
    }
}
