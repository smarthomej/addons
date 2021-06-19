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
import java.util.Locale;
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
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapabilities.SmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevices.SmartHomeDevice;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeGroupIdentifiers;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeGroupIdentity;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeGroups.SmartHomeGroup;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeTags;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.SmartHomeBaseDevice;
import org.smarthomej.binding.amazonechocontrol.internal.smarthome.Constants;
import org.smarthomej.binding.amazonechocontrol.internal.smarthome.HandlerBase;
import org.smarthomej.binding.amazonechocontrol.internal.smarthome.HandlerBase.ChannelInfo;
import org.smarthomej.binding.amazonechocontrol.internal.smarthome.HandlerBase.UpdateChannelResult;

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
    private final Map<String, HandlerBase> handlers = new HashMap<>();
    private final Map<String, JsonArray> lastStates = new HashMap<>();

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

        Set<String> unusedHandlers = new HashSet<>(handlers.keySet());

        Map<String, List<SmartHomeCapability>> capabilities = new HashMap<>();
        getCapabilities(capabilities, accountHandler, smartHomeBaseDevice);

        ThingBuilder thingBuilder = editThing();

        for (Map.Entry<String, List<SmartHomeCapability>> capability : capabilities.entrySet()) {
            String interfaceName = capability.getKey();
            HandlerBase handler = handlers.get(interfaceName);
            if (handler != null) {
                unusedHandlers.remove(interfaceName);
            } else {
                Function<SmartHomeDeviceHandler, HandlerBase> creator = Constants.HANDLER_FACTORY.get(interfaceName);
                if (creator != null) {
                    handler = creator.apply(this);
                    handlers.put(interfaceName, handler);
                }
            }
            if (handler != null) {
                Collection<ChannelInfo> required = handler.initialize(capability.getValue());
                for (ChannelInfo channelInfo : required) {
                    unusedChannels.remove(channelInfo.channelId);
                    if (addChannelToDevice(thingBuilder, channelInfo.channelId, channelInfo.itemType,
                            channelInfo.channelTypeUID)) {
                        changed = true;
                    }
                }
            }
        }

        unusedHandlers.forEach(handlers::remove);
        if (!unusedChannels.isEmpty()) {
            changed = true;
            unusedChannels.stream().map(id -> new ChannelUID(thing.getUID(), id)).forEach(thingBuilder::withoutChannel);
        }

        if (changed) {
            updateThing(thingBuilder.build());
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "Thing has changed.");
            accountHandler.forceDelayedSmartHomeStateUpdate(getId());
        }
    }

    public String getId() {
        return Objects.requireNonNullElse((String) getConfig().get(DEVICE_PROPERTY_ID), "");
    }

    @Override
    public void updateState(String channelId, State state) {
        super.updateState(new ChannelUID(thing.getUID(), channelId), state);
    }

    @Override
    public void initialize() {
        AccountHandler accountHandler = getAccountHandler();
        if (accountHandler != null) {
            accountHandler.addSmartHomeDeviceHandler(this);
            setDeviceAndUpdateThingState(accountHandler, smartHomeBaseDevice);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridgehandler not found");
        }
    }

    private boolean addChannelToDevice(ThingBuilder thingBuilder, String channelId, String itemType,
            ChannelTypeUID channelTypeUID) {
        Channel channel = thing.getChannel(channelId);
        if (channel != null) {
            if (channelTypeUID.equals(channel.getChannelTypeUID()) && itemType.equals(channel.getAcceptedItemType())) {
                // channel exist with the same settings
                return false;
            }
            // channel exist with other settings, remove it first
            thingBuilder.withoutChannel(channel.getUID());
        }
        thingBuilder.withChannel(ChannelBuilder.create(new ChannelUID(thing.getUID(), channelId), itemType)
                .withType(channelTypeUID).build());
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

        for (HandlerBase handlerBase : handlers.values()) {
            UpdateChannelResult result = new UpdateChannelResult();
            for (String interfaceName : handlerBase.getSupportedInterface()) {
                List<JsonObject> stateList = mapInterfaceToStates.get(interfaceName);
                if (stateList != null) {
                    try {
                        handlerBase.updateChannels(interfaceName, stateList, result);
                    } catch (RuntimeException e) {
                        // We catch all exceptions, otherwise all other things are not updated!
                        logger.debug("Updating states failed", e);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                e.getLocalizedMessage());
                    }
                }
            }

            if (result.needSingleUpdate && smartHomeBaseDevice instanceof SmartHomeDevice && accountHandler != null) {
                SmartHomeDevice shd = (SmartHomeDevice) smartHomeBaseDevice;
                accountHandler.forceDelayedSmartHomeStateUpdate(shd.findId());
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
                accountHandler.forceDelayedSmartHomeStateUpdate(getId());
                return;
            }
            SmartHomeBaseDevice smartHomeBaseDevice = this.smartHomeBaseDevice;
            if (smartHomeBaseDevice == null) {
                logger.debug("smarthomeBaseDevice is null in {}", thing.getUID());
                return;
            }
            Set<SmartHomeDevice> devices = getSupportedSmartHomeDevices(smartHomeBaseDevice,
                    accountHandler.getLastKnownSmartHomeDevices());
            String channelId = channelUID.getId();

            for (HandlerBase handlerBase : handlers.values()) {
                if (!handlerBase.hasChannel(channelId)) {
                    continue;
                }
                for (SmartHomeDevice shd : devices) {
                    String entityId = shd.entityId;
                    if (entityId == null) {
                        continue;
                    }
                    accountHandler.forceDelayedSmartHomeStateUpdate(getId()); // block updates
                    if (handlerBase.handleCommand(connection, shd, entityId, shd.getCapabilities(), channelUID.getId(),
                            command)) {
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

    private void getCapabilities(Map<String, List<SmartHomeCapability>> result, AccountHandler accountHandler,
            SmartHomeBaseDevice device) {
        if (device instanceof SmartHomeDevice) {
            SmartHomeDevice shd = (SmartHomeDevice) device;
            for (SmartHomeCapability capability : shd.getCapabilities()) {
                String interfaceName = capability.interfaceName;
                if (interfaceName != null) {
                    Objects.requireNonNull(result.computeIfAbsent(interfaceName, name -> new ArrayList<>()))
                            .add(capability);
                }
            }
        }
        if (device instanceof SmartHomeGroup) {
            for (SmartHomeDevice shd : getSupportedSmartHomeDevices(device,
                    accountHandler.getLastKnownSmartHomeDevices())) {
                getCapabilities(result, accountHandler, shd);
            }
        }
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

    public @Nullable StateDescription findStateDescription(Channel channel, StateDescription originalStateDescription,
            @Nullable Locale locale) {
        String channelId = channel.getUID().getId();
        for (HandlerBase handler : handlers.values()) {
            if (handler.hasChannel(channelId)) {
                return handler.findStateDescription(channelId, originalStateDescription, locale);
            }
        }
        return null;
    }
}
