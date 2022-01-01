/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.deconz.internal.action;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.deconz.internal.Util;
import org.smarthomej.binding.deconz.internal.handler.DeconzBridgeHandler;

/**
 * The {@link BridgeActions} provides actions for managing scenes in groups
 *
 * @author Jan N. Klug - Initial contribution
 */
@ThingActionsScope(name = "deconz")
@NonNullByDefault
public class BridgeActions implements ThingActions {
    private final Logger logger = LoggerFactory.getLogger(BridgeActions.class);

    private @Nullable DeconzBridgeHandler handler;

    @RuleAction(label = "permit join ZigBee network", description = "Permits ew devices to join the zigbee network for a given duration (default 120s).")
    public void permitJoin(@ActionInput(name = "duration") @Nullable Integer duration) {
        DeconzBridgeHandler handler = this.handler;

        if (handler == null) {
            logger.warn("Deconz BridgeActions service ThingHandler is null!");
            return;
        }

        int searchDuration = Util.constrainToRange(Objects.requireNonNullElse(duration, 120), 1, 240);

        Object object = Map.of("permitjoin", searchDuration);
        handler.sendObject("config", object, HttpMethod.PUT).thenAccept(v -> {
            if (v.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                logger.warn("Sending {} via PUT to config failed: {} - {}", object, v.getResponseCode(), v.getBody());
            } else {
                logger.trace("Result code={}, body={}", v.getResponseCode(), v.getBody());
                logger.info("Enabled device searching for {} seconds on bridge {}.", searchDuration,
                        handler.getThing().getUID());
            }
        }).exceptionally(e -> {
            logger.warn("Sending {} via PUT to config failed: {} - {}", object, e.getClass(), e.getMessage());
            return null;
        });
    }

    public static void permitJoin(ThingActions actions, @Nullable Integer duration) {
        if (actions instanceof BridgeActions) {
            ((BridgeActions) actions).permitJoin(duration);
        }
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof DeconzBridgeHandler) {
            this.handler = (DeconzBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }
}
