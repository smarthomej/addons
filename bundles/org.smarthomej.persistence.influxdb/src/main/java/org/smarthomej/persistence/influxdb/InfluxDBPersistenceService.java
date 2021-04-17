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
package org.smarthomej.persistence.influxdb;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceItemInfo;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.State;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.persistence.influxdb.internal.FilterCriteriaQueryCreator;
import org.smarthomej.persistence.influxdb.internal.InfluxDBConfiguration;
import org.smarthomej.persistence.influxdb.internal.InfluxDBHistoricItem;
import org.smarthomej.persistence.influxdb.internal.InfluxDBMetadataService;
import org.smarthomej.persistence.influxdb.internal.InfluxDBPersistentItemInfo;
import org.smarthomej.persistence.influxdb.internal.InfluxDBRepository;
import org.smarthomej.persistence.influxdb.internal.InfluxDBStateConvertUtils;
import org.smarthomej.persistence.influxdb.internal.InfluxPoint;
import org.smarthomej.persistence.influxdb.internal.InfluxRow;
import org.smarthomej.persistence.influxdb.internal.ItemToStorePointCreator;
import org.smarthomej.persistence.influxdb.internal.UnnexpectedConditionException;
import org.smarthomej.persistence.influxdb.internal.influx1.InfluxDB1RepositoryImpl;
import org.smarthomej.persistence.influxdb.internal.influx2.InfluxDB2RepositoryImpl;

/**
 * This is the implementation of the InfluxDB {@link PersistenceService}. It persists item values
 * using the <a href="http://influxdb.org">InfluxDB time series database. The states (
 * {@link State}) of an {@link Item} are persisted by default in a time series with names equal to the name of
 * the item.
 *
 * This addon supports 1.X and 2.X versions, as two versions are incompatible and use different drivers the
 * specific code for each version is accessed by {@link InfluxDBRepository} and {@link FilterCriteriaQueryCreator}
 * interfaces and specific implementation reside in {@link org.smarthomej.persistence.influxdb.internal.influx1} and
 * {@link org.smarthomej.persistence.influxdb.internal.influx2} packages
 *
 * @author Theo Weiss - Initial contribution, rewrite of org.openhab.persistence.influxdb
 * @author Joan Pujol Espinar - Addon rewrite refactoring code and adding support for InfluxDB 2.0. Some tag code is
 *         based
 *         from not integrated branch from Dominik Vorreiter
 */
@NonNullByDefault
@Component(service = { PersistenceService.class,
        QueryablePersistenceService.class }, configurationPid = "org.openhab.influxdb", //
        property = Constants.SERVICE_PID + "=org.openhab.influxdb")
@ConfigurableService(category = "persistence", label = "InfluxDB Persistence Service", description_uri = InfluxDBPersistenceService.CONFIG_URI)
public class InfluxDBPersistenceService implements QueryablePersistenceService {
    public static final String SERVICE_NAME = "influxdb";

    private final Logger logger = LoggerFactory.getLogger(InfluxDBPersistenceService.class);

    protected static final String CONFIG_URI = "persistence:influxdb";

    // External dependencies
    private final ItemRegistry itemRegistry;
    private final InfluxDBMetadataService influxDBMetadataService;
    // Internal dependencies/state
    private InfluxDBConfiguration configuration = InfluxDBConfiguration.NO_CONFIGURATION;

    // Relax rules because can only be null if component is not active
    private @NonNullByDefault({}) ItemToStorePointCreator itemToStorePointCreator;
    private @NonNullByDefault({}) InfluxDBRepository influxDBRepository;

    private boolean tryReconnection = false;

    @Activate
    public InfluxDBPersistenceService(final @Reference ItemRegistry itemRegistry,
            final @Reference InfluxDBMetadataService influxDBMetadataService) {
        this.itemRegistry = itemRegistry;
        this.influxDBMetadataService = influxDBMetadataService;
    }

    /**
     * Connect to database when service is activated
     */
    @Activate
    public void activate(final @Nullable Map<String, Object> config) {
        logger.debug("InfluxDB persistence service is being activated");

        if (loadConfiguration(config)) {
            itemToStorePointCreator = new ItemToStorePointCreator(configuration, influxDBMetadataService);
            influxDBRepository = createInfluxDBRepository();
            influxDBRepository.connect();
            tryReconnection = true;
        } else {
            logger.error("Cannot load configuration, persistence service wont work");
            tryReconnection = false;
        }

        logger.debug("InfluxDB persistence service is now activated");
    }

    // Visible for testing
    protected InfluxDBRepository createInfluxDBRepository() {
        switch (configuration.getVersion()) {
            case V1:
                return new InfluxDB1RepositoryImpl(configuration, influxDBMetadataService);
            case V2:
                return new InfluxDB2RepositoryImpl(configuration, influxDBMetadataService);
            default:
                throw new UnnexpectedConditionException("Not expected version " + configuration.getVersion());
        }
    }

    /**
     * Disconnect from database when service is deactivated
     */
    @Deactivate
    public void deactivate() {
        logger.debug("InfluxDB persistence service deactivated");
        if (influxDBRepository != null) {
            tryReconnection = false;
            influxDBRepository.disconnect();
            influxDBRepository = null;
        }
        if (itemToStorePointCreator != null) {
            itemToStorePointCreator = null;
        }
    }

    /**
     * Rerun deactivation/activation code each time configuration is changed
     */
    @Modified
    protected void modified(@Nullable Map<String, Object> config) {
        if (config != null) {
            logger.debug("Config has been modified will deactivate/activate with new config");

            deactivate();
            activate(config);
        } else {
            logger.warn("Null configuration, ignoring");
        }
    }

    private boolean loadConfiguration(@Nullable Map<String, Object> config) {
        boolean configurationIsValid;
        if (config != null) {
            configuration = new InfluxDBConfiguration(config);
            configurationIsValid = configuration.isValid();
            if (configurationIsValid) {
                logger.debug("Loaded configuration {}", config);
            } else {
                logger.warn("Some configuration properties are not valid {}", config);
            }
        } else {
            configuration = InfluxDBConfiguration.NO_CONFIGURATION;
            configurationIsValid = false;
            logger.warn("Ignoring configuration because it's null");
        }
        return configurationIsValid;
    }

    @Override
    public String getId() {
        return SERVICE_NAME;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "InfluxDB persistence layer";
    }

    /**
     * check connection and try reconnect
     *
     * @return true if connected
     */
    private boolean checkConnection() {
        if (influxDBRepository == null) {
            return false;
        } else if (influxDBRepository.isConnected()) {
            return true;
        } else if (tryReconnection) {
            logger.debug("Connection lost, trying re-connection");
            influxDBRepository.connect();
            return influxDBRepository.isConnected();
        }
        return false;
    }

    @Override
    public Set<PersistenceItemInfo> getItemInfo() {
        if (checkConnection()) {
            return influxDBRepository.getStoredItemsCount().entrySet().stream().map(InfluxDBPersistentItemInfo::new)
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            logger.info("getItemInfo ignored, InfluxDB is not yet connected");
            return Set.of();
        }
    }

    @Override
    public void store(Item item) {
        store(item, item.getName());
    }

    @Override
    public void store(Item item, @Nullable String alias) {
        if (checkConnection()) {
            InfluxPoint point = itemToStorePointCreator.convert(item, alias);
            if (point != null) {
                logger.trace("Storing item {} in InfluxDB point {}", item, point);
                influxDBRepository.write(point);
            } else {
                logger.trace("Ignoring item {}, conversion to a InfluxDB point failed.", item);
            }
        } else {
            logger.debug("store ignored, InfluxDB is not yet connected");
        }
    }

    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {
        logger.debug("Got a query for historic points!");

        if (checkConnection()) {
            logger.trace(
                    "Filter: itemname: {}, ordering: {}, state: {},  operator: {}, getBeginDate: {}, getEndDate: {}, getPageSize: {}, getPageNumber: {}",
                    filter.getItemName(), filter.getOrdering().toString(), filter.getState(), filter.getOperator(),
                    filter.getBeginDate(), filter.getEndDate(), filter.getPageSize(), filter.getPageNumber());

            String query = influxDBRepository.createQueryCreator().createQuery(filter,
                    configuration.getRetentionPolicy());
            logger.trace("Query {}", query);
            List<InfluxRow> results = influxDBRepository.query(query);
            return results.stream().map(this::mapRow2HistoricItem).collect(Collectors.toList());
        } else {
            logger.debug("query ignored, InfluxDB is not yet connected");
            return List.of();
        }
    }

    private HistoricItem mapRow2HistoricItem(InfluxRow row) {
        State state = InfluxDBStateConvertUtils.objectToState(row.getValue(), row.getItemName(), itemRegistry);
        return new InfluxDBHistoricItem(row.getItemName(), state,
                ZonedDateTime.ofInstant(row.getTime(), ZoneId.systemDefault()));
    }

    @Override
    public List<PersistenceStrategy> getDefaultStrategies() {
        return List.of(PersistenceStrategy.Globals.RESTORE, PersistenceStrategy.Globals.CHANGE);
    }
}
