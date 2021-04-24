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
package org.smarthomej.persistence.influxdb.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.persistence.FilterCriteria;

/**
 * Creates InfluxDB query sentence given a OpenHab persistence {@link FilterCriteria}
 *
 * @author Joan Pujol Espinar - Initial contribution
 */
@NonNullByDefault
public interface FilterCriteriaQueryCreator {
    /**
     * Create query from {@link FilterCriteria}
     * 
     * @param criteria Criteria to create query from
     * @param retentionPolicy Name of the retentionPolicy/bucket to use in query
     * @return Created query as an String
     */
    String createQuery(FilterCriteria criteria, String retentionPolicy) throws UnexpectedConditionException;

    default String getOperationSymbol(FilterCriteria.Operator operator, InfluxDBVersion version)
            throws UnexpectedConditionException {
        switch (operator) {
            case EQ:
                return "=";
            case LT:
                return "<";
            case LTE:
                return "<=";
            case GT:
                return ">";
            case GTE:
                return ">=";
            case NEQ:
                return version == InfluxDBVersion.V1 ? "<>" : "!=";
            default:
                throw new UnexpectedConditionException("Not expected operator " + operator);
        }
    }
}
