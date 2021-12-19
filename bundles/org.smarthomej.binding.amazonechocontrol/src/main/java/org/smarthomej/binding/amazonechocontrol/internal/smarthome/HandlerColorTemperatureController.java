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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.smarthomej.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapabilities.SmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevices.SmartHomeDevice;

import com.google.gson.JsonObject;

/**
 * The {@link HandlerColorTemperatureController} is responsible for the Alexa.ColorTemperatureController interface
 *
 * @author Lukas Knoeller - Initial contribution
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class HandlerColorTemperatureController extends AbstractInterfaceHandler {
    // Interface
    public static final String INTERFACE = "Alexa.ColorTemperatureController";
    public static final String INTERFACE_COLOR_PROPERTIES = "Alexa.ColorPropertiesController";

    // Channel types
    private static final ChannelTypeUID CHANNEL_TYPE_COLOR_TEMPERATURE_NAME = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "colorTemperatureName");

    private static final ChannelTypeUID CHANNEL_TYPE_COLOR_TEPERATURE_IN_KELVIN = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "colorTemperatureInKelvin");

    // Channel and Properties
    private static final ChannelInfo COLOR_TEMPERATURE_IN_KELVIN = new ChannelInfo(
            "colorTemperatureInKelvin" /* propertyName */ , "colorTemperatureInKelvin" /* ChannelId */,
            CHANNEL_TYPE_COLOR_TEPERATURE_IN_KELVIN /* Channel Type */ );

    private static final ChannelInfo COLOR_TEMPERATURE_NAME = new ChannelInfo("colorProperties" /* propertyName */ ,
            "colorTemperatureName" /* ChannelId */, CHANNEL_TYPE_COLOR_TEMPERATURE_NAME /* Channel Type */ );

    private @Nullable Integer lastColorTemperature;
    private @Nullable String lastColorName;

    public HandlerColorTemperatureController(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler, List.of(INTERFACE, INTERFACE_COLOR_PROPERTIES));
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(SmartHomeCapability capability, @Nullable String property) {
        if (COLOR_TEMPERATURE_IN_KELVIN.propertyName.equals(property)) {
            return Set.of(COLOR_TEMPERATURE_IN_KELVIN, COLOR_TEMPERATURE_NAME);
        }
        return Set.of();
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        if (INTERFACE.equals(interfaceName)) {
            Integer colorTemperatureInKelvinValue = null;
            for (JsonObject state : stateList) {
                if (COLOR_TEMPERATURE_IN_KELVIN.propertyName.equals(state.get("name").getAsString())) {
                    int value = state.get("value").getAsInt();
                    // For groups take the maximum
                    if (colorTemperatureInKelvinValue == null) {
                        colorTemperatureInKelvinValue = value;
                    }
                }
            }
            if (colorTemperatureInKelvinValue != null && !colorTemperatureInKelvinValue.equals(lastColorTemperature)) {
                lastColorTemperature = colorTemperatureInKelvinValue;
                result.needSingleUpdate = true;
            }
            smartHomeDeviceHandler.updateState(COLOR_TEMPERATURE_IN_KELVIN.channelId,
                    colorTemperatureInKelvinValue == null ? UnDefType.UNDEF
                            : new DecimalType(colorTemperatureInKelvinValue));
        }
        if (INTERFACE_COLOR_PROPERTIES.equals(interfaceName)) {
            String colorTemperatureNameValue = null;
            for (JsonObject state : stateList) {
                if (COLOR_TEMPERATURE_NAME.propertyName.equals(state.get("name").getAsString())) {
                    if (colorTemperatureNameValue == null) {
                        result.needSingleUpdate = false;
                        colorTemperatureNameValue = state.get("value").getAsJsonObject().get("name").getAsString();
                    }
                }
            }
            if (lastColorName == null) {
                lastColorName = colorTemperatureNameValue;
            } else if (colorTemperatureNameValue == null && lastColorName != null) {
                colorTemperatureNameValue = lastColorName;
            }
            smartHomeDeviceHandler.updateState(COLOR_TEMPERATURE_NAME.channelId,
                    colorTemperatureNameValue == null ? UnDefType.UNDEF : new StringType(colorTemperatureNameValue));
        }
    }

    @Override
    public boolean handleCommand(Connection connection, SmartHomeDevice shd, String entityId,
            List<SmartHomeCapability> capabilities, String channelId, Command command)
            throws IOException, InterruptedException {
        if (channelId.equals(COLOR_TEMPERATURE_IN_KELVIN.channelId)) {
            // WRITING TO THIS CHANNEL DOES CURRENTLY NOT WORK, BUT WE LEAVE THE CODE FOR FUTURE USE!
            if (containsCapabilityProperty(capabilities, COLOR_TEMPERATURE_IN_KELVIN.propertyName)) {
                if (command instanceof DecimalType) {
                    int intValue = ((DecimalType) command).intValue();
                    if (intValue < 1000) {
                        intValue = 1000;
                    }
                    if (intValue > 10000) {
                        intValue = 10000;
                    }
                    connection.smartHomeCommand(entityId, "setColorTemperature",
                            Map.of("colorTemperatureInKelvin", intValue));
                    return true;
                }
            }
        }
        if (channelId.equals(COLOR_TEMPERATURE_NAME.channelId)) {
            if (containsCapabilityProperty(capabilities, COLOR_TEMPERATURE_IN_KELVIN.propertyName)) {
                if (command instanceof StringType) {
                    String colorTemperatureName = command.toFullString();
                    if (!colorTemperatureName.isEmpty()) {
                        lastColorName = colorTemperatureName;
                        connection.smartHomeCommand(entityId, "setColorTemperature",
                                Map.of("colorTemperatureName", colorTemperatureName));
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
