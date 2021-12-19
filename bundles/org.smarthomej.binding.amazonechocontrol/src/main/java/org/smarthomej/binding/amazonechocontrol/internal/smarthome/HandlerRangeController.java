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
package org.smarthomej.binding.amazonechocontrol.internal.smarthome;

import static org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants.CHANNEL_TYPE_AIR_QUALITY_CARBON_MONOXIDE;
import static org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants.CHANNEL_TYPE_AIR_QUALITY_HUMIDITY;
import static org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants.CHANNEL_TYPE_AIR_QUALITY_INDOOR_AIR_QUALITY;
import static org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants.CHANNEL_TYPE_AIR_QUALITY_PM25;
import static org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants.CHANNEL_TYPE_AIR_QUALITY_VOC;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapabilities;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapabilities.SmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevices.SmartHomeDevice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link HandlerRangeController} is responsible for the Alexa.RangeController interface
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class HandlerRangeController extends AbstractInterfaceHandler {
    public static final String INTERFACE = "Alexa.RangeController";

    private final Logger logger = LoggerFactory.getLogger(HandlerRangeController.class);

    private static final Map<String, ChannelInfo> CHANNEL_INFO_MAP = Map.ofEntries(
            Map.entry("Alexa.AirQuality.IndoorAirQuality",
                    new ChannelInfo("Alexa.AirQuality.IndoorAirQuality", "indoorAirQuality",
                            CHANNEL_TYPE_AIR_QUALITY_INDOOR_AIR_QUALITY)),
            Map.entry("Alexa.AirQuality.Humidity",
                    new ChannelInfo("Alexa.AirQuality.Humidity", "humidity", CHANNEL_TYPE_AIR_QUALITY_HUMIDITY)),
            Map.entry("Alexa.AirQuality.ParticulateMatter",
                    new ChannelInfo("Alexa.AirQuality.ParticulateMatter", "pm25", CHANNEL_TYPE_AIR_QUALITY_PM25)),
            Map.entry("Alexa.AirQuality.VolatileOrganicCompounds",
                    new ChannelInfo("Alexa.AirQuality.VolatileOrganicCompounds", "voc", CHANNEL_TYPE_AIR_QUALITY_VOC)),
            Map.entry("Alexa.AirQuality.CarbonMonoxide", new ChannelInfo("Alexa.AirQuality.CarbonMonoxide",
                    "carbonMonoxide", CHANNEL_TYPE_AIR_QUALITY_CARBON_MONOXIDE)));

    private static final Map<String, Unit<?>> ALEXA_UNITS_TO_SMART_HOME_UNITS = Map.of( //
            "Alexa.Unit.Percent", Units.PERCENT, //
            "Alexa.Unit.Density.MicroGramsPerCubicMeter", Units.MICROGRAM_PER_CUBICMETRE, //
            "Alexa.Unit.PartsPerMillion", Units.PARTS_PER_MILLION);

    private final Map<String, ChannelInfo> instanceToChannelInfo = new HashMap<>();
    private final Map<String, Unit<?>> instanceToUnit = new HashMap<>();

    public HandlerRangeController(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler, List.of(INTERFACE));
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(SmartHomeCapability capability, @Nullable String property) {
        JsonSmartHomeCapabilities.Resources resources = capability.resources;
        String instance = capability.instance;
        if (resources == null || instance == null) { // instance is needed to identify state updates
            return Set.of();
        }

        List<JsonSmartHomeCapabilities.Resources.Names> names = resources.friendlyNames;
        if (names == null) {
            return Set.of();
        }

        JsonSmartHomeCapabilities.Resources.Names.Value nameValue = names.stream().filter(n -> "asset".equals(n.type))
                .findAny().map(n -> n.value).orElse(null);
        if (nameValue == null) {
            return Set.of();
        }

        String assetId = nameValue.assetId;
        if (assetId == null) {
            return Set.of();
        }

        ChannelInfo channelInfo = CHANNEL_INFO_MAP.get(assetId);
        if (channelInfo != null) {
            instanceToChannelInfo.put(instance, channelInfo);
            // try to get configuration if present
            JsonSmartHomeCapabilities.Configuration configuration = capability.configuration;
            if (configuration != null) {
                String alexaUnit = configuration.unitOfMeasure;
                if (alexaUnit != null) {
                    Unit<?> unit = ALEXA_UNITS_TO_SMART_HOME_UNITS.get(alexaUnit);
                    if (unit != null) {
                        instanceToUnit.put(instance, unit);
                    }
                }
            }
            return Set.of(channelInfo);
        }

        return Set.of();
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        for (JsonObject state : stateList) {
            JsonElement instanceElement = state.get("instance");
            JsonElement valueElement = state.get("value");
            if (instanceElement == null || valueElement == null) {
                logger.trace("Could not identify instance in {}, skipping update.", state);
                continue;
            }
            String instance = instanceElement.getAsString();
            ChannelInfo channelInfo = instanceToChannelInfo.get(instance);
            if (channelInfo != null) {
                double value = valueElement.getAsDouble();
                Unit<?> unit = instanceToUnit.get(instance);
                if (unit != null) {
                    // if unit is present, use QuantityType;
                    smartHomeDeviceHandler.updateState(channelInfo.channelId, new QuantityType<>(value, unit));
                } else {
                    // fallback is DecimalType
                    smartHomeDeviceHandler.updateState(channelInfo.channelId, new DecimalType(value));
                }
            }
        }
    }

    @Override
    public boolean handleCommand(Connection connection, SmartHomeDevice shd, String entityId,
            List<SmartHomeCapability> capabilities, String channelId, Command command)
            throws IOException, InterruptedException {
        return false;
    }
}
