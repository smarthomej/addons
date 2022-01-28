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
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.DefaultSystemChannelTypeProvider;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link HandlerEndpointHealth} is responsible for the Alexa.EndpointHealth interface
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class HandlerEndpointHealth extends AbstractInterfaceHandler {
    public static final String INTERFACE = "Alexa.EndpointHealth";

    private static final ChannelInfo LOW_BATTERY_STATE = new ChannelInfo("batteryState", "batteryState",
            DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_LOW_BATTERY);
    private static final ChannelInfo CONNECTIVITY_STATE = new ChannelInfo("connectivity", "connectivity",
            Constants.CHANNEL_TYPE_CONNECTIVITY);

    private final Logger logger = LoggerFactory.getLogger(HandlerEndpointHealth.class);

    public HandlerEndpointHealth(SmartHomeDeviceHandler smartHomeDeviceHandler) {
        super(smartHomeDeviceHandler, List.of(INTERFACE));
    }

    @Override
    protected Set<ChannelInfo> findChannelInfos(JsonSmartHomeCapability capability, @Nullable String property) {
        if (LOW_BATTERY_STATE.propertyName.equals(property)) {
            return Set.of(LOW_BATTERY_STATE);
        } else if (CONNECTIVITY_STATE.propertyName.equals(property)) {
            return Set.of(CONNECTIVITY_STATE);
        }
        return Set.of();
    }

    @Override
    public void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result) {
        for (JsonObject state : stateList) {
            String stateName = state.get("name").getAsString();
            if (LOW_BATTERY_STATE.propertyName.equals(stateName)) {
                JsonObject batteryHealthObject = state.getAsJsonObject("value");
                if (batteryHealthObject != null) {
                    // first try if we have health
                    JsonObject healthObject = batteryHealthObject.getAsJsonObject("health");
                    if (healthObject != null) {
                        String status = healthObject.get("state").getAsString();
                        updateState(LOW_BATTERY_STATE.channelId, OnOffType.from("OK".equals(status)));
                        continue;
                    }
                    // try if we know the percentage
                    JsonElement levelPercentage = batteryHealthObject.get("levelPercentage");
                    if (levelPercentage != null) {
                        updateState(LOW_BATTERY_STATE.channelId, OnOffType.from(levelPercentage.getAsInt() < 10));
                        continue;
                    }
                    // we don't know the state
                    updateState(LOW_BATTERY_STATE.channelId, UnDefType.UNDEF);
                }
            } else if (CONNECTIVITY_STATE.propertyName.equals(stateName)) {
                JsonObject connectivityValueObject = state.get("value").getAsJsonObject();
                if (connectivityValueObject != null) {
                    String connectivityValue = connectivityValueObject.get("value").getAsString();
                    if ("OK".equals(connectivityValue)) {
                        updateState(CONNECTIVITY_STATE.channelId, new StringType(connectivityValue));
                    } else if (connectivityValue != null) {
                        String connectivityReason = "UNKNOWN";
                        if (connectivityValueObject.has("reason")) {
                            connectivityReason = connectivityValueObject.get("reason").getAsString();
                        }
                        updateState(CONNECTIVITY_STATE.channelId,
                                new StringType(connectivityValue + " / " + connectivityReason));
                    } else {
                        updateState(CONNECTIVITY_STATE.channelId, UnDefType.UNDEF);
                    }
                }
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
