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
import java.util.Set;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapabilities.SmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevices.SmartHomeDevice;

import com.google.gson.JsonObject;

/**
 * The {@link HandlerTemperatureSensor} is responsible for the Alexa.PowerControllerInterface
 *
 * @author Lukas Knoeller - Initial contribution
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class HandlerTemperatureSensor extends AbstractInterfaceHandler {
    public static final String INTERFACE = "Alexa.TemperatureSensor";

    private static final ChannelInfo TEMPERATURE = new ChannelInfo("temperature", "temperature",
            CHANNEL_TYPE_TEMPERATURE);

    public HandlerTemperatureSensor(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler, List.of(INTERFACE));
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(SmartHomeCapability capability, @Nullable String property) {
        if (TEMPERATURE.propertyName.equals(property)) {
            return Set.of(TEMPERATURE);
        }
        return Set.of();
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        QuantityType<Temperature> temperatureValue = null;
        for (JsonObject state : stateList) {
            if (TEMPERATURE.propertyName.equals(state.get("name").getAsString())) {
                JsonObject value = state.get("value").getAsJsonObject();
                // For groups take the first
                if (temperatureValue == null) {
                    float temperature = value.get("value").getAsFloat();
                    String scale = value.get("scale").getAsString();
                    if ("CELSIUS".equals(scale)) {
                        temperatureValue = new QuantityType<Temperature>(temperature, SIUnits.CELSIUS);
                    } else {
                        temperatureValue = new QuantityType<Temperature>(temperature, ImperialUnits.FAHRENHEIT);
                    }
                }
            }
        }
        smartHomeDeviceHandler.updateState(TEMPERATURE.channelId,
                temperatureValue == null ? UnDefType.UNDEF : temperatureValue);
    }

    @Override
    public boolean handleCommand(Connection connection, SmartHomeDevice shd, String entityId,
            List<SmartHomeCapability> capabilities, String channelId, Command command) throws IOException {
        return false;
    }
}
