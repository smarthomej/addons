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

import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.smarthomej.commons.itemvalueconverter.ChannelMode;
import org.smarthomej.commons.itemvalueconverter.ContentWrapper;
import org.smarthomej.commons.itemvalueconverter.ItemValueConverter;
import org.smarthomej.commons.itemvalueconverter.ItemValueConverterChannelConfig;
import org.smarthomej.commons.transform.ValueTransformation;

/**
 * The {@link AbstractTransformingItemConverter} is a base class for an item converter with transformations
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractTransformingItemConverter implements ItemValueConverter {
    private final Consumer<State> updateState;
    private final Consumer<Command> postCommand;
    private final @Nullable Consumer<String> sendValue;
    private final ValueTransformation stateTransformations;
    private final ValueTransformation commandTransformations;

    protected final ItemValueConverterChannelConfig channelConfig;

    public AbstractTransformingItemConverter(Consumer<State> updateState, Consumer<Command> postCommand,
            @Nullable Consumer<String> sendValue, ValueTransformation stateTransformations,
            ValueTransformation commandTransformations, ItemValueConverterChannelConfig channelConfig) {
        this.updateState = updateState;
        this.postCommand = postCommand;
        this.sendValue = sendValue;
        this.stateTransformations = stateTransformations;
        this.commandTransformations = commandTransformations;
        this.channelConfig = channelConfig;
    }

    @Override
    public void process(@Nullable ContentWrapper content) {
        if (content == null) {
            updateState.accept(UnDefType.UNDEF);
            return;
        }
        if (channelConfig.mode != ChannelMode.WRITEONLY) {
            stateTransformations.apply(content.getAsString()).ifPresent(transformedValue -> {
                Command command = toCommand(transformedValue);
                if (command != null) {
                    postCommand.accept(command);
                } else {
                    toState(transformedValue).ifPresent(updateState);
                }
            });
        } else {
            throw new IllegalStateException("Write-only channel");
        }
    }

    @Override
    public void send(Command command) {
        Consumer<String> sendHttpValue = this.sendValue;
        if (sendHttpValue != null && channelConfig.mode != ChannelMode.READONLY) {
            commandTransformations.apply(toString(command)).ifPresent(sendHttpValue);
        } else {
            throw new IllegalStateException("Read-only channel");
        }
    }

    /**
     * check if this converter received a value that needs to be sent as command
     *
     * @param value the value
     * @return the command or null
     */
    protected abstract @Nullable Command toCommand(String value);

    /**
     * convert the received value to a state
     *
     * @param value the value
     * @return the state that represents the value of UNDEF if conversion failed
     */
    protected abstract Optional<State> toState(String value);

    /**
     * convert a command to a string
     *
     * @param command the command
     * @return the string representation of the command
     */
    protected abstract String toString(Command command);

    @FunctionalInterface
    public interface Factory {
        ItemValueConverter create(Consumer<State> updateState, Consumer<Command> postCommand,
                @Nullable Consumer<String> sendHttpValue, ValueTransformation stateTransformations,
                ValueTransformation commandTransformations, ItemValueConverterChannelConfig channelConfig);
    }
}
