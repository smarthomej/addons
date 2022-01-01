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
package org.smarthomej.transform.math.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.transform.TransformationService;
import org.osgi.service.component.annotations.Component;

/**
 * This {@link TransformationService} performs an OR operation on the input
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = { TransformationService.class }, property = { "openhab.transform=BITOR" })
public class BitwiseOrTransformationService extends AbstractBitwiseTransformationService {

    @Override
    long performCalculation(long source, long mask) {
        return (source | mask);
    }
}
