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
package org.smarthomej.binding.amazonechocontrol.internal.handler;

import static org.smarthomej.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.DEVICE_PROPERTY_ID;
import static org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants.SUPPORTED_INTERFACES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapabilities.SmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevices.SmartHomeDevice;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeGroupIdentifiers;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeGroupIdentity;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeGroups.SmartHomeGroup;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeTags;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.SmartHomeBaseDevice;
import org.smarthomej.binding.amazonechocontrol.internal.smarthome.ChannelInfo;
import org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants;
import org.smarthomej.binding.amazonechocontrol.internal.smarthome.InterfaceHandler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Lukas Knoeller - Initial contribution
 */
@NonNullByDefault
public class SmartHomeDeviceHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(SmartHomeDeviceHandler.class);

    private @Nullable SmartHomeBaseDevice smartHomeBaseDevice;
    private final Gson gson;
    private final Map<String, InterfaceHandler> interfaceHandlers = new HashMap<>();
    private final Map<String, JsonArray> lastStates = new HashMap<>();
    private String deviceId = "";

    public SmartHomeDeviceHandler(Thing thing, Gson gson) {
        super(thing);
        this.gson = gson;
    }

    public synchronized void setDeviceAndUpdateThingState(AccountHandler accountHandler,
            @Nullable SmartHomeBaseDevice smartHomeBaseDevice) {
        if (smartHomeBaseDevice == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Can't find smartHomeBaseDevice");
            return;
        }

        boolean changed = this.smartHomeBaseDevice == null;
        this.smartHomeBaseDevice = smartHomeBaseDevice;

        Set<String> unusedChannels = new HashSet<>();
        thing.getChannels().forEach(channel -> unusedChannels.add(channel.getUID().getId()));

        Set<String> unusedHandlers = new HashSet<>(interfaceHandlers.keySet());

        Map<String, List<SmartHomeCapability>> capabilities = getCapabilities(accountHandler, smartHomeBaseDevice);

        ThingBuilder thingBuilder = editThing();

        for (Map.Entry<String, List<SmartHomeCapability>> capability : capabilities.entrySet()) {
            String interfaceName = capability.getKey();
            InterfaceHandler handler = interfaceHandlers.get(interfaceName);
            if (handler != null) {
                unusedHandlers.remove(interfaceName);
            } else {
                Function<SmartHomeDeviceHandler, InterfaceHandler> creator = Constants.HANDLER_FACTORY
                        .get(interfaceName);
                if (creator != null) {
                    handler = creator.apply(this);
                    interfaceHandlers.put(interfaceName, handler);
                }
            }
            if (handler != null) {
                Collection<ChannelInfo> required = handler.initialize(capability.getValue());
                ThingHandlerCallback callback = getCallback();
                if (callback == null) {
                    logger.warn("Trying to modify {} but no callback present.", thing.getUID());
                    return;
                }
                for (ChannelInfo channelInfo : required) {
                    unusedChannels.remove(channelInfo.channelId);
                    if (addChannelToDevice(thingBuilder, callback, channelInfo)) {
                        changed = true;
                    }
                }
            }
        }

        unusedHandlers.forEach(interfaceHandlers::remove);
        if (!unusedChannels.isEmpty()) {
            changed = true;
            unusedChannels.forEach(channelId -> thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelId)));
        }

        if (changed) {
            updateThing(thingBuilder.build());
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "Thing has changed.");
            accountHandler.forceDelayedSmartHomeStateUpdate(deviceId);
        }
    }

    @Override
    public void updateState(String channelId, State state) {
        super.updateState(channelId, state);
    }

    @Override
    public void initialize() {
        deviceId = Objects.requireNonNullElse((String) getConfig().get(DEVICE_PROPERTY_ID), "");
        if (deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "id not set");
            return;
        }

        AccountHandler accountHandler = getAccountHandler();
        if (accountHandler != null) {
            accountHandler.addSmartHomeDeviceHandler(this);
            setDeviceAndUpdateThingState(accountHandler, smartHomeBaseDevice);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridgehandler not found");
        }
    }

    @Override
    public void dispose() {
        AccountHandler accountHandler = getAccountHandler();
        if (accountHandler != null) {
            accountHandler.removeSmartHomeDeviceHandler(this);
        }
    }

    public String getId() {
        return deviceId;
    }

    private boolean addChannelToDevice(ThingBuilder thingBuilder, ThingHandlerCallback callback,
            ChannelInfo channelInfo) {
        Channel channel = thing.getChannel(channelInfo.channelId);
        if (channel != null) {
            if (channelInfo.channelTypeUID.equals(channel.getChannelTypeUID())) {
                // channel exist with the same settings
                return false;
            }
            // channel exist with other settings, remove it first
            thingBuilder.withoutChannel(channel.getUID());
        }

        ChannelBuilder channelBuilder = callback.createChannelBuilder(
                new ChannelUID(thing.getUID(), channelInfo.channelId), channelInfo.channelTypeUID);
        String label = channelInfo.label;
        if (label != null) {
            channelBuilder.withLabel(label);
        }
        thingBuilder.withChannel(channelBuilder.build());

        return true;
    }

    public void updateChannelStates(List<SmartHomeBaseDevice> allDevices,
            Map<String, JsonArray> applianceIdToCapabilityStates) {
        logger.trace("Updating allDevices={} with states={}", allDevices, applianceIdToCapabilityStates);
        AccountHandler accountHandler = getAccountHandler();
        SmartHomeBaseDevice smartHomeBaseDevice = this.smartHomeBaseDevice;
        if (smartHomeBaseDevice == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Can't find smartHomeBaseDevice!");
            return;
        }

        Map<String, List<JsonObject>> mapInterfaceToStates = new HashMap<>();
        Set<SmartHomeDevice> smartHomeDevices = getSupportedSmartHomeDevices(smartHomeBaseDevice, allDevices);
        logger.trace("Search for smartHomeBaseDevice='{}' resulted in '{}'", smartHomeBaseDevice, smartHomeDevices);
        if (smartHomeDevices.isEmpty()) {
            logger.debug("Did not find a supported smartHomeDevice.");
            return;
        }

        for (SmartHomeDevice smartHomeDevice : smartHomeDevices) {
            String applianceId = smartHomeDevice.applianceId;
            logger.trace("applianceId={}, group={}, keys={}", applianceId, smartHomeDevice.isGroup(),
                    applianceIdToCapabilityStates.keySet());
            if (applianceId == null) {
                logger.debug("applianceId is null in smartHomeDevice={}", smartHomeDevice);
                continue;
            }
            JsonArray states = applianceIdToCapabilityStates.getOrDefault(applianceId,
                    lastStates.getOrDefault(applianceId, new JsonArray()));
            if (states.size() == 0) {
                logger.trace("No states array found for applianceId={}.", applianceId);
                continue;
            }
            if (smartHomeBaseDevice.isGroup()) {
                // for groups, store the last state of all devices
                lastStates.put(applianceId, states);
            }
            logger.trace("Found states array={} for applianceId={}", states, applianceId);

            for (JsonElement stateElement : states) {
                String stateJson = stateElement.getAsString();
                if (stateJson.startsWith("{") && stateJson.endsWith("}")) {
                    JsonObject state = Objects.requireNonNull(gson.fromJson(stateJson, JsonObject.class));
                    JsonElement interfaceName = state.get("namespace");
                    if (interfaceName != null) {
                        Objects.requireNonNull(mapInterfaceToStates.computeIfAbsent(interfaceName.getAsString(),
                                k -> new ArrayList<>())).add(state);
                    }
                }
            }
        }

        if (mapInterfaceToStates.isEmpty()) {
            logger.trace("Found no matching states.");
            return;
        }
        logger.trace("mapInterfaceToState='{}'", mapInterfaceToStates);

        for (InterfaceHandler interfaceHandler : interfaceHandlers.values()) {
            InterfaceHandler.UpdateChannelResult result = new InterfaceHandler.UpdateChannelResult();
            for (String interfaceName : interfaceHandler.getSupportedInterface()) {
                List<JsonObject> stateList = mapInterfaceToStates.get(interfaceName);
                if (stateList != null) {
                    try {
                        interfaceHandler.updateChannels(interfaceName, stateList, result);
                    } catch (RuntimeException e) {
                        // We catch all exceptions, otherwise all other things are not updated!
                        logger.debug("Updating states failed", e);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                "RuntimeException while processing updates");
                    }
                }
            }

            if (result.needSingleUpdate && smartHomeBaseDevice instanceof SmartHomeDevice && accountHandler != null) {
                SmartHomeDevice shd = (SmartHomeDevice) smartHomeBaseDevice;
                String applianceId = shd.applianceId;
                if (applianceId != null) {
                    accountHandler.forceDelayedSmartHomeStateUpdate(applianceId);
                }
            }
        }

        updateStatus(ThingStatus.ONLINE);
    }

    private @Nullable AccountHandler getAccountHandler() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            BridgeHandler bridgeHandler = bridge.getHandler();
            if (bridgeHandler instanceof AccountHandler) {
                return (AccountHandler) bridgeHandler;
            }
        }

        return null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        AccountHandler accountHandler = getAccountHandler();
        if (accountHandler == null) {
            logger.debug("accountHandler is null in {}", thing.getUID());
            return;
        }
        Connection connection = accountHandler.findConnection();
        if (connection == null) {
            logger.debug("connection is null in {}", thing.getUID());
            return;
        }

        try {
            if (command instanceof RefreshType) {
                accountHandler.forceDelayedSmartHomeStateUpdate(deviceId);
                return;
            }
            SmartHomeBaseDevice smartHomeBaseDevice = this.smartHomeBaseDevice;
            if (smartHomeBaseDevice == null) {
                logger.debug("smartHomeBaseDevice is null in {}", thing.getUID());
                return;
            }
            Set<SmartHomeDevice> devices = getSupportedSmartHomeDevices(smartHomeBaseDevice,
                    accountHandler.getLastKnownSmartHomeDevices());
            String channelId = channelUID.getId();

            for (InterfaceHandler interfaceHandler : interfaceHandlers.values()) {
                if (!interfaceHandler.hasChannel(channelId)) {
                    continue;
                }
                for (SmartHomeDevice shd : devices) {
                    String entityId = shd.entityId;
                    if (entityId == null) {
                        continue;
                    }
                    accountHandler.forceDelayedSmartHomeStateUpdate(getId()); // block updates
                    if (interfaceHandler.handleCommand(connection, shd, entityId, shd.getCapabilities(),
                            channelUID.getId(), command)) {
                        accountHandler.forceDelayedSmartHomeStateUpdate(getId()); // force update again to restart
                        // update timer
                        logger.debug("Command {} sent to {}", command, shd.findId());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Handle command failed", e);
        }
    }

    private Map<String, List<SmartHomeCapability>> getCapabilities(AccountHandler accountHandler,
            SmartHomeBaseDevice device) {
        Map<String, List<SmartHomeCapability>> capabilities = new HashMap<>();
        if (device instanceof SmartHomeDevice) {
            SmartHomeDevice shd = (SmartHomeDevice) device;
            for (SmartHomeCapability capability : shd.getCapabilities()) {
                String interfaceName = capability.interfaceName;
                if (interfaceName != null) {
                    Objects.requireNonNull(capabilities.computeIfAbsent(interfaceName, name -> new ArrayList<>()))
                            .add(capability);
                }
            }
        } else if (device instanceof SmartHomeGroup) {
            for (SmartHomeDevice shd : getSupportedSmartHomeDevices(device,
                    accountHandler.getLastKnownSmartHomeDevices())) {
                getCapabilities(accountHandler, shd).forEach((interfaceName, caps) -> Objects
                        .requireNonNull(capabilities.computeIfAbsent(interfaceName, name -> new ArrayList<>()))
                        .addAll(caps));
            }
        }

        return capabilities;
    }

    public static Set<SmartHomeDevice> getSupportedSmartHomeDevices(@Nullable SmartHomeBaseDevice baseDevice,
            List<SmartHomeBaseDevice> allDevices) {
        if (baseDevice == null) {
            return Set.of();
        }
        Set<SmartHomeDevice> result = new HashSet<>();
        if (baseDevice instanceof SmartHomeDevice) {
            SmartHomeDevice shd = (SmartHomeDevice) baseDevice;
            if (shd.getCapabilities().stream().map(capability -> capability.interfaceName)
                    .anyMatch(SUPPORTED_INTERFACES::contains)) {
                result.add(shd);
            }
        } else {
            SmartHomeGroup shg = (SmartHomeGroup) baseDevice;
            for (SmartHomeBaseDevice device : allDevices) {
                if (device instanceof SmartHomeDevice) {
                    SmartHomeDevice shd = (SmartHomeDevice) device;
                    JsonSmartHomeTags.JsonSmartHomeTag tags = shd.tags;
                    if (tags != null) {
                        JsonSmartHomeGroupIdentity.SmartHomeGroupIdentity tagNameToValueSetMap = tags.tagNameToValueSetMap;
                        JsonSmartHomeGroupIdentifiers.SmartHomeGroupIdentifier applianceGroupIdentifier = shg.applianceGroupIdentifier;
                        if (tagNameToValueSetMap != null) {
                            List<String> groupIdentity = Objects.requireNonNullElse(tagNameToValueSetMap.groupIdentity,
                                    List.of());
                            if (applianceGroupIdentifier != null && applianceGroupIdentifier.value != null
                                    && groupIdentity.contains(applianceGroupIdentifier.value)) {
                                if (shd.getCapabilities().stream().map(capability -> capability.interfaceName)
                                        .anyMatch(SUPPORTED_INTERFACES::contains)) {
                                    result.add(shd);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
