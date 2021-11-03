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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link FeatureDataDTO} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class FeatureDataDTO {

    @SerializedName("apiVersion")
    @Expose
    public Integer apiVersion;
    @SerializedName("isEnabled")
    @Expose
    public Boolean isEnabled;
    @SerializedName("isReady")
    @Expose
    public Boolean isReady;
    @SerializedName("gatewayId")
    @Expose
    public String gatewayId;
    @SerializedName("feature")
    @Expose
    public String feature;
    @SerializedName("uri")
    @Expose
    public String uri;
    @SerializedName("deviceId")
    @Expose
    public String deviceId;
    @SerializedName("timestamp")
    @Expose
    public String timestamp;
    @SerializedName("properties")
    @Expose
    public FeatureProperties properties;
    @SerializedName("commands")
    @Expose
    public FeatureCommands commands;
    @SerializedName("components")
    @Expose
    public List<String> components = null;
}
