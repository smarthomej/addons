/**
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
package org.smarthomej.binding.amazonechocontrol.internal.smarthome;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.DefaultSystemChannelTypeProvider;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevice;

import com.google.gson.JsonObject;

/**
 * The {@link HandlerBatteryLevelSensor} is responsible for the Alexa.BatteryLevelSensor interface
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class HandlerBatteryLevelSensor extends AbstractInterfaceHandler {
    public static final String INTERFACE = "Alexa.BatteryLevelSensor";

    private static final ChannelInfo BATTERY_LEVEL_STATE = new ChannelInfo("batterylevel", "batteryLevel",
            DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_BATTERY_LEVEL);

    private final Logger logger = LoggerFactory.getLogger(HandlerBatteryLevelSensor.class);

    public HandlerBatteryLevelSensor(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler, List.of(INTERFACE));
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(JsonSmartHomeCapability capability, @Nullable String property) {
        if (BATTERY_LEVEL_STATE.propertyName.equals(property)) {
            return Set.of(BATTERY_LEVEL_STATE);
        }
        return Set.of();
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        for (JsonObject state : stateList) {
            if (BATTERY_LEVEL_STATE.propertyName.equals(state.get("name").getAsString())) {
                logger.error("batteryLevel: {}", state);
            }
        }
    }

    @Override
    public boolean handleCommand(Connection connection, JsonSmartHomeDevice shd, String entityId,
            List<JsonSmartHomeCapability> capabilities, String channelId, Command command)
            throws IOException, InterruptedException {
        return false;
    }
}
