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
package org.smarthomej.commons.transform.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.transform.TransformationHelper;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.smarthomej.commons.transform.CascadedValueTransformation;
import org.smarthomej.commons.transform.NoOpValueTransformation;
import org.smarthomej.commons.transform.ValueTransformation;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link ValueTransformationProviderImpl} implements
 * {@link org.smarthomej.commons.transform.ValueTransformationProvider}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = ValueTransformationProvider.class)
public class ValueTransformationProviderImpl implements ValueTransformationProvider {

    private final BundleContext bundleContext;

    @Activate
    @SuppressWarnings("unused")
    public ValueTransformationProviderImpl(ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
    }

    @Override
    public ValueTransformation getValueTransformation(@Nullable String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return NoOpValueTransformation.getInstance();
        }

        return new CascadedValueTransformation(pattern,
                name -> TransformationHelper.getTransformationService(bundleContext, name));
    }
}
