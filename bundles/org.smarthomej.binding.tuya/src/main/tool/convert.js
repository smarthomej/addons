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

/**
 * Converts the device-specific data from ioBroker.tuya to a binding compatible JSON
 *
 * @author Jan N. Klug - Initial contribution
 */
const http = require('https');
const fs = require('fs');
const rewire = require('rewire');

const mapperJs = fs.createWriteStream("../../../target/mapper.js");
http.get("https://raw.githubusercontent.com/Apollon77/ioBroker.tuya/master/lib/mapper.js", function(response) {
    response.pipe(mapperJs);
    mapperJs.on('finish', () => {
        mapperJs.close();

        const knownSchemas = rewire('../../../target/mapper').__get__('knownSchemas');

        let productKey, value;
        let convertedSchemas = {};

        for (productKey in knownSchemas) {
            try {
                let schema = JSON.parse(knownSchemas[productKey].schema);
                let convertedSchema = {};
                for (value in schema) {
                    let entry = schema[value];
                    let convertedEntry;
                    if (entry.type === 'raw') {
                        convertedEntry = {id: entry.id, type: entry.type};
                    } else {
                        convertedEntry = {id: entry.id, type: entry.property.type};
                        if (convertedEntry.type === 'enum') {
                            convertedEntry['range'] = entry.property.range;
                        }
                        if (convertedEntry.type === 'value' && entry.property.min !== null && entry.property.max !== null) {
                            convertedEntry['min'] = entry.property.min;
                            convertedEntry['max'] = entry.property.max;
                        }
                    }
                    convertedSchema[entry.code] = convertedEntry;
                }
                if (Object.keys(convertedSchema).length > 0) {
                    convertedSchemas[productKey] = convertedSchema;
                }
            } catch (err) {
                console.log('Parse Error in Schema for ' + productKey + ': ' + err);
            }
        }

        fs.writeFile('../resources/schema.json', JSON.stringify(convertedSchemas, null, '\t'), (err) => {
            if (err) throw err;
        });
    });
});
