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
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final ValueTransformation toItem;
    private final ValueTransformation toChannel;
    private final boolean undefOnError;

    public ChainTransformationProfile(ProfileCallback callback, ProfileContext context,
            ValueTransformationProvider valueTransformationProvider) {
        this.callback = callback;

        ChainProfileConfiguration configuration = context.getConfiguration().as(ChainProfileConfiguration.class);
        logger.debug("Profile configured with: '{}'", configuration);

        toItem = valueTransformationProvider.getValueTransformation(configuration.toItem);
        toChannel = valueTransformationProvider.getValueTransformation(configuration.toChannel);
        undefOnError = configuration.undefOnError;
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
        toChannel.apply(command.toString()).map(StringType::new).ifPresentOrElse(callback::handleCommand, () -> {
            if (undefOnError) {
                callback.sendUpdate(UnDefType.UNDEF);
            }
        });
    }

    @Override
    public void onCommandFromHandler(Command command) {
        toItem.apply(command.toString()).map(StringType::new).ifPresent(callback::sendCommand);
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        toItem.apply(state.toString()).map(StringType::new).ifPresentOrElse(callback::sendUpdate, () -> {
            if (undefOnError) {
                callback.sendUpdate(UnDefType.UNDEF);
            }
        });
    }
}
