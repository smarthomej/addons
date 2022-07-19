/**
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
package org.smarthomej.transform.math.internal.profiles;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.util.stream.Stream;

import javax.measure.Unit;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.smarthomej.transform.math.internal.MultiplyTransformationService;

/**
 * Basic unit tests for {@link MultiplyTransformationProfile}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
class MultiplyTransformationProfileTest {

    private static final String UNKNOWN_ITEM_NAME = "unknownItem";
    private static final String TEST_ITEM_NAME = "testItem";

    private static final Stream<Arguments> configurations() {
        return Stream.of(Arguments.of(2, DecimalType.valueOf("23.333"), null, null, DecimalType.valueOf("46.666")), //
                Arguments.of(2, DecimalType.valueOf("23.333"), null, DecimalType.valueOf("3"),
                        DecimalType.valueOf("46.666")), //
                Arguments.of(2, DecimalType.valueOf("23.333"), TEST_ITEM_NAME, UnDefType.UNDEF,
                        DecimalType.valueOf("46.666")), //
                Arguments.of(2, DecimalType.valueOf("23.333"), UNKNOWN_ITEM_NAME, DecimalType.valueOf("3"),
                        DecimalType.valueOf("46.666")), //
                Arguments.of(2, DecimalType.valueOf("23.333"), TEST_ITEM_NAME, DecimalType.valueOf("3"),
                        DecimalType.valueOf("69.999")), //
                Arguments.of(2, QuantityType.valueOf("230 V"), TEST_ITEM_NAME, QuantityType.valueOf("6 A"),
                        QuantityType.valueOf("1380 W")));
    }

    @BeforeEach
    public void setup() {
        // initialize parser with ImperialUnits, otherwise units like °F are unknown
        @SuppressWarnings("unused")
        Unit<Temperature> fahrenheit = ImperialUnits.FAHRENHEIT;
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void testDecimalTypeOnCommandFromHandler(Integer multiplicand, Command cmd, @Nullable String itemName,
            @Nullable State itemState, Command expectedResult) throws ItemNotFoundException {
        ProfileCallback callback = mock(ProfileCallback.class);
        MultiplyTransformationProfile profile = createProfile(callback, multiplicand, itemName, itemState);

        profile.onCommandFromHandler(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).sendCommand(capture.capture());

        Command result = capture.getValue();
        assertThat(result, is(expectedResult));
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void testDecimalTypeOnStateUpdateFromHandler(Integer multiplicand, State state, @Nullable String itemName,
            @Nullable State itemState, State expectedResult) throws ItemNotFoundException {
        ProfileCallback callback = mock(ProfileCallback.class);
        MultiplyTransformationProfile profile = createProfile(callback, multiplicand, itemName, itemState);

        profile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        assertThat(result, is(expectedResult));
    }

    @Test
    @Disabled
    public void testQuantityTypeOnCommandFromHandler() throws ItemNotFoundException {
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
    @Disabled
    public void testQuantityTypeOnStateUpdateFromHandler() throws ItemNotFoundException {
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

    private MultiplyTransformationProfile createProfile(ProfileCallback callback, Integer multiplicand)
            throws ItemNotFoundException {
        return createProfile(callback, multiplicand, null, null);
    }

    private MultiplyTransformationProfile createProfile(ProfileCallback callback, Integer multiplicand,
            @Nullable String itemName, @Nullable State state) throws ItemNotFoundException {
        ProfileContext mockedProfileContext = mock(ProfileContext.class);
        ItemRegistry mockedItemRegistry = mock(ItemRegistry.class);
        Configuration config = new Configuration();
        config.put(MultiplyTransformationProfile.MUTLIPLICAND_PARAM, multiplicand);
        if (itemName != null && state != null) {
            config.put(AbstractArithmeticMathTransformationProfile.ITEM_NAME_PARAM, itemName);
            GenericItem item = new NumberItem(TEST_ITEM_NAME);
            item.setState(state);
            when(mockedItemRegistry.getItem(TEST_ITEM_NAME)).thenReturn(item);
            when(mockedItemRegistry.getItem(UNKNOWN_ITEM_NAME)).thenThrow(ItemNotFoundException.class);
        }
        when(mockedProfileContext.getConfiguration()).thenReturn(config);

        return new MultiplyTransformationProfile(callback, mockedProfileContext, new MultiplyTransformationService(),
                mockedItemRegistry);
    }
}
