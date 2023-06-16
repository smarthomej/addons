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
package org.smarthomej.binding.tuya.internal.schema;

import java.lang.reflect.Type;
import java.util.List;

import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smarthomej.binding.tuya.internal.util.SchemaDp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Registry for device channels
 *
 * @author Vitalii Herhel - Initial contribution
 */
@Component(service = SchemaRegistry.class, configurationPid = "tuya.schemaService")
public class SchemaRegistry {

    private static final Type STORAGE_TYPE = TypeToken.getParameterized(List.class, SchemaDp.class).getType();

    private final Gson gson = new Gson();
    private final Storage<String> storage;

    @Activate
    public SchemaRegistry(@Reference StorageService storageService) {
        this.storage = storageService.getStorage("org.smarthomej.binding.tuya.Schema");
    }

    public void add(String id, List<SchemaDp> schema) {
        storage.put(id, gson.toJson(schema));
    }

    public List<SchemaDp> get(String id) {
        return gson.fromJson(storage.get(id), STORAGE_TYPE);
    }
}
