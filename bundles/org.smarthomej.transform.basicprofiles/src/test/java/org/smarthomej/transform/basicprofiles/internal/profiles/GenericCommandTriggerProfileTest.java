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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.Command;

/**
 * Basic unit tests for {@link GenericCommandTriggerProfile}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class GenericCommandTriggerProfileTest {

    @NonNullByDefault
    public static class ParameterSet {
        public final String events;
        public final Command command;

        public ParameterSet(String events, Command command) {
            this.events = events;
            this.command = command;
        }
    }

    public static Collection<Object[]> parameters() {
        return List.of(new Object[][] { //
                { new ParameterSet("1002", OnOffType.ON) }, //
                { new ParameterSet("1002", OnOffType.OFF) }, //
                { new ParameterSet("1002,1003", PlayPauseType.PLAY) }, //
                { new ParameterSet("1002,1003", PlayPauseType.PAUSE) }, //
                { new ParameterSet("1002,1003,3001", StopMoveType.STOP) }, //
                { new ParameterSet("1002,1003,3001", StopMoveType.MOVE) } //
        });
    }

    private @Mock ProfileCallback mockCallback;
    private @Mock ProfileContext mockContext;

    @BeforeEach
    public void setup() {
        mockCallback = mock(ProfileCallback.class);
        mockContext = mock(ProfileContext.class);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnOffSwitchItem(ParameterSet parameterSet) {
        Map<String, Object> properties = Map.of(AbstractTriggerProfile.PARAM_EVENTS, parameterSet.events,
                GenericCommandTriggerProfile.PARAM_COMMAND, parameterSet.command.toFullString());
        when(mockContext.getConfiguration()).thenReturn(new Configuration(properties));
        TriggerProfile profile = new GenericCommandTriggerProfile(mockCallback, mockContext);
        for (String event : parameterSet.events.split(",")) {
            verifyAction(profile, event, parameterSet.command);
        }
    }

    private void verifyAction(TriggerProfile profile, String trigger, Command expectation) {
        reset(mockCallback);
        profile.onTriggerFromHandler(trigger);
        verify(mockCallback, times(1)).sendCommand(eq(expectation));
    }
}
