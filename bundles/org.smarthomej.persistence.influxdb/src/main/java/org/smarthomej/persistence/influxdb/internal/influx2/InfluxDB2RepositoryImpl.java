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

import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.COLUMN_TIME_NAME_V2;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.COLUMN_VALUE_NAME_V2;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.FIELD_VALUE_NAME;
import static org.smarthomej.persistence.influxdb.internal.InfluxDBConstants.TAG_ITEM_NAME;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.persistence.influxdb.internal.FilterCriteriaQueryCreator;
import org.smarthomej.persistence.influxdb.internal.InfluxDBConfiguration;
import org.smarthomej.persistence.influxdb.internal.InfluxDBConstants;
import org.smarthomej.persistence.influxdb.internal.InfluxDBMetadataService;
import org.smarthomej.persistence.influxdb.internal.InfluxDBRepository;
import org.smarthomej.persistence.influxdb.internal.InfluxPoint;
import org.smarthomej.persistence.influxdb.internal.InfluxRow;
import org.smarthomej.persistence.influxdb.internal.UnexpectedConditionException;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.Ready;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;

/**
 * Implementation of {@link InfluxDBRepository} for InfluxDB 2.0
 *
 * @author Joan Pujol Espinar - Initial contribution
 */
@NonNullByDefault
public class InfluxDB2RepositoryImpl implements InfluxDBRepository {
    private final Logger logger = LoggerFactory.getLogger(InfluxDB2RepositoryImpl.class);
    private final InfluxDBConfiguration configuration;
    private final InfluxDBMetadataService influxDBMetadataService;

    private @Nullable InfluxDBClient client;
    private @Nullable QueryApi queryAPI;
    private @Nullable WriteApi writeAPI;

    public InfluxDB2RepositoryImpl(InfluxDBConfiguration configuration,
            InfluxDBMetadataService influxDBMetadataService) {
        this.configuration = configuration;
        this.influxDBMetadataService = influxDBMetadataService;
    }

    /**
     * Returns if the client has been successfully connected to server
     *
     * @return True if it's connected, otherwise false
     */
    @Override
    public boolean isConnected() {
        return client != null;
    }

    /**
     * Connect to InfluxDB server
     *
     * @return True if successful, otherwise false
     */
    @Override
    public boolean connect() {
        InfluxDBClientOptions.Builder optionsBuilder = InfluxDBClientOptions.builder().url(configuration.getUrl())
                .org(configuration.getDatabaseName()).bucket(configuration.getRetentionPolicy());
        char[] token = configuration.getTokenAsCharArray();
        if (token.length > 0) {
            optionsBuilder.authenticateToken(token);
        } else {
            optionsBuilder.authenticate(configuration.getUser(), configuration.getPassword().toCharArray());
        }
        InfluxDBClientOptions clientOptions = optionsBuilder.build();

        final InfluxDBClient createdClient = InfluxDBClientFactory.create(clientOptions);
        this.client = createdClient;

        queryAPI = createdClient.getQueryApi();
        writeAPI = createdClient.getWriteApi();
        logger.debug("Successfully connected to InfluxDB. Instance ready={}", createdClient.ready());

        return checkConnectionStatus();
    }

    /**
     * Disconnect from InfluxDB server
     */
    @Override
    public void disconnect() {
        final InfluxDBClient currentClient = this.client;
        if (currentClient != null) {
            currentClient.close();
        }
        this.client = null;
    }

    /**
     * Check if connection is currently ready
     *
     * @return True if its ready, otherwise false
     */
    @Override
    public boolean checkConnectionStatus() {
        final InfluxDBClient currentClient = client;
        if (currentClient != null) {
            Ready ready = currentClient.ready();
            boolean isUp = ready != null && ready.getStatus() == Ready.StatusEnum.READY;
            if (isUp) {
                logger.debug("database status is OK");
            } else {
                logger.warn("database not ready");
            }
            return isUp;
        } else {
            logger.warn("checkConnection: database is not connected");
            return false;
        }
    }

    @Override
    public void write(InfluxPoint point) throws UnexpectedConditionException {
        final WriteApi currentWriteAPI = writeAPI;
        if (currentWriteAPI != null) {
            currentWriteAPI.writePoint(convertPointToClientFormat(point));
        } else {
            logger.warn("Write point {} ignored due to writeAPI isn't present", point);
        }
    }

    private Point convertPointToClientFormat(InfluxPoint point) throws UnexpectedConditionException {
        Point clientPoint = Point.measurement(point.getMeasurementName()).time(point.getTime(), WritePrecision.MS);
        setPointValue(point.getValue(), clientPoint);
        point.getTags().forEach(clientPoint::addTag);
        return clientPoint;
    }

    private void setPointValue(@Nullable Object value, Point point) throws UnexpectedConditionException {
        if (value instanceof String) {
            point.addField(FIELD_VALUE_NAME, (String) value);
        } else if (value instanceof Number) {
            point.addField(FIELD_VALUE_NAME, (Number) value);
        } else if (value instanceof Boolean) {
            point.addField(FIELD_VALUE_NAME, (Boolean) value);
        } else if (value == null) {
            point.addField(FIELD_VALUE_NAME, (String) null);
        } else {
            throw new UnexpectedConditionException("Not expected value type");
        }
    }

    /**
     * Executes Flux query
     *
     * @param query Query
     * @return Query results
     */
    @Override
    public List<InfluxRow> query(String query) {
        final QueryApi currentQueryAPI = queryAPI;
        if (currentQueryAPI != null) {
            List<FluxTable> clientResult = currentQueryAPI.query(query);
            return convertClientResutToRepository(clientResult);
        } else {
            logger.warn("Returning empty list because queryAPI isn't present");
            return Collections.emptyList();
        }
    }

    private List<InfluxRow> convertClientResutToRepository(List<FluxTable> clientResult) {
        return clientResult.stream().flatMap(this::mapRawResultToHistoric).collect(Collectors.toList());
    }

    private Stream<InfluxRow> mapRawResultToHistoric(FluxTable rawRow) {
        return rawRow.getRecords().stream().map(r -> {
            String itemName = (String) r.getValueByKey(InfluxDBConstants.TAG_ITEM_NAME);
            Object value = r.getValueByKey(COLUMN_VALUE_NAME_V2);
            Instant time = (Instant) r.getValueByKey(COLUMN_TIME_NAME_V2);
            return new InfluxRow(time, itemName, value);
        });
    }

    /**
     * Return all stored item names with it's count of stored points
     *
     * @return Map with <ItemName,ItemCount> entries
     */
    @Override
    public Map<String, Integer> getStoredItemsCount() {
        final QueryApi currentQueryAPI = queryAPI;

        if (currentQueryAPI != null) {
            Map<String, Integer> result = new LinkedHashMap<>();
            // Query wrote by hand https://github.com/influxdata/influxdb-client-java/issues/75
            String query = "from(bucket: \"" + configuration.getRetentionPolicy() + "\")\n"
                    + "  |> range(start:-365d)\n" + "  |> filter(fn: (r) => exists r." + TAG_ITEM_NAME + " )\n"
                    + "  |> group(columns: [\"" + TAG_ITEM_NAME + "\"], mode:\"by\")\n" + "  |> count()\n"
                    + "  |> group()";

            List<FluxTable> queryResult = currentQueryAPI.query(query);
            Objects.requireNonNull(queryResult.stream().findFirst().orElse(new FluxTable())).getRecords()
                    .forEach(row -> {
                        result.put((String) row.getValueByKey(TAG_ITEM_NAME), ((Number) row.getValue()).intValue());
                    });
            return result;
        } else {
            logger.warn("Returning empty result  because queryAPI isn't present");
            return Collections.emptyMap();
        }
    }

    @Override
    public FilterCriteriaQueryCreator createQueryCreator() {
        return new InfluxDB2FilterCriteriaQueryCreatorImpl(configuration, influxDBMetadataService);
    }
}
