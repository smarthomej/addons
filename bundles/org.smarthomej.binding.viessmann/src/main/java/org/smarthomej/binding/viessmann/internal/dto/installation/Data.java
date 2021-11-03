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
 * The {@link Data} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class Data {

    @SerializedName("id")
    @Expose
    public Integer id;
    @SerializedName("description")
    @Expose
    public String description;
    @SerializedName("address")
    @Expose
    public Address address;
    @SerializedName("gateways")
    @Expose
    public List<Gateway> gateways = null;
    @SerializedName("registeredAt")
    @Expose
    public String registeredAt;
    @SerializedName("updatedAt")
    @Expose
    public String updatedAt;
    @SerializedName("aggregatedStatus")
    @Expose
    public String aggregatedStatus;
    @SerializedName("servicedBy")
    @Expose
    public Object servicedBy;
    @SerializedName("heatingType")
    @Expose
    public Object heatingType;
    @SerializedName("ownedByMaintainer")
    @Expose
    public Boolean ownedByMaintainer;
    @SerializedName("endUserWlanCommissioned")
    @Expose
    public Boolean endUserWlanCommissioned;
    @SerializedName("withoutViCareUser")
    @Expose
    public Boolean withoutViCareUser;
    @SerializedName("installationType")
    @Expose
    public String installationType;
}
