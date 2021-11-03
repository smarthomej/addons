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
 * The {@link FeatureWeekDays} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class FeatureWeekDays {

    @SerializedName("mon")
    @Expose
    public List<FeatureDay> mon = null;
    @SerializedName("tue")
    @Expose
    public List<FeatureDay> tue = null;
    @SerializedName("wed")
    @Expose
    public List<FeatureDay> wed = null;
    @SerializedName("thu")
    @Expose
    public List<FeatureDay> thu = null;
    @SerializedName("fri")
    @Expose
    public List<FeatureDay> fri = null;
    @SerializedName("sat")
    @Expose
    public List<FeatureDay> sat = null;
    @SerializedName("sun")
    @Expose
    public List<FeatureDay> sun = null;
}
