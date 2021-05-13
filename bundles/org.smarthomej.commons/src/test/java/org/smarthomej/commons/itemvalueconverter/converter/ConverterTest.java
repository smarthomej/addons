/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.commons.itemvalueconverter.converter;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.smarthomej.commons.itemvalueconverter.ContentWrapper;
import org.smarthomej.commons.itemvalueconverter.ItemValueConverterChannelConfig;
import org.smarthomej.commons.transform.NoOpValueTransformation;

/**
 * The {@link ConverterTest} is a test class for state converters
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ConverterTest {

    @Mock
    private @NonNullByDefault({}) Consumer<String> sendHttpValue;

    @Mock
    private @NonNullByDefault({}) Consumer<State> updateState;

    @Mock
    private @NonNullByDefault({}) Consumer<Command> postCommand;

    private @NonNullByDefault({}) AutoCloseable closeable;

    @BeforeEach
    public void init() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void numberItemConverter() {
        NumberItemConverter converter = new NumberItemConverter(updateState, postCommand, sendHttpValue,
                NoOpValueTransformation.getInstance(), NoOpValueTransformation.getInstance(),
                new ItemValueConverterChannelConfig());

        // without unit
        Assertions.assertEquals(Optional.of(new DecimalType(1234)), converter.toState("1234"));

        // unit in transformation result
        Assertions.assertEquals(Optional.of(new QuantityType<>(100, SIUnits.CELSIUS)), converter.toState("100°C"));

        // no valid value
        Assertions.assertEquals(Optional.of(UnDefType.UNDEF), converter.toState("W"));
        Assertions.assertEquals(Optional.of(UnDefType.UNDEF), converter.toState(""));
    }

    @Test
    public void numberItemConverterWithUnit() {
        ItemValueConverterChannelConfig channelConfig = new ItemValueConverterChannelConfig();
        channelConfig.unit = "W";
        NumberItemConverter converter = new NumberItemConverter(updateState, postCommand, sendHttpValue,
                NoOpValueTransformation.getInstance(), NoOpValueTransformation.getInstance(), channelConfig);

        // without unit
        Assertions.assertEquals(Optional.of(new QuantityType<>(500, Units.WATT)), converter.toState("500"));

        // no valid value
        Assertions.assertEquals(Optional.of(UnDefType.UNDEF), converter.toState("foo"));
        Assertions.assertEquals(Optional.of(UnDefType.UNDEF), converter.toState(""));
    }

    @Test
    public void stringTypeConverter() {
        GenericItemConverter converter = createConverter(StringType::new);
        Assertions.assertEquals(Optional.of(new StringType("Test")), converter.toState("Test"));
    }

    @Test
    public void decimalTypeConverter() {
        GenericItemConverter converter = createConverter(DecimalType::new);
        Assertions.assertEquals(Optional.of(new DecimalType(15.6)), converter.toState("15.6"));
    }

    @Test
    public void pointTypeConverter() {
        GenericItemConverter converter = createConverter(PointType::new);
        Assertions.assertEquals(
                Optional.of(new PointType(new DecimalType(51.1), new DecimalType(7.2), new DecimalType(100))),
                converter.toState("51.1, 7.2, 100"));
    }

    @Test
    public void playerItemTypeConverter() {
        ItemValueConverterChannelConfig cfg = new ItemValueConverterChannelConfig();
        cfg.playValue = "PLAY";
        ContentWrapper content = new ContentWrapper("PLAY".getBytes(StandardCharsets.UTF_8), "UTF-8", null);
        PlayerItemConverter converter = new PlayerItemConverter(updateState, postCommand, sendHttpValue,
                NoOpValueTransformation.getInstance(), NoOpValueTransformation.getInstance(), cfg);
        converter.process(content);
        converter.process(content);

        Mockito.verify(postCommand, Mockito.atMostOnce()).accept(PlayPauseType.PLAY);
        Mockito.verify(updateState, Mockito.never()).accept(ArgumentMatchers.any());
    }

    public GenericItemConverter createConverter(Function<String, State> fcn) {
        return new GenericItemConverter(fcn, updateState, postCommand, sendHttpValue,
                NoOpValueTransformation.getInstance(), NoOpValueTransformation.getInstance(),
                new ItemValueConverterChannelConfig());
    }
}
