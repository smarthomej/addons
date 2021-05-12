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
package org.smarthomej.transform.chain.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.commons.transform.NoOpValueTransformation;
import org.smarthomej.commons.transform.ValueTransformation;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * Profile to offer the ChainTransformation on a ItemChannelLink
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ChainTransformationProfile implements StateProfile {

    public static final ProfileTypeUID PROFILE_TYPE_UID = new ProfileTypeUID(
            TransformationService.TRANSFORM_PROFILE_SCOPE, "CHAIN");

    private final Logger logger = LoggerFactory.getLogger(ChainTransformationProfile.class);
    private final ProfileCallback callback;

    private static final String TO_ITEM_PARAM = "toItem";
    private static final String TO_CHANNEL_PARAM = "toChannel";

    private final ValueTransformation toItem;
    private final ValueTransformation toChannel;

    public ChainTransformationProfile(ProfileCallback callback, ProfileContext context,
            ValueTransformationProvider valueTransformationProvider) {
        this.callback = callback;

        Object toItemObject = context.getConfiguration().get(TO_ITEM_PARAM);
        Object toChannelObject = context.getConfiguration().get(TO_CHANNEL_PARAM);

        logger.debug("Profile configured with '{}'='{}', '{}'={}", TO_ITEM_PARAM, toItemObject, TO_CHANNEL_PARAM,
                toChannelObject);

        if (toItemObject instanceof String && toChannelObject instanceof String) {
            toItem = valueTransformationProvider.getValueTransformation((String) toItemObject);
            toChannel = valueTransformationProvider.getValueTransformation((String) toChannelObject);
        } else {
            logger.warn("Parameters '{}' and '{}' have to be Strings. Profile will be inactive.", TO_ITEM_PARAM,
                    TO_CHANNEL_PARAM);
            toItem = NoOpValueTransformation.getInstance();
            toChannel = NoOpValueTransformation.getInstance();
        }
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return PROFILE_TYPE_UID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onCommandFromItem(Command command) {
        callback.handleCommand(new StringType(toChannel.apply(command.toString()).orElse(command.toString())));
    }

    @Override
    public void onCommandFromHandler(Command command) {
        callback.sendCommand(new StringType(toItem.apply(command.toString()).orElse(command.toString())));
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        callback.sendUpdate(new StringType(toItem.apply(state.toString()).orElse(state.toString())));
    }
}
