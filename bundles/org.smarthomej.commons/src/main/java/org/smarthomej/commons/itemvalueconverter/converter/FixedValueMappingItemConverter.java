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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.smarthomej.commons.itemvalueconverter.ItemValueConverterChannelConfig;
import org.smarthomej.commons.transform.ValueTransformation;

/**
 * The {@link FixedValueMappingItemConverter} implements mapping conversions for different item-types
 *
 * @author Jan N. Klug - Initial contribution
 */

@NonNullByDefault
public class FixedValueMappingItemConverter extends AbstractTransformingItemConverter {

    public FixedValueMappingItemConverter(Consumer<State> updateState, Consumer<Command> postCommand,
            @Nullable Consumer<String> sendValue, ValueTransformation stateTransformations,
            ValueTransformation commandTransformations, ItemValueConverterChannelConfig channelConfig) {
        super(updateState, postCommand, sendValue, stateTransformations, commandTransformations, channelConfig);
    }

    @Override
    protected @Nullable Command toCommand(String value) {
        return null;
    }

    @Override
    public String toString(Command command) {
        String value = channelConfig.commandToFixedValue(command);
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(
                "Command type '" + command.toString() + "' not supported or mapping not defined.");
    }

    @Override
    public Optional<State> toState(String string) {
        State state = channelConfig.fixedValueToState(string);

        return Optional.of(Objects.requireNonNullElse(state, UnDefType.UNDEF));
    }
}
