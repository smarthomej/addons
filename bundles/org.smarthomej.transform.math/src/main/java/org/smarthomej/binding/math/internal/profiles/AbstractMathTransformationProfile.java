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
package org.smarthomej.binding.math.internal.profiles;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for {@link StateProfile}s which applies simple math on the item state.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractMathTransformationProfile implements StateProfile {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final TransformationService service;
    protected final ProfileCallback callback;

    private final ProfileTypeUID profileTypeUID;

    public AbstractMathTransformationProfile(ProfileCallback callback, TransformationService service,
            ProfileTypeUID profileTypeUID) {
        this.service = service;
        this.callback = callback;
        this.profileTypeUID = profileTypeUID;
    }

    protected @Nullable String getParam(ProfileContext context, String param) {
        Object paramValue = context.getConfiguration().get(param);
        logger.debug("Profile configured with '{}'='{}'", param, paramValue);
        if (paramValue instanceof String) {
            return (String) paramValue;
        } else if (paramValue instanceof BigDecimal) {
            final BigDecimal value = (BigDecimal) paramValue;
            return value.toPlainString();
        } else {
            logger.error("Parameter '{}' has to be a BigDecimal or a String. Profile will be inactive.", param);
            return null;
        }
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return profileTypeUID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onCommandFromItem(Command command) {
        callback.handleCommand(command);
    }

    protected Type transformState(Type state, String value) {
        String result = state.toFullString();
        try {
            result = TransformationHelper.transform(service, value, "%s", result);
        } catch (TransformationException e) {
            logger.warn("Could not apply math transformation state '{}' with value '{}'.", state, value);
        }
        Type resultType = state;
        if (result != null) {
            if (state instanceof DecimalType) {
                resultType = DecimalType.valueOf(result);
            } else if (state instanceof QuantityType) {
                resultType = new QuantityType<>(result);
            }
            logger.debug("Transformed '{}' into '{}'", state, resultType);
        }
        return resultType;
    }
}
