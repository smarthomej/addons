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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapability;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeCapability.Properties;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevice;

import com.google.gson.JsonObject;

/**
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractInterfaceHandler implements InterfaceHandler {
    private final Logger logger = LoggerFactory.getLogger(AbstractInterfaceHandler.class);

    private final List<String> interfaces;

    protected SmartHomeDeviceHandler smartHomeDeviceHandler;
    protected Map<String, ChannelInfo> channels = new HashMap<>();

    public AbstractInterfaceHandler(SmartHomeDeviceHandler smartHomeDeviceHandler, List<String> interfaces) {
        this.smartHomeDeviceHandler = smartHomeDeviceHandler;
        this.interfaces = interfaces;
    }

    @Override
    public @Nullable List<CommandOption> getCommandDescription(ChannelInfo channelInfo) {
        // return null if not used
        return null;
    }

    protected abstract Set<ChannelInfo> findChannelInfos(JsonSmartHomeCapability capability, String property);

    public abstract void updateChannels(String interfaceName, List<JsonObject> stateList, UpdateChannelResult result);

    public abstract boolean handleCommand(Connection connection, JsonSmartHomeDevice shd, String entityId,
            List<JsonSmartHomeCapability> capabilities, String channelId, Command command)
            throws IOException, InterruptedException;

    public boolean hasChannel(String channelId) {
        return channels.containsKey(channelId);
    }

    public List<String> getSupportedInterface() {
        return interfaces;
    }

    SmartHomeDeviceHandler getSmartHomeDeviceHandler() {
        return smartHomeDeviceHandler;
    }

    public Collection<ChannelInfo> initialize(List<JsonSmartHomeCapability> capabilities) {
        // TODO: reduce or remove
        Map<String, ChannelInfo> channels = new HashMap<>();
        for (JsonSmartHomeCapability capability : capabilities) {
            Properties properties = capability.properties;
            if (properties != null) {
                List<Properties.Property> supported = Objects.requireNonNullElse(properties.supported, List.of());
                for (Properties.Property property : supported) {
                    String name = property.name;
                    if (name != null) {
                        findChannelInfos(capability, name).forEach(c -> channels.put(c.channelId, c));
                    }
                }
            }
        }
        logger.trace("Handler '{}' has capabilities '{}' and uses channels '{}'", capabilities,
                smartHomeDeviceHandler.getId(), channels);
        this.channels = channels;
        return channels.values();
    }

    protected boolean containsCapabilityProperty(List<JsonSmartHomeCapability> capabilities, String propertyName) {
        for (JsonSmartHomeCapability capability : capabilities) {
            Properties properties = capability.properties;
            if (properties != null) {
                List<Properties.Property> supported = Objects.requireNonNullElse(properties.supported, List.of());
                if (supported.stream().anyMatch(p -> propertyName.equals(p.name))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateState(String channelId, State state) {
        getSmartHomeDeviceHandler().updateState(channelId, state);
    }
}
