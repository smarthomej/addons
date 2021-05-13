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
package org.smarthomej.transform.basicprofiles.internal.profiles;

import static org.smarthomej.transform.basicprofiles.internal.factory.BasicProfilesFactory.TO_PERCENT_TYPE_UID;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.units.indriya.AbstractUnit;

/***
 * This is the default implementation for a {@link ToPercentStateProfile}}. Maps a numeric value to 0 - 100%.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ToPercentStateProfile implements StateProfile {

    static final String MIN_PARAM = "min";
    static final String MAX_PARAM = "max";

    private final Logger logger = LoggerFactory.getLogger(ToPercentStateProfile.class);

    private final ProfileCallback callback;

    private final QuantityType<?> min;
    private final QuantityType<?> max;

    public ToPercentStateProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;

        final QuantityType<?> minParam = getParam(context, MIN_PARAM);
        if (minParam == null) {
            throw new IllegalArgumentException(String.format("Parameter '%s' is not a Number value.", MIN_PARAM));
        }
        min = minParam;

        final QuantityType<?> maxParam = getParam(context, MAX_PARAM);
        if (maxParam == null) {
            throw new IllegalArgumentException(String.format("Parameter '%s' is not a Number value.", MAX_PARAM));
        }
        final QuantityType<?> convertedMaxParam = maxParam.toUnit(min.getUnit());
        if (convertedMaxParam == null) {
            throw new IllegalArgumentException(
                    String.format("Units of parameters '%s' and '%s' are not compatible: %s != %s", MIN_PARAM,
                            MAX_PARAM, min, maxParam));
        }
        if (convertedMaxParam.doubleValue() <= min.doubleValue()) {
            throw new IllegalArgumentException(
                    String.format("Parameter '%s' (%s) is less than or equal to '%s' (%s) parameter.", MAX_PARAM,
                            convertedMaxParam, MIN_PARAM, min));
        }
        max = convertedMaxParam;
    }

    private @Nullable QuantityType<?> getParam(ProfileContext context, String param) {
        final Object paramValue = context.getConfiguration().get(param);
        logger.debug("Configuring profile with {} parameter '{}'", param, paramValue);
        if (paramValue instanceof String) {
            try {
                return new QuantityType<>((String) paramValue);
            } catch (IllegalArgumentException e) {
                logger.error("Cannot convert value '{}' of parameter {} into a valid QuantityType.", paramValue, param);
            }
        } else if (paramValue instanceof BigDecimal) {
            final BigDecimal value = (BigDecimal) paramValue;
            return QuantityType.valueOf(value.doubleValue(), AbstractUnit.ONE);
        }
        return null;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return TO_PERCENT_TYPE_UID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        // do nothing
    }

    @Override
    public void onCommandFromHandler(Command command) {
        final Type mappedCommand = mapValueToPercent(command);
        logger.trace("Mapped command from '{}' to value '{}'.", command, mappedCommand);
        if (mappedCommand instanceof Command) {
            callback.sendCommand((Command) mappedCommand);
        }
    }

    @Override
    public void onCommandFromItem(Command command) {
        logger.trace("Received command: {} ({})", command, command.getClass());
        final Command mappedCommand = mapPercentToValue(command);
        logger.trace("Mapped value from '{}' to command '{}'.", command, mappedCommand);
        callback.handleCommand(mappedCommand);
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        final Type mappedState = mapValueToPercent(state);
        logger.trace("Mapped state from '{}' to value '{}'.", state, mappedState);
        if (mappedState instanceof State) {
            callback.sendUpdate((State) mappedState);
        }
    }

    private Type mapValueToPercent(Type value) {
        if (value instanceof QuantityType) {
            final QuantityType<?> qtState = (QuantityType<?>) value;
            final QuantityType<?> finalMin;
            final QuantityType<?> finalMax;
            if (min.getUnit() == Units.ONE && max.getUnit() == Units.ONE) {
                // allow min/max values without unit -> implicitly assume its the same as the one from the state, but
                // warn the user
                finalMin = new QuantityType<>(min.toBigDecimal(), qtState.getUnit());
                finalMax = new QuantityType<>(max.toBigDecimal(), qtState.getUnit());
                logger.warn(
                        "Received a QuantityType '{}' with unit, but the min/max vaules are defined as a plain number without units (min={}, max={}), please consider adding units to them.",
                        value, min, max);
            } else {
                finalMin = min.toUnit(qtState.getUnit());
                finalMax = min.toUnit(qtState.getUnit());
                if (finalMin == null || finalMax == null) {
                    logger.warn(
                            "Cannot compare state '{}' to min/max values because units (min={}, max={}) do not match.",
                            qtState, min, max);
                    return UnDefType.UNDEF;
                }
            }
            return mapValueToPercent(finalMin.doubleValue(), finalMax.doubleValue(), qtState.doubleValue());
        } else if (value instanceof DecimalType) {
            return mapValueToPercent(min.doubleValue(), max.doubleValue(), ((DecimalType) value).doubleValue());
        }
        return UnDefType.UNDEF;
    }

    private Type mapValueToPercent(double min, double max, double value) {
        return new PercentType(restrictToBounds(Math.round(((value - min) * 100.0) / (max - min))));
    }

    private Command mapPercentToValue(Command command) {
        if (command instanceof DecimalType) {
            return mapPercentToValue(min.doubleValue(), max.doubleValue(), ((DecimalType) command).doubleValue());
        }
        return command;
    }

    private Command mapPercentToValue(double min, double max, double value) {
        return new DecimalType(min + Math.round(((max - min) * value) / 100.0));
    }

    private int restrictToBounds(double percentValue) {
        return (int) Math.max(0, Math.min(percentValue, 100));
    }
}
