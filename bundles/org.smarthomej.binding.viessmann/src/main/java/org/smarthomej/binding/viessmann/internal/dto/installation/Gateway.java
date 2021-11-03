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
package org.smarthomej.binding.viessmann.internal.dto.installation;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link Gateway} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class Gateway {

    @SerializedName("serial")
    @Expose
    public String serial;
    @SerializedName("version")
    @Expose
    public String version;
    @SerializedName("firmwareUpdateFailureCounter")
    @Expose
    public Integer firmwareUpdateFailureCounter;
    @SerializedName("autoUpdate")
    @Expose
    public Boolean autoUpdate;
    @SerializedName("createdAt")
    @Expose
    public String createdAt;
    @SerializedName("producedAt")
    @Expose
    public String producedAt;
    @SerializedName("lastStatusChangedAt")
    @Expose
    public String lastStatusChangedAt;
    @SerializedName("aggregatedStatus")
    @Expose
    public String aggregatedStatus;
    @SerializedName("targetRealm")
    @Expose
    public String targetRealm;
    @SerializedName("devices")
    @Expose
    public List<Device> devices = null;
    @SerializedName("gatewayType")
    @Expose
    public String gatewayType;
    @SerializedName("installationId")
    @Expose
    public Integer installationId;
    @SerializedName("registeredAt")
    @Expose
    public String registeredAt;
    @SerializedName("description")
    @Expose
    public Object description;
}
