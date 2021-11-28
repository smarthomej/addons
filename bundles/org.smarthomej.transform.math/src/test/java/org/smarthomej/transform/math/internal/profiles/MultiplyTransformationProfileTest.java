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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import javax.measure.Unit;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.smarthomej.transform.math.internal.MultiplyTransformationService;

/**
 * Basic unit tests for {@link MultiplyTransformationProfile}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
class MultiplyTransformationProfileTest {

    @BeforeEach
    public void setup() {
        // initialize parser with ImperialUnits, otherwise units like °F are unknown
        @SuppressWarnings("unused")
        Unit<Temperature> fahrenheit = ImperialUnits.FAHRENHEIT;
    }

    @Test
    public void testDecimalTypeOnCommandFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        MultiplyTransformationProfile profile = createProfile(callback, 2);

        Command cmd = new DecimalType(23.333);
        profile.onCommandFromHandler(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).sendCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(46.666));
    }

    @Test
    public void testDecimalTypeOnStateUpdateFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        MultiplyTransformationProfile profile = createProfile(callback, 2);

        State state = new DecimalType(23.333);
        profile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(46.666));
    }

    @Test
    public void testQuantityTypeOnCommandFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        MultiplyTransformationProfile profile = createProfile(callback, 2);

        Command cmd = new QuantityType<Temperature>("23.333 °C");
        profile.onCommandFromHandler(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).sendCommand(capture.capture());

        Command result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> qtResult = (QuantityType<Temperature>) result;
        assertThat(qtResult.doubleValue(), is(46.666));
        assertThat(qtResult.getUnit(), is(SIUnits.CELSIUS));
    }

    @Test
    public void testQuantityTypeOnStateUpdateFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        MultiplyTransformationProfile profile = createProfile(callback, 2);

        State state = new QuantityType<Temperature>("23.333 °C");
        profile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> qtResult = (QuantityType<Temperature>) result;
        assertThat(qtResult.doubleValue(), is(46.666));
        assertThat(qtResult.getUnit(), is(SIUnits.CELSIUS));
    }

    private MultiplyTransformationProfile createProfile(ProfileCallback callback, Integer multiplicand) {
        ProfileContext context = mock(ProfileContext.class);
        Configuration config = new Configuration();
        config.put(MultiplyTransformationProfile.MUTLIPLICAND_PARAM, multiplicand);
        when(context.getConfiguration()).thenReturn(config);

        return new MultiplyTransformationProfile(callback, context, new MultiplyTransformationService());
    }
}
