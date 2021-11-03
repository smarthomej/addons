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
 * The {@link Device} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class Device {

    @SerializedName("gatewaySerial")
    @Expose
    public String gatewaySerial;
    @SerializedName("id")
    @Expose
    public String id;
    @SerializedName("boilerSerial")
    @Expose
    public String boilerSerial;
    @SerializedName("boilerSerialEditor")
    @Expose
    public String boilerSerialEditor;
    @SerializedName("bmuSerial")
    @Expose
    public String bmuSerial;
    @SerializedName("bmuSerialEditor")
    @Expose
    public String bmuSerialEditor;
    @SerializedName("createdAt")
    @Expose
    public String createdAt;
    @SerializedName("editedAt")
    @Expose
    public String editedAt;
    @SerializedName("modelId")
    @Expose
    public String modelId;
    @SerializedName("status")
    @Expose
    public String status;
    @SerializedName("deviceType")
    @Expose
    public String deviceType;
    @SerializedName("roles")
    @Expose
    public List<String> roles = null;
}
