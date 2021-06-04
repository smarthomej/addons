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
package org.smarthomej.transform.basicprofiles.internal.profiles;

import static org.smarthomej.transform.basicprofiles.internal.factory.BasicProfilesFactory.DEBOUNCE_TIME_UID;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.transform.basicprofiles.internal.config.DebounceTimeStateProfileConfig;

/**
 * Debounces a {@link State} by time.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DebounceTimeStateProfile implements StateProfile {
    private final Logger logger = LoggerFactory.getLogger(DebounceTimeStateProfile.class);

    private final ProfileCallback callback;
    private final DebounceTimeStateProfileConfig config;
    private final ScheduledExecutorService scheduler;

    private @Nullable ScheduledFuture<?> toHandlerJob;
    private @Nullable ScheduledFuture<?> toItemJob;

    public DebounceTimeStateProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        this.scheduler = context.getExecutorService();
        this.config = context.getConfiguration().as(DebounceTimeStateProfileConfig.class);
        logger.debug("Configuring profile with parameters: [toHandler='{}', toItem='{}']", config.toHandlerDelay,
                config.toItemDelay);

        if (config.toHandlerDelay < 0) {
            throw new IllegalArgumentException(String.format(
                    "debounceTimeToHandler has to be a non-negative integer but was '%d'.", config.toHandlerDelay));
        }

        if (config.toItemDelay < 0) {
            throw new IllegalArgumentException(String
                    .format("debounceTimeToItem has to be a non-negative integer but was '%d'.", config.toItemDelay));
        }
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return DEBOUNCE_TIME_UID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        // no-op
    }

    @Override
    public void onCommandFromItem(Command command) {
        logger.debug("Received command '{}' from item", command);
        if (config.toHandlerDelay == 0) {
            callback.handleCommand(command);
            return;
        }
        ScheduledFuture<?> localToHandlerJob = toHandlerJob;
        if (localToHandlerJob != null) {
            // if we have an old job, cancel it
            localToHandlerJob.cancel(true);
        }
        toHandlerJob = scheduler.schedule(() -> {
            logger.debug("Sending command '{}' to handler", command);
            callback.handleCommand(command);
            toHandlerJob = null;
        }, config.toHandlerDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onCommandFromHandler(Command command) {
        logger.debug("Received command '{}' from handler", command);
        if (config.toItemDelay == 0) {
            callback.sendCommand(command);
            return;
        }
        ScheduledFuture<?> localToItemJob = toItemJob;
        if (localToItemJob != null) {
            // if we have an old job, cancel it
            localToItemJob.cancel(true);
        }
        toItemJob = scheduler.schedule(() -> {
            logger.debug("Sending command '{}' to item", command);
            callback.sendCommand(command);
            toItemJob = null;
        }, config.toItemDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        logger.debug("Received state update from Handler");
        if (config.toItemDelay == 0) {
            callback.sendUpdate(state);
            return;
        }
        ScheduledFuture<?> localToItemJob = toItemJob;
        if (localToItemJob != null) {
            // if we have an old job, cancel it
            localToItemJob.cancel(true);
        }
        toItemJob = scheduler.schedule(() -> {
            logger.debug("Posting state update '{}' to Item", state);
            callback.sendUpdate(state);
            toItemJob = null;
        }, config.toItemDelay, TimeUnit.MILLISECONDS);
    }
}
