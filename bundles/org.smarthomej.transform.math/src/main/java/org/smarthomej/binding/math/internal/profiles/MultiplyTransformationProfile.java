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
package org.smarthomej.binding.math.internal.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.smarthomej.binding.math.internal.MultiplyTransformationService;

/**
 * Profile to offer the {@link MultiplyTransformationService} on a ItemChannelLink.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class MultiplyTransformationProfile extends AbstractMathTransformationProfile {

    public static final ProfileTypeUID PROFILE_TYPE_UID = new ProfileTypeUID(
            TransformationService.TRANSFORM_PROFILE_SCOPE, "MULTIPLY");

    private static final String MUTLIPLICAND_PARAM = "multiplicand";

    private final @Nullable String multiplicand;

    public MultiplyTransformationProfile(ProfileCallback callback, ProfileContext context,
            TransformationService service) {
        super(callback, service, PROFILE_TYPE_UID);

        multiplicand = getParam(context, MUTLIPLICAND_PARAM);
    }

    @Override
    public void onCommandFromHandler(Command command) {
        String localMultiplicand = multiplicand;
        if (localMultiplicand == null) {
            logger.warn(
                    "Please specify a multiplicand for this Profile in the '{}' parameter. Returning the original command now.",
                    MUTLIPLICAND_PARAM);
            callback.sendCommand(command);
            return;
        }
        callback.sendCommand((Command) transformState(command, localMultiplicand));
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        String localMultiplicand = multiplicand;
        if (localMultiplicand == null) {
            logger.warn(
                    "Please specify a multiplicand for this Profile in the '{}' parameter. Returning the original state now.",
                    MUTLIPLICAND_PARAM);
            callback.sendUpdate(state);
            return;
        }
        callback.sendUpdate((State) transformState(state, localMultiplicand));
    }
}
