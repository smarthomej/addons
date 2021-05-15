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

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
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

    private final ValueTransformationProvider valueTransformationProvider;
    private final ChainProfileConfiguration configuration;

    private ValueTransformation toItem;
    private ValueTransformation toChannel;

    public ChainTransformationProfile(ProfileCallback callback, ProfileContext context,
            ValueTransformationProvider valueTransformationProvider) {
        this.callback = callback;
        this.valueTransformationProvider = valueTransformationProvider;

        configuration = context.getConfiguration().as(ChainProfileConfiguration.class);
        logger.debug("Profile configured with: '{}'", configuration);

        toItem = valueTransformationProvider.getValueTransformation(configuration.toItem);
        toChannel = valueTransformationProvider.getValueTransformation(configuration.toChannel);
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return PROFILE_TYPE_UID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        // do nothing
    }

    @Override
    public void onCommandFromItem(Command command) {
        transformToChannel(command.toString()).ifPresent(callback::handleCommand);
    }

    @Override
    public void onCommandFromHandler(Command command) {
        transformToItem(command.toString()).ifPresentOrElse(callback::sendCommand, () -> {
            if (configuration.undefOnError) {
                callback.sendUpdate(UnDefType.UNDEF);
            }
        });
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        transformToItem(state.toString()).ifPresentOrElse(callback::sendUpdate, () -> {
            if (configuration.undefOnError) {
                callback.sendUpdate(UnDefType.UNDEF);
            }
        });
    }

    private Optional<StringType> transformToItem(String input) {
        if (!NoOpValueTransformation.getInstance().equals(toItem) && !configuration.toItem.isEmpty()) {
            toItem = valueTransformationProvider.getValueTransformation(configuration.toItem);
        }
        return toItem.apply(input).map(StringType::new);
    }

    private Optional<StringType> transformToChannel(String input) {
        if (!NoOpValueTransformation.getInstance().equals(toChannel) && !configuration.toChannel.isEmpty()) {
            toChannel = valueTransformationProvider.getValueTransformation(configuration.toChannel);
        }
        return toChannel.apply(input).map(StringType::new);
    }
}
