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

import java.util.ArrayList;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link FeatureCommands} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class FeatureCommands {

    @SerializedName("setName")
    @Expose
    public FeatureSetName setName;
    @SerializedName("setCurve")
    @Expose
    public FeatureSetCurve setCurve;
    @SerializedName("setSchedule")
    @Expose
    public FeatureSetSchedule setSchedule;
    @SerializedName("setMode")
    @Expose
    public FeatureSetMode setMode;
    @SerializedName("setTemperature")
    @Expose
    public FeatureSetTemperature setTemperature;
    @SerializedName("activate")
    @Expose
    public FeatureDefaultCommands activate;
    @SerializedName("deactivate")
    @Expose
    public FeatureDefaultCommands deactivate;
    @SerializedName("changeEndDate")
    @Expose
    public FeatureChangeEndDate changeEndDate;
    @SerializedName("schedule")
    @Expose
    public FeatureSchedule schedule;
    @SerializedName("unschedule")
    @Expose
    public FeatureDefaultCommands unschedule;
    @SerializedName("setTargetTemperature")
    @Expose
    public FeatureSetTargetTemperature setTargetTemperature;

    public ArrayList<String> getUsedCommands() {
        ArrayList<String> list = new ArrayList<String>();

        if (setName != null) {
            list.add("setName");
        }
        if (setCurve != null) {
            list.add("setCurve");
        }
        if (setSchedule != null) {
            list.add("setSchedule");
        }
        if (setMode != null) {
            list.add("setMode");
        }
        if (setTemperature != null) {
            list.add("setTemperature");
        }
        if (activate != null) {
            list.add("activate");
        }
        if (deactivate != null) {
            list.add("deactivate");
        }
        if (changeEndDate != null) {
            list.add("changeEndDate");
        }
        if (schedule != null) {
            list.add("schedule");
        }
        if (unschedule != null) {
            list.add("unschedule");
        }
        if (setTargetTemperature != null) {
            list.add("setTargetTemperature");
        }
        return list;
    }
}
