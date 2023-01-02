/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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

import static org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants.CHANNEL_TYPE_AIR_QUALITY_HUMIDITY;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.measure.quantity.Dimensionless;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevice;

import com.google.gson.JsonObject;

/**
 * The {@link HandlerHumiditySensor} is responsible for the Alexa.HumiditySensor interface
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class HandlerHumiditySensor extends AbstractInterfaceHandler {
    public static final String INTERFACE = "Alexa.HumiditySensor";

    private static final ChannelInfo HUMIDITY = new ChannelInfo("relativeHumidity", "humidity",
            CHANNEL_TYPE_AIR_QUALITY_HUMIDITY);

    public HandlerHumiditySensor(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler, List.of(INTERFACE));
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(JsonSmartHomeCapability capability, @Nullable String property) {
        if (HUMIDITY.propertyName.equals(property)) {
            return Set.of(HUMIDITY);
        }
        return Set.of();
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        QuantityType<Dimensionless> humidityValue = null;
        for (JsonObject state : stateList) {
            if (HUMIDITY.propertyName.equals(state.get("name").getAsString())) {
                JsonObject value = state.get("value").getAsJsonObject();
                // For groups take the first
                if (humidityValue == null) {
                    BigDecimal humidity = value.get("value").getAsBigDecimal();
                    humidityValue = new QuantityType<>(humidity, Units.PERCENT);
                }
            }
        }
        smartHomeDeviceHandler.updateState(HUMIDITY.channelId, humidityValue == null ? UnDefType.UNDEF : humidityValue);
    }

    @Override
    public boolean handleCommand(Connection connection, JsonSmartHomeDevice shd, String entityId,
            List<JsonSmartHomeCapability> capabilities, String channelId, Command command) throws IOException {
        return false;
    }
}
