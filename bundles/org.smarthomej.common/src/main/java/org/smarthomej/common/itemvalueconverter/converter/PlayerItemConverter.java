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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.smarthomej.common.itemvalueconverter.ItemValueConverterChannelConfig;
import org.smarthomej.common.transform.ValueTransformation;

/**
 * The {@link PlayerItemConverter} implements {@link org.openhab.core.library.items.RollershutterItem}
 * conversions
 *
 * @author Jan N. Klug - Initial contribution
 */

@NonNullByDefault
public class PlayerItemConverter extends AbstractTransformingItemConverter {
    private @Nullable String lastCommand; // store last command to prevent duplicate commands

    public PlayerItemConverter(Consumer<State> updateState, Consumer<Command> postCommand,
            @Nullable Consumer<String> sendValue, ValueTransformation stateTransformations,
            ValueTransformation commandTransformations, ItemValueConverterChannelConfig channelConfig) {
        super(updateState, postCommand, sendValue, stateTransformations, commandTransformations, channelConfig);
    }

    @Override
    public String toString(Command command) {
        String string = channelConfig.commandToFixedValue(command);
        if (string != null) {
            return string;
        }

        throw new IllegalArgumentException("Command type '" + command.toString() + "' not supported");
    }

    @Override
    protected @Nullable Command toCommand(String string) {
        if (string.equals(lastCommand)) {
            // only send commands once
            return null;
        }
        lastCommand = string;

        if (string.equals(channelConfig.playValue)) {
            return PlayPauseType.PLAY;
        } else if (string.equals(channelConfig.pauseValue)) {
            return PlayPauseType.PAUSE;
        } else if (string.equals(channelConfig.nextValue)) {
            return NextPreviousType.NEXT;
        } else if (string.equals(channelConfig.previousValue)) {
            return NextPreviousType.PREVIOUS;
        } else if (string.equals(channelConfig.rewindValue)) {
            return RewindFastforwardType.REWIND;
        } else if (string.equals(channelConfig.fastforwardValue)) {
            return RewindFastforwardType.FASTFORWARD;
        }

        return null;
    }

    @Override
    public Optional<State> toState(String string) {
        // no value - we ignore state updates
        return Optional.empty();
    }
}
