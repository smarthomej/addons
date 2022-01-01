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
package org.smarthomej.commons.itemvalueconverter.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.smarthomej.commons.itemvalueconverter.ContentWrapper;
import org.smarthomej.commons.itemvalueconverter.ItemValueConverterChannelConfig;
import org.smarthomej.commons.transform.NoOpValueTransformation;
import org.smarthomej.commons.transform.ValueTransformation;

/**
 * The {@link AbstractTransformingItemConverterTest} is a test class for the {@link AbstractTransformingItemConverter}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AbstractTransformingItemConverterTest {

    @Mock
    private @NonNullByDefault({}) Consumer<String> sendHttpValue;

    @Mock
    private @NonNullByDefault({}) Consumer<State> updateState;

    @Mock
    private @NonNullByDefault({}) Consumer<Command> postCommand;

    private @NonNullByDefault({}) AutoCloseable closeable;

    @Spy
    private ValueTransformation stateValueTransformation = NoOpValueTransformation.getInstance();

    @Spy
    private ValueTransformation commandValueTransformation = NoOpValueTransformation.getInstance();

    @BeforeEach
    public void init() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void undefOnNullContentTest() {
        TestItemConverter realConverter = new TestItemConverter(updateState, postCommand, sendHttpValue,
                stateValueTransformation, commandValueTransformation, false);
        TestItemConverter converter = spy(realConverter);

        converter.process(null);
        // make sure UNDEF is send as state update
        verify(updateState, only()).accept(UnDefType.UNDEF);
        verify(postCommand, never()).accept(any());
        verify(sendHttpValue, never()).accept(any());

        // make sure no other processing applies
        verify(converter, never()).toState(any());
        verify(converter, never()).toCommand(any());
        verify(converter, never()).toString(any());
    }

    @Test
    public void commandIsPostedAsCommand() {
        TestItemConverter converter = new TestItemConverter(updateState, postCommand, sendHttpValue,
                stateValueTransformation, commandValueTransformation, true);

        converter.process(new ContentWrapper("TEST".getBytes(StandardCharsets.UTF_8), "", null));

        // check state transformation is applied
        verify(stateValueTransformation).apply(any());
        verify(commandValueTransformation, never()).apply(any());

        // check only postCommand is applied
        verify(updateState, never()).accept(any());
        verify(postCommand, only()).accept(new StringType("TEST"));
        verify(sendHttpValue, never()).accept(any());
    }

    @Test
    public void updateIsPostedAsUpdate() {
        TestItemConverter converter = new TestItemConverter(updateState, postCommand, sendHttpValue,
                stateValueTransformation, commandValueTransformation, false);

        converter.process(new ContentWrapper("TEST".getBytes(StandardCharsets.UTF_8), "", null));

        // check state transformation is applied
        verify(stateValueTransformation).apply(any());
        verify(commandValueTransformation, never()).apply(any());

        // check only updateState is called
        verify(updateState, only()).accept(new StringType("TEST"));
        verify(postCommand, never()).accept(any());
        verify(sendHttpValue, never()).accept(any());
    }

    @Test
    public void sendCommandSendsCommand() {
        TestItemConverter converter = new TestItemConverter(updateState, postCommand, sendHttpValue,
                stateValueTransformation, commandValueTransformation, false);

        converter.send(new StringType("TEST"));

        // check command transformation is applied
        verify(stateValueTransformation, never()).apply(any());
        verify(commandValueTransformation).apply(any());

        // check only sendHttpValue is applied
        verify(updateState, never()).accept(any());
        verify(postCommand, never()).accept(any());
        verify(sendHttpValue, only()).accept("TEST");
    }

    private static class TestItemConverter extends AbstractTransformingItemConverter {
        private boolean hasCommand;

        public TestItemConverter(Consumer<State> updateState, Consumer<Command> postCommand,
                @Nullable Consumer<String> sendValue, ValueTransformation stateValueTransformation,
                ValueTransformation commandValueTransformation, boolean hasCommand) {
            super(updateState, postCommand, sendValue, stateValueTransformation, commandValueTransformation,
                    new ItemValueConverterChannelConfig());
            this.hasCommand = hasCommand;
        }

        @Override
        protected @Nullable Command toCommand(String value) {
            return hasCommand ? new StringType(value) : null;
        }

        @Override
        protected Optional<State> toState(String value) {
            return Optional.of(new StringType(value));
        }

        @Override
        protected String toString(Command command) {
            return command.toString();
        }
    }
}
