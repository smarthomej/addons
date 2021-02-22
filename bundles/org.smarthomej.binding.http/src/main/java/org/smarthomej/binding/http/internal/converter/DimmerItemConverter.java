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

import java.math.BigDecimal;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.smarthomej.binding.http.internal.config.HttpChannelConfig;
import org.smarthomej.binding.http.internal.transform.ValueTransformation;

/**
 * The {@link DimmerItemConverter} implements {@link org.openhab.core.library.items.DimmerItem} conversions
 *
 * @author Jan N. Klug - Initial contribution
 */

@NonNullByDefault
public class DimmerItemConverter extends AbstractTransformingItemConverter {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private State state = UnDefType.UNDEF;

    public DimmerItemConverter(Consumer<State> updateState, Consumer<Command> postCommand,
            @Nullable Consumer<String> sendHttpValue, ValueTransformation stateTransformations,
            ValueTransformation commandTransformations, HttpChannelConfig channelConfig) {
        super(updateState, postCommand, sendHttpValue, stateTransformations, commandTransformations, channelConfig);
        this.channelConfig = channelConfig;
    }

    @Override
    protected @Nullable Command toCommand(String value) {
        return null;
    }

    @Override
    public String toString(Command command) {
        String string = channelConfig.commandToFixedValue(command);
        if (string != null) {
            return string;
        }

        if (command instanceof PercentType) {
            return ((PercentType) command).toString();
        }

        throw new IllegalArgumentException("Command type '" + command.toString() + "' not supported");
    }

    @Override
    public State toState(String string) {
        State newState = UnDefType.UNDEF;

        if (string.equals(channelConfig.onValue)) {
            newState = PercentType.HUNDRED;
        } else if (string.equals(channelConfig.offValue)) {
            newState = PercentType.ZERO;
        } else if (string.equals(channelConfig.increaseValue) && state instanceof PercentType) {
            BigDecimal newBrightness = ((PercentType) state).toBigDecimal().add(channelConfig.step);
            if (HUNDRED.compareTo(newBrightness) < 0) {
                newBrightness = HUNDRED;
            }
            newState = new PercentType(newBrightness);
        } else if (string.equals(channelConfig.decreaseValue) && state instanceof PercentType) {
            BigDecimal newBrightness = ((PercentType) state).toBigDecimal().subtract(channelConfig.step);
            if (BigDecimal.ZERO.compareTo(newBrightness) > 0) {
                newBrightness = BigDecimal.ZERO;
            }
            newState = new PercentType(newBrightness);
        } else {
            try {
                BigDecimal value = new BigDecimal(string);
                if (value.compareTo(PercentType.HUNDRED.toBigDecimal()) > 0) {
                    value = PercentType.HUNDRED.toBigDecimal();
                }
                if (value.compareTo(PercentType.ZERO.toBigDecimal()) < 0) {
                    value = PercentType.ZERO.toBigDecimal();
                }
                newState = new PercentType(value);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        state = newState;
        return newState;
    }
}
