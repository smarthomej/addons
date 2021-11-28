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
package org.smarthomej.transform.math.internal.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.smarthomej.transform.math.internal.AddTransformationService;

/**
 * Profile to offer the {@link AddTransformationService} on a ItemChannelLink.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class AddTransformationProfile extends AbstractMathTransformationProfile {

    public static final ProfileTypeUID PROFILE_TYPE_UID = new ProfileTypeUID(
            TransformationService.TRANSFORM_PROFILE_SCOPE, "ADD");

    static final String ADDEND_PARAM = "addend";

    private final @Nullable String addend;

    public AddTransformationProfile(ProfileCallback callback, ProfileContext context, TransformationService service) {
        super(callback, service, PROFILE_TYPE_UID);

        addend = getParam(context, ADDEND_PARAM);
    }

    @Override
    public void onCommandFromHandler(Command command) {
        String localAddend = addend;
        if (localAddend == null) {
            logger.warn(
                    "Please specify an addend for this Profile in the '{}' parameter. Returning the original command now.",
                    ADDEND_PARAM);
            callback.sendCommand(command);
            return;
        }
        callback.sendCommand((Command) transformState(command, localAddend));
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        String localAddend = addend;
        if (localAddend == null) {
            logger.warn(
                    "Please specify an addend for this Profile in the '{}' parameter. Returning the original state now.",
                    ADDEND_PARAM);
            callback.sendUpdate(state);
            return;
        }
        callback.sendUpdate((State) transformState(state, localAddend));
    }
}
