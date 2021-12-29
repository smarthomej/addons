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
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapabilities.SmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevices.SmartHomeDevice;

import com.google.gson.JsonObject;

/**
 * The {@link HandlerPercentageController} is responsible for the Alexa.PowerControllerInterface
 *
 * @author Lukas Knoeller - Initial contribution
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class HandlerPercentageController extends AbstractInterfaceHandler {
    public static final String INTERFACE = "Alexa.PercentageController";

    private static final ChannelInfo PERCENTAGE = new ChannelInfo("percentage", "percentage",
            Constants.CHANNEL_TYPE_PERCENTAGE);

    private @Nullable Integer lastPercentage;

    public HandlerPercentageController(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler, List.of(INTERFACE));
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(SmartHomeCapability capability, @Nullable String property) {
        if (PERCENTAGE.propertyName.equals(property)) {
            return Set.of(PERCENTAGE);
        }
        return Set.of();
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        Integer percentageValue = null;
        for (JsonObject state : stateList) {
            if (PERCENTAGE.propertyName.equals(state.get("name").getAsString())) {
                int value = state.get("value").getAsInt();
                // For groups take the maximum
                if (percentageValue == null) {
                    percentageValue = value;
                } else if (value > percentageValue) {
                    percentageValue = value;
                }
            }
        }
        if (percentageValue != null) {
            lastPercentage = percentageValue;
        }
        smartHomeDeviceHandler.updateState(PERCENTAGE.channelId,
                percentageValue == null ? UnDefType.UNDEF : new PercentType(percentageValue));
    }

    @Override
    public boolean handleCommand(Connection connection, SmartHomeDevice shd, String entityId,
            List<SmartHomeCapability> capabilities, String channelId, Command command)
            throws IOException, InterruptedException {
        if (channelId.equals(PERCENTAGE.channelId)) {
            if (containsCapabilityProperty(capabilities, PERCENTAGE.propertyName)) {
                if (command.equals(IncreaseDecreaseType.INCREASE)) {
                    Integer lastPercentage = this.lastPercentage;
                    if (lastPercentage != null) {
                        int newValue = lastPercentage++;
                        if (newValue > 100) {
                            newValue = 100;
                        }
                        this.lastPercentage = newValue;
                        connection.smartHomeCommand(entityId, "setPercentage",
                                Map.of(PERCENTAGE.propertyName, newValue));
                        return true;
                    }
                } else if (command.equals(IncreaseDecreaseType.DECREASE)) {
                    Integer lastPercentage = this.lastPercentage;
                    if (lastPercentage != null) {
                        int newValue = lastPercentage--;
                        if (newValue < 0) {
                            newValue = 0;
                        }
                        this.lastPercentage = newValue;
                        connection.smartHomeCommand(entityId, "setPercentage",
                                Map.of(PERCENTAGE.propertyName, newValue));
                        return true;
                    }
                } else if (command.equals(OnOffType.OFF)) {
                    lastPercentage = 0;
                    connection.smartHomeCommand(entityId, "setPercentage", Map.of(PERCENTAGE.propertyName, 0));
                    return true;
                } else if (command.equals(OnOffType.ON)) {
                    lastPercentage = 100;
                    connection.smartHomeCommand(entityId, "setPercentage", Map.of(PERCENTAGE.propertyName, 100));
                    return true;
                } else if (command instanceof PercentType) {
                    Integer lastPercentage = ((PercentType) command).intValue();
                    connection.smartHomeCommand(entityId, "setPercentage",
                            Map.of(PERCENTAGE.propertyName, lastPercentage));
                    this.lastPercentage = lastPercentage;
                    return true;
                }
            }
        }
        return false;
    }
}
