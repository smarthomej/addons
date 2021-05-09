/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.basicprofiles.internal.profiles;

import static org.smarthomej.binding.basicprofiles.internal.factory.BasicProfilesFactory.TOGGLE_SWITCH_PROFILE_TYPE_UID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.types.State;

/**
 * The {@link ToggleSwitchTriggerProfile} class implements the toggle ON/OFF behavior when being linked to a
 * Switch item.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ToggleSwitchTriggerProfile extends AbstractTriggerProfile {

    private @Nullable State previousState;

    public ToggleSwitchTriggerProfile(ProfileCallback callback, ProfileContext context) {
        super(callback, context);
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return TOGGLE_SWITCH_PROFILE_TYPE_UID;
    }

    @Override
    public void onTriggerFromHandler(String payload) {
        if (events.contains(payload)) {
            OnOffType state = OnOffType.ON.equals(previousState) ? OnOffType.OFF : OnOffType.ON;
            callback.sendCommand(state);
            previousState = state;
        }
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        previousState = state.as(OnOffType.class);
    }
}
