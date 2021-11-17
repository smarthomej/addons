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
package org.smarthomej.binding.viessmann.internal.dto.features;

import java.util.List;

/**
 * The {@link FeatureDataDTO} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class FeatureDataDTO {
    public Integer apiVersion;
    public Boolean isEnabled;
    public Boolean isReady;
    public String gatewayId;
    public String feature;
    public String uri;
    public String deviceId;
    public String timestamp;
    public FeatureProperties properties;
    public FeatureCommands commands;
    public List<String> components = null;
}
