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
package org.smarthomej.common.itemvalueconverter.converter;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.smarthomej.common.itemvalueconverter.ItemValueConverterChannelConfig;
import org.smarthomej.common.transform.ValueTransformation;

/**
 * The {@link GenericItemConverter} implements simple conversions for different item types
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class GenericItemConverter extends AbstractTransformingItemConverter {
    private final Function<String, State> toState;

    public GenericItemConverter(Function<String, State> toState, Consumer<State> updateState,
            Consumer<Command> postCommand, @Nullable Consumer<String> sendValue,
            ValueTransformation stateTransformations, ValueTransformation commandTransformations,
            ItemValueConverterChannelConfig channelConfig) {
        super(updateState, postCommand, sendValue, stateTransformations, commandTransformations, channelConfig);
        this.toState = toState;
    }

    protected Optional<State> toState(String value) {
        try {
            return Optional.of(toState.apply(value));
        } catch (IllegalArgumentException e) {
            return Optional.of(UnDefType.UNDEF);
        }
    }

    @Override
    protected @Nullable Command toCommand(String value) {
        return null;
    }

    protected String toString(Command command) {
        return command.toString();
    }
}
