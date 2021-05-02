/**
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
package org.smarthomej.binding.telenot.internal.actions;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.handler.TelenotBridgeHandler;
import org.smarthomej.binding.telenot.internal.protocol.TelenotCommand;

/**
 * The {@link BridgeActions} class defines thing actions for Telenot bridge.
 *
 * @author Ronny Grun - Initial contribution
 */
@ThingActionsScope(name = "telenot")
@NonNullByDefault
public class BridgeActions implements ThingActions {

    private final Logger logger = LoggerFactory.getLogger(BridgeActions.class);

    private @Nullable TelenotBridgeHandler bridge;

    public BridgeActions() {
        logger.trace("Telenot bridge actions service created");
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof TelenotBridgeHandler) {
            this.bridge = (TelenotBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridge;
    }

    /**
     * Set date/time thing action
     */
    @RuleAction(label = "set date/time", description = "Set's the current date and time.")
    public void setDateTime() {
        TelenotBridgeHandler bridge = this.bridge;
        if (bridge != null) {
            bridge.sendTelenotCommand(TelenotCommand.setDateTime());
            logger.debug("Sending set date/time command.");
        } else {
            logger.debug("Request for set date/time action, but bridge is undefined.");
        }
    }

    // Static method for Rules DSL backward compatibility
    public static void setDateTime(ThingActions actions) {
        ((BridgeActions) actions).setDateTime();
    }
}
