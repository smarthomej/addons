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
package org.smarthomej.automation.javarule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * The {@link JavaRuleBusEvent} is a wrapper class for the hidden
 * {@link org.openhab.core.automation.module.script.internal.defaultscope.ScriptBusEvent}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JavaRuleBusEvent {
    private final Object events;

    public JavaRuleBusEvent(Object events) {
        this.events = events;
    }

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param commandString the command to send
     */
    public @Nullable Object sendCommand(@Nullable Item item, @Nullable String commandString) {
        if (item != null && commandString != null) {
            return sendCommand(item.getName(), commandString);
        } else {
            return null;
        }
    }

    /**
     * Sends a number as a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param number the number to send as a command
     */
    public @Nullable Object sendCommand(@Nullable Item item, @Nullable Number number) {
        if (item != null && number != null) {
            return sendCommand(item.getName(), number.toString());
        } else {
            return null;
        }
    }

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the command to
     * @param command the command to send
     */
    public @Nullable Object sendCommand(@Nullable String itemName, @Nullable Command command) {
        if (itemName != null && command != null) {
            return sendCommand(itemName, command.toFullString());
        } else {
            return null;
        }
    }

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the command to
     * @param commandString the command to send
     */
    public @Nullable Object sendCommand(@Nullable String itemName, @Nullable String commandString) {
        if (itemName == null || commandString == null) {
            return null;
        }
        try {
            Method sendCommand = events.getClass().getMethod("sendCommand", String.class, String.class);
            return sendCommand.invoke(events, itemName, commandString);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     */
    public @Nullable Object sendCommand(@Nullable Item item, @Nullable Command command) {
        if (item == null || command == null) {
            return null;
        }
        try {
            Method sendCommand = events.getClass().getMethod("sendCommand", Item.class, Command.class);
            return sendCommand.invoke(events, item, command);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item as a number
     */
    public @Nullable Object postUpdate(@Nullable Item item, @Nullable Number state) {
        if (item != null && state != null) {
            return postUpdate(item.getName(), state.toString());
        } else {
            return null;
        }
    }

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param stateAsString the new state of the item
     */
    public @Nullable Object postUpdate(@Nullable Item item, @Nullable String stateAsString) {
        if (item != null && stateAsString != null) {
            return postUpdate(item.getName(), stateAsString);
        } else {
            return null;
        }
    }

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param itemName the item to send the status update for
     * @param state the new state of the item
     */
    public @Nullable Object postUpdate(@Nullable String itemName, @Nullable State state) {
        if (itemName != null && state != null) {
            return postUpdate(itemName, state.toFullString());
        } else {
            return null;
        }
    }

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the status update for
     * @param stateString the new state of the item
     */
    public @Nullable Object postUpdate(@Nullable String itemName, @Nullable String stateString) {
        if (itemName == null || stateString == null) {
            return null;
        }
        try {
            Method postUpdate = events.getClass().getMethod("postUpdate", String.class, String.class);
            return postUpdate.invoke(events, itemName, stateString);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    /**
     * Posts a status update for a specified item to the event bus.
     * t
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     */
    public @Nullable Object postUpdate(@Nullable Item item, @Nullable State state) {
        if (item == null || state == null) {
            return null;
        }
        try {
            Method postUpdate = events.getClass().getMethod("postUpdate", Item.class, State.class);
            return postUpdate.invoke(events, item, state);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    /**
     * Stores the current states for a list of items in a map.
     * A group item is not itself put into the map, but instead all its members.
     *
     * @param items the items for which the state should be stored
     * @return the map of items with their states
     */
    public Map<Item, State> storeStates(Item @Nullable... items) {
        Map<Item, State> statesMap = new HashMap<>();
        if (items != null) {
            for (Item item : items) {
                if (item instanceof GroupItem) {
                    GroupItem groupItem = (GroupItem) item;
                    for (Item member : groupItem.getAllMembers()) {
                        statesMap.put(member, member.getState());
                    }
                } else {
                    statesMap.put(item, item.getState());
                }
            }
        }
        return statesMap;
    }

    /**
     * Restores item states from a map.
     * If the saved state can be interpreted as a command, a command is sent for the item
     * (and the physical device can send a status update if occurred). If it is no valid
     * command, the item state is directly updated to the saved value.
     *
     * @param statesMap a map with ({@link Item}, {@link State}) entries
     * @return null
     */
    public @Nullable Object restoreStates(@Nullable Map<Item, State> statesMap) {
        if (statesMap != null) {
            for (Map.Entry<Item, State> entry : statesMap.entrySet()) {
                if (entry.getValue() instanceof Command) {
                    sendCommand(entry.getKey(), (Command) entry.getValue());
                } else {
                    postUpdate(entry.getKey(), entry.getValue());
                }
            }
        }
        return null;
    }
}
