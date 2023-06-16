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

import java.util.ArrayList;
import java.util.List;

import org.smarthomej.binding.tuya.internal.cloud.dto.DeviceSchema;
import org.smarthomej.binding.tuya.internal.util.SchemaDp;

import com.google.gson.Gson;

/**
 * Maps {@link DeviceSchema} to {@link List<SchemaDp>}
 *
 * @author Vitalii Herhel - Initial contribution
 */
public class DeviceSchemaMapper {

    private final Gson gson;

    public DeviceSchemaMapper(Gson gson) {
        this.gson = gson;
    }

    public List<SchemaDp> covert(DeviceSchema schema) {
        List<SchemaDp> schemaDps = new ArrayList<>();
        schema.functions.forEach(description -> addUniqueSchemaDp(description, schemaDps));
        schema.status.forEach(description -> addUniqueSchemaDp(description, schemaDps));
        return schemaDps;
    }

    private void addUniqueSchemaDp(DeviceSchema.Description description, List<SchemaDp> schemaDps) {
        if (description.dp_id == 0 || schemaDps.stream().anyMatch(schemaDp -> schemaDp.id == description.dp_id)) {
            // dp is missing or already present, skip it
            return;
        }
        // some devices report the same function code for different dps
        // we add an index only if this is the case
        String originalCode = description.code;
        int index = 1;
        while (schemaDps.stream().anyMatch(schemaDp -> schemaDp.code.equals(description.code))) {
            description.code = originalCode + "_" + index;
        }

        schemaDps.add(SchemaDp.fromRemoteSchema(gson, description));
    }
}
