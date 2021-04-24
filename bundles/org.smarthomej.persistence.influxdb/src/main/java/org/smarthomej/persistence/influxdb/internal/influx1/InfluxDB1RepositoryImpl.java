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

import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.COLUMN_TIME_NAME_V1;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.COLUMN_VALUE_NAME_V1;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.FIELD_VALUE_NAME;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.TAG_ITEM_NAME;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.persistence.influxdb.InfluxDBPersistenceService;
import org.smarthomej.persistence.influxdb.internal.FilterCriteriaQueryCreator;
import org.smarthomej.persistence.influxdb.internal.InfluxDBConfiguration;
import org.smarthomej.persistence.influxdb.internal.InfluxDBMetadataService;
import org.smarthomej.persistence.influxdb.internal.InfluxDBRepository;
import org.smarthomej.persistence.influxdb.internal.InfluxPoint;
import org.smarthomej.persistence.influxdb.internal.InfluxRow;
import org.smarthomej.persistence.influxdb.internal.UnexpectedConditionException;

/**
 * Implementation of {@link InfluxDBRepository} for InfluxDB 1.0
 *
 * @author Joan Pujol Espinar - Initial contribution. Most code has been moved from
 *         {@link InfluxDBPersistenceService} where it was in previous version
 */
@NonNullByDefault
public class InfluxDB1RepositoryImpl implements InfluxDBRepository {
    private final Logger logger = LoggerFactory.getLogger(InfluxDB1RepositoryImpl.class);
    private final InfluxDBConfiguration configuration;
    private final InfluxDBMetadataService influxDBMetadataService;
    private @Nullable InfluxDB client;

    public InfluxDB1RepositoryImpl(InfluxDBConfiguration configuration,
            InfluxDBMetadataService influxDBMetadataService) {
        this.configuration = configuration;
        this.influxDBMetadataService = influxDBMetadataService;
    }

    @Override
    public boolean isConnected() {
        return client != null;
    }

    @Override
    public boolean connect() {
        final InfluxDB createdClient = InfluxDBFactory.connect(configuration.getUrl(), configuration.getUser(),
                configuration.getPassword());
        createdClient.setDatabase(configuration.getDatabaseName());
        createdClient.setRetentionPolicy(configuration.getRetentionPolicy());
        createdClient.enableBatch(200, 100, TimeUnit.MILLISECONDS);
        this.client = createdClient;
        return checkConnectionStatus();
    }

    @Override
    public void disconnect() {
        this.client = null;
    }

    @Override
    public boolean checkConnectionStatus() {
        boolean dbStatus = false;
        final InfluxDB currentClient = client;
        if (currentClient != null) {
            try {
                Pong pong = currentClient.ping();
                String version = pong.getVersion();
                // may be check for version >= 0.9
                if (version != null && !version.contains("unknown")) {
                    dbStatus = true;
                    logger.debug("database status is OK, version is {}", version);
                } else {
                    logger.warn("database ping error, version is: \"{}\" response time was \"{}\"", version,
                            pong.getResponseTime());
                    dbStatus = false;
                }
            } catch (RuntimeException e) {
                dbStatus = false;
                logger.error("database connection failed", e);
                handleDatabaseException(e);
            }
        } else {
            logger.warn("checkConnection: database is not connected");
        }
        return dbStatus;
    }

    private void handleDatabaseException(Exception e) {
        logger.warn("database error: {}", e.getMessage(), e);
    }

    @Override
    public void write(InfluxPoint point) throws UnexpectedConditionException {
        final InfluxDB currentClient = this.client;
        if (currentClient != null) {
            Point clientPoint = convertPointToClientFormat(point);
            currentClient.write(configuration.getDatabaseName(), configuration.getRetentionPolicy(), clientPoint);
        } else {
            logger.warn("Write point {} ignored due to client isn't connected", point);
        }
    }

    private Point convertPointToClientFormat(InfluxPoint point) throws UnexpectedConditionException {
        Point.Builder clientPoint = Point.measurement(point.getMeasurementName()).time(point.getTime().toEpochMilli(),
                TimeUnit.MILLISECONDS);
        setPointValue(point.getValue(), clientPoint);
        point.getTags().forEach(clientPoint::tag);
        return clientPoint.build();
    }

    private void setPointValue(@Nullable Object value, Point.Builder point) throws UnexpectedConditionException {
        if (value instanceof String) {
            point.addField(FIELD_VALUE_NAME, (String) value);
        } else if (value instanceof Number) {
            point.addField(FIELD_VALUE_NAME, (Number) value);
        } else if (value instanceof Boolean) {
            point.addField(FIELD_VALUE_NAME, (Boolean) value);
        } else if (value == null) {
            point.addField(FIELD_VALUE_NAME, "null");
        } else {
            throw new UnexpectedConditionException("Not expected value type");
        }
    }

    @Override
    public List<InfluxRow> query(String query) {
        final InfluxDB currentClient = client;
        if (currentClient != null) {
            Query parsedQuery = new Query(query, configuration.getDatabaseName());
            List<QueryResult.Result> results = currentClient.query(parsedQuery, TimeUnit.MILLISECONDS).getResults();
            return convertClientResultToRepository(results);
        } else {
            logger.warn("Returning empty list because queryAPI isn't present");
            return List.of();
        }
    }

    private List<InfluxRow> convertClientResultToRepository(List<QueryResult.Result> results) {
        List<InfluxRow> rows = new ArrayList<>();
        for (QueryResult.Result result : results) {
            List<QueryResult.Series> allSeries = result.getSeries();
            if (result.getError() != null) {
                logger.warn("{}", result.getError());
                continue;
            }
            if (allSeries == null) {
                logger.debug("query returned no series");
            } else {
                for (QueryResult.Series series : allSeries) {
                    logger.trace("series {}", series);
                    String defaultItemName = series.getName();
                    List<List<Object>> allValues = series.getValues();
                    if (allValues == null) {
                        logger.debug("query returned no values");
                    } else {
                        List<String> columns = series.getColumns();
                        logger.trace("columns {}", columns);
                        if (columns != null) {
                            int timestampColumn = columns.indexOf(COLUMN_TIME_NAME_V1);
                            int valueColumn = columns.indexOf(COLUMN_VALUE_NAME_V1);
                            int itemNameColumn = columns.indexOf(TAG_ITEM_NAME);
                            if (valueColumn == -1 || timestampColumn == -1) {
                                throw new IllegalStateException("missing column");
                            }
                            for (List<Object> valueObject : allValues) {
                                Double rawTime = (Double) valueObject.get(timestampColumn);
                                Instant time = Instant.ofEpochMilli(rawTime.longValue());
                                Object value = valueObject.get(valueColumn);
                                String itemName = itemNameColumn == -1 ? defaultItemName
                                        : Objects.requireNonNullElse((String) valueObject.get(itemNameColumn),
                                                defaultItemName);
                                logger.trace("adding historic item {}: time {} value {}", itemName, time, value);
                                rows.add(new InfluxRow(time, itemName, value));
                            }
                        }
                    }
                }
            }
        }
        return rows;
    }

    @Override
    public Map<String, Integer> getStoredItemsCount() {
        return Collections.emptyMap();
    }

    @Override
    public FilterCriteriaQueryCreator createQueryCreator() {
        return new InfluxDB1FilterCriteriaQueryCreatorImpl(configuration, influxDBMetadataService);
    }
}
