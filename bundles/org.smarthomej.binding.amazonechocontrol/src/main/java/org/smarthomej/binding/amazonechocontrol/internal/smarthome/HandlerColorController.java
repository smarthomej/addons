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
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
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
 * The {@link HandlerColorController} is responsible for the Alexa.ColorTemperatureController interface
 *
 * @author Lukas Knoeller - Initial contribution
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class HandlerColorController extends AbstractInterfaceHandler {
    // Interface
    public static final String INTERFACE = "Alexa.ColorController";
    public static final String INTERFACE_COLOR_PROPERTIES = "Alexa.ColorPropertiesController";

    // Channel types
    private static final ChannelTypeUID CHANNEL_TYPE_COLOR_NAME = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "colorName");

    private static final ChannelTypeUID CHANNEL_TYPE_COLOR = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "color");

    // Channel and Properties
    private static final ChannelInfo COLOR = new ChannelInfo("color" /* propertyName */, "color" /* ChannelId */,
            CHANNEL_TYPE_COLOR /* Channel Type */);

    private static final ChannelInfo COLOR_PROPERTIES = new ChannelInfo("colorProperties" /* propertyName */,
            "colorName" /* ChannelId */, CHANNEL_TYPE_COLOR_NAME /* Channel Type */);

    private @Nullable HSBType lastColor;
    private @Nullable String lastColorName;

    public HandlerColorController(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler, List.of(INTERFACE, INTERFACE_COLOR_PROPERTIES));
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(SmartHomeCapability capability, @Nullable String property) {
        if (COLOR.propertyName.equals(property)) {
            return Set.of(COLOR, COLOR_PROPERTIES);
        }
        return Set.of();
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        if (INTERFACE.equals(interfaceName)) {
            // WRITING TO THIS CHANNEL DOES CURRENTLY NOT WORK, BUT WE LEAVE THE CODE FOR FUTURE USE!
            HSBType colorValue = null;
            for (JsonObject state : stateList) {
                if (COLOR.propertyName.equals(state.get("name").getAsString())) {
                    JsonObject value = state.get("value").getAsJsonObject();
                    // For groups take the maximum
                    if (colorValue == null) {
                        colorValue = new HSBType(new DecimalType(value.get("hue").getAsInt()),
                                new PercentType(value.get("saturation").getAsInt() * 100),
                                new PercentType(value.get("brightness").getAsInt() * 100));
                    }
                }
            }
            if (colorValue != null) {
                if (!colorValue.equals(lastColor)) {
                    result.needSingleUpdate = true;
                    lastColor = colorValue;
                }
            }
            smartHomeDeviceHandler.updateState(COLOR.channelId, colorValue == null ? UnDefType.UNDEF : colorValue);
        }
        if (INTERFACE_COLOR_PROPERTIES.equals(interfaceName)) {
            String colorNameValue = null;
            for (JsonObject state : stateList) {
                if (COLOR_PROPERTIES.propertyName.equals(state.get("name").getAsString())) {
                    if (colorNameValue == null) {
                        result.needSingleUpdate = false;
                        colorNameValue = state.get("value").getAsJsonObject().get("name").getAsString();
                    }
                }
            }
            if (colorNameValue == null && lastColorName != null) {
                colorNameValue = lastColorName;
            }
            lastColorName = colorNameValue;
            smartHomeDeviceHandler.updateState(COLOR_PROPERTIES.channelId,
                    lastColorName == null ? UnDefType.UNDEF : new StringType(lastColorName));
        }
    }

    @Override
    public boolean handleCommand(Connection connection, SmartHomeDevice shd, String entityId,
            List<SmartHomeCapability> capabilities, String channelId, Command command)
            throws IOException, InterruptedException {
        if (channelId.equals(COLOR.channelId)) {
            if (containsCapabilityProperty(capabilities, COLOR.propertyName)) {
                if (command instanceof HSBType) {
                    HSBType color = ((HSBType) command);
                    JsonObject colorObject = new JsonObject();
                    colorObject.addProperty("hue", color.getHue());
                    colorObject.addProperty("saturation", color.getSaturation().floatValue() / 100);
                    colorObject.addProperty("brightness", color.getBrightness().floatValue() / 100);
                    connection.smartHomeCommand(entityId, "setColor", Map.of("value", colorObject));
                }
            }
        }
        if (channelId.equals(COLOR_PROPERTIES.channelId)) {
            if (containsCapabilityProperty(capabilities, COLOR.propertyName)) {
                if (command instanceof StringType) {
                    String colorName = command.toFullString();
                    if (!colorName.isEmpty()) {
                        lastColorName = colorName;
                        connection.smartHomeCommand(entityId, "setColor", Map.of("colorName", colorName));
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
