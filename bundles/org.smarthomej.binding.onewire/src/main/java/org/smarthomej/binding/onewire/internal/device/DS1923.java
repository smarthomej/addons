/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 * Copyright (c) 2021-2022 Contributors to the SmartHome/J project
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
package org.smarthomej.binding.onewire.internal.device;

import static org.smarthomej.binding.onewire.internal.OwBindingConstants.*;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.onewire.internal.OwException;
import org.smarthomej.binding.onewire.internal.SensorId;
import org.smarthomej.binding.onewire.internal.Util;
import org.smarthomej.binding.onewire.internal.handler.OwBaseThingHandler;
import org.smarthomej.binding.onewire.internal.handler.OwserverBridgeHandler;
import org.smarthomej.binding.onewire.internal.owserver.OwserverDeviceParameter;

/**
 * The {@link DS1923} class defines an DS1923 device
 *
 * @author Jan N. Klug - Initial contribution
 * @author Michał Wójcik - Adapted to DS1923
 */
@NonNullByDefault
public class DS1923 extends AbstractOwDevice {
    private final Logger logger = LoggerFactory.getLogger(DS1923.class);
    private final OwserverDeviceParameter temperatureParameter = new OwserverDeviceParameter("/temperature");
    private final OwserverDeviceParameter humidityParameter = new OwserverDeviceParameter("/humidity");

    public DS1923(SensorId sensorId, OwBaseThingHandler callback) {
        super(sensorId, callback);
    }

    @Override
    public void configureChannels() throws OwException {
        isConfigured = true;
    }

    @Override
    public void refresh(OwserverBridgeHandler bridgeHandler, Boolean forcedRefresh) throws OwException {
        if (isConfigured) {
            logger.trace("refresh of sensor {} started", sensorId);
            if (enabledChannels.contains(CHANNEL_TEMPERATURE) || enabledChannels.contains(CHANNEL_HUMIDITY)
                    || enabledChannels.contains(CHANNEL_ABSOLUTE_HUMIDITY)
                    || enabledChannels.contains(CHANNEL_DEWPOINT)) {
                QuantityType<Temperature> temperature = new QuantityType<>(
                        (DecimalType) bridgeHandler.readDecimalType(sensorId, temperatureParameter), SIUnits.CELSIUS);
                callback.postUpdate(CHANNEL_TEMPERATURE, temperature);

                if (enabledChannels.contains(CHANNEL_HUMIDITY) || enabledChannels.contains(CHANNEL_ABSOLUTE_HUMIDITY)
                        || enabledChannels.contains(CHANNEL_DEWPOINT)) {
                    QuantityType<Dimensionless> humidity = new QuantityType<>(
                            (DecimalType) bridgeHandler.readDecimalType(sensorId, humidityParameter), Units.PERCENT);

                    if (enabledChannels.contains(CHANNEL_HUMIDITY)) {
                        callback.postUpdate(CHANNEL_HUMIDITY, humidity);
                    }

                    if (enabledChannels.contains(CHANNEL_ABSOLUTE_HUMIDITY)) {
                        callback.postUpdate(CHANNEL_ABSOLUTE_HUMIDITY,
                                Util.calculateAbsoluteHumidity(temperature, humidity));
                    }

                    if (enabledChannels.contains(CHANNEL_DEWPOINT)) {
                        callback.postUpdate(CHANNEL_DEWPOINT, Util.calculateDewpoint(temperature, humidity));
                    }
                }
            }
        }
    }
}
