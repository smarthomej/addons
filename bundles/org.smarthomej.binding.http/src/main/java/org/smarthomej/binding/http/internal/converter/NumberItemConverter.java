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
package org.smarthomej.binding.http.internal.converter;

import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.smarthomej.binding.http.internal.config.HttpChannelConfig;
import org.smarthomej.binding.http.internal.transform.ValueTransformation;

/**
 * The {@link NumberItemConverter} implements {@link org.openhab.core.library.items.NumberItem} conversions
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class NumberItemConverter extends AbstractTransformingItemConverter {

    public NumberItemConverter(Consumer<State> updateState, Consumer<Command> postCommand,
            @Nullable Consumer<String> sendHttpValue, ValueTransformation stateTransformations,
            ValueTransformation commandTransformations, HttpChannelConfig channelConfig) {
        super(updateState, postCommand, sendHttpValue, stateTransformations, commandTransformations, channelConfig);
    }

    @Override
    protected @Nullable Command toCommand(String value) {
        return null;
    }

    @Override
    protected Optional<State> toState(String value) {
        String trimmedValue = value.trim();
        State newState = UnDefType.UNDEF;
        if (!trimmedValue.isEmpty()) {
            try {
                if (channelConfig.unit != null) {
                    // we have a given unit - use that
                    newState = new QuantityType<>(trimmedValue + " " + channelConfig.unit);
                } else {
                    try {
                        // try if we have a simple number
                        newState = new DecimalType(trimmedValue);
                    } catch (IllegalArgumentException e1) {
                        // not a plain number, maybe with unit?
                        newState = new QuantityType<>(trimmedValue);
                    }
                }
            } catch (IllegalArgumentException e) {
                // finally failed
            }
        }
        return Optional.of(newState);
    }

    @Override
    protected String toString(Command command) {
        return command.toString();
    }
}
