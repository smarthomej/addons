/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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
package org.smarthomej.binding.viessmann.internal.dto.features;

/**
 * The {@link FeatureSetTargetTemperature} provides set target temperature of features
 *
 * @author Ronny Grun - Initial contribution
 */
public class FeatureSetTargetTemperature {
    public String uri;
    public String name;
    public Boolean isExecutable;
    public FeatureSetTargetTemperatureParams params;
}
