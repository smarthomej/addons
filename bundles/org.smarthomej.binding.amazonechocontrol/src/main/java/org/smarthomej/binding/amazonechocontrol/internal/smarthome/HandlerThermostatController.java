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

import static org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.types.Command;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.UnDefType;
import org.smarthomej.binding.amazonechocontrol.internal.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapabilities.SmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevices.SmartHomeDevice;

import com.google.gson.JsonObject;

/**
 * The {@link HandlerThermostatController} is responsible for the Alexa.ThermostatControllerInterface
 *
 * @author Sven Killig - Initial contribution
 */
@NonNullByDefault
public class HandlerThermostatController extends HandlerBase {
    // Interface
    public static final String INTERFACE = "Alexa.ThermostatController";
    // Channel definitions
    private static final ChannelInfo TARGET_SETPOINT = new ChannelInfo("targetSetpoint" /* propertyName */ ,
            "targetSetpoint" /* ChannelId */, CHANNEL_TYPE_TARGETSETPOINT /* Channel Type */ ,
            ITEM_TYPE_NUMBER_TEMPERATURE /* Item Type */);
    private static final ChannelInfo LOWER_SETPOINT = new ChannelInfo("lowerSetpoint" /* propertyName */ ,
            "lowerSetpoint" /* ChannelId */, CHANNEL_TYPE_TARGETSETPOINT /* Channel Type */ ,
            ITEM_TYPE_NUMBER_TEMPERATURE /* Item Type */);
    private static final ChannelInfo UPPER_SETPOINT = new ChannelInfo("upperSetpoint" /* propertyName */ ,
            "upperSetpoint" /* ChannelId */, CHANNEL_TYPE_TARGETSETPOINT /* Channel Type */ ,
            ITEM_TYPE_NUMBER_TEMPERATURE /* Item Type */);
    private static final Set<ChannelInfo> ALL_CHANNELS = Set.of(TARGET_SETPOINT, LOWER_SETPOINT, UPPER_SETPOINT);

    public HandlerThermostatController(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler);
    }

    @Override
    public String[] getSupportedInterface() {
        return new String[] { INTERFACE };
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(SmartHomeCapability capability, String property) {
        return ALL_CHANNELS.stream().filter(c -> c.propertyName.equals(property)).collect(Collectors.toSet());
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        ALL_CHANNELS.forEach(channel -> {
            QuantityType<Temperature> temperatureValue = null;
            for (JsonObject state : stateList) {
                if (channel.propertyName.equals(state.get("name").getAsString())) {
                    JsonObject value = state.get("value").getAsJsonObject();
                    // For groups take the first
                    if (temperatureValue == null) {
                        float temperature = value.get("value").getAsFloat();
                        String scale = value.get("scale").getAsString().toUpperCase();
                        if ("CELSIUS".equals(scale)) {
                            temperatureValue = new QuantityType<>(temperature, SIUnits.CELSIUS);
                        } else {
                            temperatureValue = new QuantityType<>(temperature, ImperialUnits.FAHRENHEIT);
                        }
                    }
                }
            }
            updateState(channel.channelId, Objects.requireNonNullElse(temperatureValue, UnDefType.UNDEF));
        });
    }

    @Override
    public boolean handleCommand(Connection connection, SmartHomeDevice shd, String entityId,
            List<SmartHomeCapability> capabilities, String channelId, Command command)
            throws IOException, InterruptedException {
        ChannelInfo channelInfo = ALL_CHANNELS.stream().filter(c -> c.channelId.equals(channelId)).findFirst()
                .orElse(null);
        if (channelInfo != null) {
            if (containsCapabilityProperty(capabilities, channelInfo.propertyName)) {
                if (command instanceof QuantityType) {
                    connection.smartHomeCommand(entityId, "setTargetTemperature", channelInfo.propertyName, command);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public @Nullable StateDescription findStateDescription(String channelId, StateDescription originalStateDescription,
            @Nullable Locale locale) {
        return null;
    }
}
