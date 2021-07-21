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
package org.smarthomej.automation.javarule.rules;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JavaRule} is the base class for Java based rules
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class JavaRule {

    private final Logger logger = LoggerFactory.getLogger(JavaRule.class);

    private @NonNullByDefault({}) EventPublisher eventPublisher;

    // make some members available to users
    protected @NonNullByDefault({}) ScheduledExecutorService scheduler;
    protected @NonNullByDefault({}) ItemRegistry itemRegistry;
    protected @NonNullByDefault({}) ThingRegistry things;

    /**
     * send a command to an item
     *
     * @param itemName
     * @param command
     */
    @SuppressWarnings("unused")
    protected void sendCommand(String itemName, @Nullable Command command) {
        if (command != null) {
            ItemCommandEvent commandEvent = ItemEventFactory.createCommandEvent(itemName, command);
            eventPublisher.post(commandEvent);
        }
    }

    /**
     * send a command to an item
     *
     * @param item
     * @param command
     */
    @SuppressWarnings("unused")
    protected void sendCommand(Item item, @Nullable Command command) {
        sendCommand(item.getName(), command);
    }

    /**
     * post an update to an item
     *
     * @param itemName
     * @param state
     */
    @SuppressWarnings("unused")
    protected void postUpdate(String itemName, @Nullable State state) {
        if (state != null) {
            ItemEvent itemEvent = ItemEventFactory.createStateEvent(itemName, state);
            eventPublisher.post(itemEvent);
        }
    }

    /**
     * post an update to an item
     *
     * @param item
     * @param state
     */
    @SuppressWarnings("unused")
    protected void postUpdate(Item item, @Nullable State state) {
        postUpdate(item.getName(), state);
    }

    /**
     * Sets context (EventPublisher, registries, scheduler) for this rule
     *
     * Only to be used internally!
     *
     * @param eventHandler
     * @param itemRegistry
     * @param thingRegistry
     * @param scheduler
     */
    public final void setExecutionContext(EventPublisher eventHandler, ItemRegistry itemRegistry,
            ThingRegistry thingRegistry, ScheduledExecutorService scheduler) {
        this.eventPublisher = eventHandler;

        this.scheduler = scheduler;
        this.itemRegistry = itemRegistry;
        this.things = thingRegistry;
    }
}
