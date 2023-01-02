/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevice;

import com.google.gson.JsonObject;

/**
 * The {@link HandlerSecurityPanelController} is responsible for the Alexa.PowerControllerInterface
 *
 * @author Lukas Knoeller - Initial contribution
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class HandlerSecurityPanelController extends AbstractInterfaceHandler {
    public static final String INTERFACE = "Alexa.SecurityPanelController";

    // Channel definitions
    private static final ChannelInfo ARM_STATE = new ChannelInfo("armState", "armState",
            Constants.CHANNEL_TYPE_ARM_STATE);

    private static final ChannelInfo BURGLARY_ALARM = new ChannelInfo("burglaryAlarm", "burglaryAlarm",
            Constants.CHANNEL_TYPE_BURGLARY_ALARM);

    private static final ChannelInfo CARBON_MONOXIDE_ALARM = new ChannelInfo("carbonMonoxideAlarm",
            "carbonMonoxideAlarm", Constants.CHANNEL_TYPE_CARBON_MONOXIDE_ALARM);

    private static final ChannelInfo FIRE_ALARM = new ChannelInfo("fireAlarm", "fireAlarm",
            Constants.CHANNEL_TYPE_FIRE_ALARM);

    private static final ChannelInfo WATER_ALARM = new ChannelInfo("waterAlarm", "waterAlarm",
            Constants.CHANNEL_TYPE_WATER_ALARM);

    private static final Set<ChannelInfo> ALARM_CHANNELS = Set.of(BURGLARY_ALARM, CARBON_MONOXIDE_ALARM, FIRE_ALARM,
            WATER_ALARM);

    public HandlerSecurityPanelController(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler, List.of(INTERFACE));
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(JsonSmartHomeCapability capability, @Nullable String property) {
        if (ARM_STATE.propertyName.equals(property)) {
            return Set.of(ARM_STATE);
        }
        for (ChannelInfo channelInfo : ALARM_CHANNELS) {
            if (channelInfo.propertyName.equals(property)) {
                return Set.of(channelInfo);
            }
        }
        return Set.of();
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        String armStateValue = null;
        Boolean burglaryAlarmValue = null;
        Boolean carbonMonoxideAlarmValue = null;
        Boolean fireAlarmValue = null;
        Boolean waterAlarmValue = null;
        for (JsonObject state : stateList) {
            if (ARM_STATE.propertyName.equals(state.get("name").getAsString())) {
                if (armStateValue == null) {
                    armStateValue = state.get("value").getAsString();
                }
            } else if (BURGLARY_ALARM.propertyName.equals(state.get("name").getAsString())) {
                if (burglaryAlarmValue == null) {
                    burglaryAlarmValue = "ALARM".equals(state.get("value").getAsString());
                }
            } else if (CARBON_MONOXIDE_ALARM.propertyName.equals(state.get("name").getAsString())) {
                if (carbonMonoxideAlarmValue == null) {
                    carbonMonoxideAlarmValue = "ALARM".equals(state.get("value").getAsString());
                }
            } else if (FIRE_ALARM.propertyName.equals(state.get("name").getAsString())) {
                if (fireAlarmValue == null) {
                    fireAlarmValue = "ALARM".equals(state.get("value").getAsString());
                }
            } else if (WATER_ALARM.propertyName.equals(state.get("name").getAsString())) {
                if (waterAlarmValue == null) {
                    waterAlarmValue = "ALARM".equals(state.get("value").getAsString());
                }
            }
        }
        smartHomeDeviceHandler.updateState(ARM_STATE.channelId,
                armStateValue == null ? UnDefType.UNDEF : new StringType(armStateValue));
        smartHomeDeviceHandler.updateState(BURGLARY_ALARM.channelId, burglaryAlarmValue == null ? UnDefType.UNDEF
                : (burglaryAlarmValue ? OpenClosedType.CLOSED : OpenClosedType.OPEN));
        smartHomeDeviceHandler.updateState(CARBON_MONOXIDE_ALARM.channelId,
                carbonMonoxideAlarmValue == null ? UnDefType.UNDEF
                        : (carbonMonoxideAlarmValue ? OpenClosedType.CLOSED : OpenClosedType.OPEN));
        smartHomeDeviceHandler.updateState(FIRE_ALARM.channelId, fireAlarmValue == null ? UnDefType.UNDEF
                : (fireAlarmValue ? OpenClosedType.CLOSED : OpenClosedType.OPEN));
        smartHomeDeviceHandler.updateState(WATER_ALARM.channelId, waterAlarmValue == null ? UnDefType.UNDEF
                : (waterAlarmValue ? OpenClosedType.CLOSED : OpenClosedType.OPEN));
    }

    @Override
    public boolean handleCommand(Connection connection, JsonSmartHomeDevice shd, String entityId,
            List<JsonSmartHomeCapability> capabilities, String channelId, Command command)
            throws IOException, InterruptedException {
        if (channelId.equals(ARM_STATE.channelId)) {
            if (containsCapabilityProperty(capabilities, ARM_STATE.propertyName)) {
                if (command instanceof StringType) {
                    String armStateValue = command.toFullString();
                    if (!armStateValue.isEmpty()) {
                        connection.smartHomeCommand(entityId, "controlSecurityPanel",
                                Map.of(ARM_STATE.propertyName, armStateValue));
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
