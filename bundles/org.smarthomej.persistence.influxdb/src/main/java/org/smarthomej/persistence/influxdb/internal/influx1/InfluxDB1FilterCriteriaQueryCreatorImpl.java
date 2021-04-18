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
package org.smarthomej.persistence.influxdb.internal.influx1;

import static org.influxdb.querybuilder.BuiltQuery.QueryBuilder.asc;
import static org.influxdb.querybuilder.BuiltQuery.QueryBuilder.desc;
import static org.influxdb.querybuilder.BuiltQuery.QueryBuilder.select;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.COLUMN_TIME_NAME_V1;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.COLUMN_VALUE_NAME_V1;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.TAG_ITEM_NAME;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBStateConvertUtils.stateToObject;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.influxdb.dto.Query;
import org.influxdb.querybuilder.Appender;
import org.influxdb.querybuilder.BuiltQuery;
import org.influxdb.querybuilder.Select;
import org.influxdb.querybuilder.Where;
import org.influxdb.querybuilder.clauses.SimpleClause;
import org.openhab.core.persistence.FilterCriteria;
import org.smarthomej.persistence.influxdb.internal.FilterCriteriaQueryCreator;
import org.smarthomej.persistence.influxdb.internal.InfluxDBConfiguration;
import org.smarthomej.persistence.influxdb.internal.InfluxDBMetadataService;
import org.smarthomej.persistence.influxdb.internal.InfluxDBVersion;
import org.smarthomej.persistence.influxdb.internal.UnexpectedConditionException;

/**
 * Implementation of {@link FilterCriteriaQueryCreator} for InfluxDB 1.0
 *
 * @author Joan Pujol Espinar - Initial contribution
 */
@NonNullByDefault
public class InfluxDB1FilterCriteriaQueryCreatorImpl implements FilterCriteriaQueryCreator {
    private final InfluxDBConfiguration configuration;
    private final InfluxDBMetadataService influxDBMetadataService;

    public InfluxDB1FilterCriteriaQueryCreatorImpl(InfluxDBConfiguration configuration,
            InfluxDBMetadataService influxDBMetadataService) {
        this.configuration = configuration;
        this.influxDBMetadataService = influxDBMetadataService;
    }

    @Override
    public String createQuery(FilterCriteria criteria, String retentionPolicy) throws UnexpectedConditionException {
        final String itemName = criteria.getItemName();
        final String tableName = getTableName(itemName);
        final boolean hasCriteriaName = itemName != null;

        Select select = select().column("\"" + COLUMN_VALUE_NAME_V1 + "\"::field")
                .column("\"" + TAG_ITEM_NAME + "\"::tag")
                .fromRaw(null, fullQualifiedTableName(retentionPolicy, tableName, hasCriteriaName));

        Where where = select.where();

        if (itemName != null && !tableName.equals(itemName)) {
            where.and(BuiltQuery.QueryBuilder.eq(TAG_ITEM_NAME, itemName));
        }
        if (criteria.getBeginDate() != null) {
            where.and(BuiltQuery.QueryBuilder.gte(COLUMN_TIME_NAME_V1, criteria.getBeginDate().toInstant().toString()));
        }
        if (criteria.getEndDate() != null) {
            where.and(BuiltQuery.QueryBuilder.lte(COLUMN_TIME_NAME_V1, criteria.getEndDate().toInstant().toString()));
        }

        if (criteria.getState() != null && criteria.getOperator() != null) {
            where.and(new SimpleClause(COLUMN_VALUE_NAME_V1,
                    getOperationSymbol(criteria.getOperator(), InfluxDBVersion.V1),
                    stateToObject(criteria.getState())));
        }

        if (criteria.getOrdering() == FilterCriteria.Ordering.DESCENDING) {
            select = select.orderBy(desc());
        } else if (criteria.getOrdering() == FilterCriteria.Ordering.ASCENDING) {
            select = select.orderBy(asc());
        }

        if (criteria.getPageSize() != Integer.MAX_VALUE) {
            if (criteria.getPageNumber() != 0) {
                select = select.limit(criteria.getPageSize(), criteria.getPageSize() * criteria.getPageNumber());
            } else {
                select = select.limit(criteria.getPageSize());
            }
        }

        return ((Query) select).getCommand();
    }

    private String getTableName(@Nullable String itemName) {
        if (itemName == null) {
            return "/.*/";
        }

        String name = influxDBMetadataService.getMeasurementNameOrDefault(itemName, itemName);

        if (configuration.isReplaceUnderscore()) {
            name = name.replace('_', '.');
        }

        return name;
    }

    private String fullQualifiedTableName(String retentionPolicy, String tableName, boolean escapeTableName) {
        StringBuilder sb = new StringBuilder();
        Appender.appendName(retentionPolicy, sb);
        sb.append(".");
        if (escapeTableName) {
            Appender.appendName(tableName, sb);
        } else {
            sb.append(tableName);
        }
        return sb.toString();
    }
}
