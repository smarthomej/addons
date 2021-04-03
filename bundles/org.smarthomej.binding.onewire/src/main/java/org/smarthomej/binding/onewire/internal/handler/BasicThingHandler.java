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
package org.smarthomej.binding.onewire.internal.handler;

import static org.smarthomej.binding.onewire.internal.OwBindingConstants.CHANNEL_DIGITAL;
import static org.smarthomej.binding.onewire.internal.OwBindingConstants.THING_TYPE_BASIC;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.onewire.internal.OwDynamicStateDescriptionProvider;
import org.smarthomej.binding.onewire.internal.device.*;

/**
 * The {@link BasicThingHandler} is responsible for handling simple sensors
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class BasicThingHandler extends OwBaseThingHandler {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BASIC);
    public static final Set<OwSensorType> SUPPORTED_SENSOR_TYPES = Collections
            .unmodifiableSet(Stream.of(OwSensorType.DS1420, OwSensorType.DS18B20, OwSensorType.DS18S20,
                    OwSensorType.DS1822, OwSensorType.DS2401, OwSensorType.DS2405, OwSensorType.DS2406,
                    OwSensorType.DS2408, OwSensorType.DS2413, OwSensorType.DS2423).collect(Collectors.toSet()));

    private final Logger logger = LoggerFactory.getLogger(BasicThingHandler.class);

    public BasicThingHandler(Thing thing, OwDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        super(thing, dynamicStateDescriptionProvider, SUPPORTED_SENSOR_TYPES);
    }

    @Override
    public void initialize() {
        if (!super.configureThingHandler()) {
            return;
        }

        // add sensor
        switch (sensorType) {
            case DS18B20:
            case DS18S20:
            case DS1822:
                sensors.add(new DS18x20(sensorId, this));
                break;
            case DS1420:
            case DS2401:
                sensors.add(new DS2401(sensorId, this));
                break;
            case DS2405:
                sensors.add(new DS2405(sensorId, this));
                break;
            case DS2406:
            case DS2413:
                sensors.add(new DS2406DS2413(sensorId, this));
                break;
            case DS2408:
                sensors.add(new DS2408(sensorId, this));
                break;
            case DS2423:
                sensors.add(new DS2423(sensorId, this));
                break;
            default:
                throw new IllegalArgumentException(
                        "unsupported sensorType " + sensorType.name() + ", this should have been checked before!");
        }

        scheduler.execute(() -> {
            configureThingChannels();
        });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof OnOffType) {
            if (channelUID.getId().startsWith(CHANNEL_DIGITAL) && thing.getChannel(channelUID.getId()) != null) {
                Integer ioChannel = Integer.valueOf(channelUID.getId().substring(channelUID.getId().length() - 1));
                Bridge bridge = getBridge();
                if (bridge != null) {
                    OwserverBridgeHandler bridgeHandler = (OwserverBridgeHandler) bridge.getHandler();
                    if (bridgeHandler != null) {
                        if (!((AbstractDigitalOwDevice) sensors.get(0)).writeChannel(bridgeHandler, ioChannel,
                                command)) {
                            logger.debug("writing to channel {} in thing {} not permitted (input channel)", channelUID,
                                    this.thing.getUID());
                        }
                    } else {
                        logger.warn("bridge handler not found");
                    }
                } else {
                    logger.warn("bridge not found");
                }
            }
        }
        super.handleCommand(channelUID, command);
    }
}
