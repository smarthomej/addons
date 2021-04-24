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

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Manages InfluxDB server interaction maintaining client connection
 *
 * @author Joan Pujol Espinar - Initial contribution
 */
@NonNullByDefault
public interface InfluxDBRepository {
    /**
     * Returns if the client is successfully connected to server
     *
     * @return True if it's connected, otherwise false
     */
    boolean isConnected();

    /**
     * connect to InfluxDB server
     *
     */
    boolean connect();

    /**
     * Disconnect from InfluxDB server
     */
    void disconnect();

    /**
     * Check if connection is currently ready
     *
     * @return True if its ready, otherwise false
     */
    boolean checkConnectionStatus();

    /**
     * Return all stored item names with it's count of stored points
     *
     * @return Map with <ItemName,ItemCount> entries
     */
    Map<String, Integer> getStoredItemsCount();

    /**
     * Executes Flux query
     *
     * @param query Query
     * @return Query results
     */
    List<InfluxRow> query(String query);

    /**
     * Write point to database
     *
     * @param influxPoint Point to write
     */
    void write(InfluxPoint influxPoint) throws UnexpectedConditionException;

    /**
     * create a query creator on this repository
     *
     * @return the query creator for this repository
     */
    FilterCriteriaQueryCreator createQueryCreator();
}
