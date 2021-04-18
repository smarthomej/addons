/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.persistence.influxdb.internal.influx2;

import static com.influxdb.query.dsl.functions.restriction.Restrictions.measurement;
import static com.influxdb.query.dsl.functions.restriction.Restrictions.tag;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.COLUMN_TIME_NAME_V2;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.FIELD_VALUE_NAME;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBStateConvertUtils.stateToObject;

import java.time.temporal.ChronoUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.persistence.FilterCriteria;
import org.smarthomej.persistence.influxdb.internal.FilterCriteriaQueryCreator;
import org.smarthomej.persistence.influxdb.internal.InfluxDBConfiguration;
import org.smarthomej.persistence.influxdb.internal.InfluxDBMetadataService;
import org.smarthomej.persistence.influxdb.internal.InfluxDBVersion;
import org.smarthomej.persistence.influxdb.internal.UnexpectedConditionException;

import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.RangeFlux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;

/**
 * Implementation of {@link FilterCriteriaQueryCreator} for InfluxDB 2.0
 *
 * @author Joan Pujol Espinar - Initial contribution
 */
@NonNullByDefault
public class InfluxDB2FilterCriteriaQueryCreatorImpl implements FilterCriteriaQueryCreator {
    private final InfluxDBConfiguration configuration;
    private final InfluxDBMetadataService influxDBMetadataService;

    public InfluxDB2FilterCriteriaQueryCreatorImpl(InfluxDBConfiguration configuration,
            InfluxDBMetadataService influxDBMetadataService) {
        this.configuration = configuration;
        this.influxDBMetadataService = influxDBMetadataService;
    }

    @Override
    public String createQuery(FilterCriteria criteria, String retentionPolicy) throws UnexpectedConditionException {
        Flux flux = Flux.from(retentionPolicy);

        RangeFlux range = flux.range();
        if (criteria.getBeginDate() != null) {
            range = range.withStart(criteria.getBeginDate().toInstant());
        } else {
            range = flux.range(-100L, ChronoUnit.YEARS); // Flux needs a mandatory start range
        }
        if (criteria.getEndDate() != null) {
            range = range.withStop(criteria.getEndDate().toInstant());
        }
        flux = range;

        String itemName = criteria.getItemName();
        if (itemName != null) {
            String measurementName = getMeasurementName(itemName);
            flux = flux.filter(measurement().equal(measurementName));
            if (!measurementName.equals(itemName)) {
                flux = flux.filter(tag("item").equal(itemName));
            }
        }

        if (criteria.getState() != null && criteria.getOperator() != null) {
            Restrictions restrictions = Restrictions.and(Restrictions.field().equal(FIELD_VALUE_NAME),
                    Restrictions.value().custom(stateToObject(criteria.getState()),
                            getOperationSymbol(criteria.getOperator(), InfluxDBVersion.V2)));
            flux = flux.filter(restrictions);
        }

        if (criteria.getOrdering() != null) {
            boolean desc = criteria.getOrdering() == FilterCriteria.Ordering.DESCENDING;
            flux = flux.sort().withDesc(desc).withColumns(new String[] { COLUMN_TIME_NAME_V2 });
        }

        if (criteria.getPageSize() != Integer.MAX_VALUE) {
            flux = flux.limit(criteria.getPageSize()).withPropertyValue("offset",
                    criteria.getPageNumber() * criteria.getPageSize());
        }

        return flux.toString();
    }

    private String getMeasurementName(String itemName) {
        String name = influxDBMetadataService.getMeasurementNameOrDefault(itemName, itemName);

        if (configuration.isReplaceUnderscore()) {
            name = name.replace('_', '.');
        }

        return name;
    }
}
