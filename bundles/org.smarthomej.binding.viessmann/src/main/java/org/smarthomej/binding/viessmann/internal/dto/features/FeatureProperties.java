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
 * The {@link FeatureProperties} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class FeatureProperties {

    @SerializedName("value")
    @Expose
    public FeatureString value;
    @SerializedName("status")
    @Expose
    public FeatureString status;
    @SerializedName("active")
    @Expose
    public FeatureBoolean active;
    @SerializedName("name")
    @Expose
    public FeatureString name;
    @SerializedName("shift")
    @Expose
    public FeatureInteger shift;
    @SerializedName("slope")
    @Expose
    public FeatureDouble slope;
    @SerializedName("entries")
    @Expose
    public FeatureEntriesWeekDays entries;
    @SerializedName("overlapAllowed")
    @Expose
    public FeatureBoolean overlapAllowed;
    @SerializedName("temperature")
    @Expose
    public FeatureInteger temperature;
    @SerializedName("start")
    @Expose
    public FeatureString start;
    @SerializedName("end")
    @Expose
    public FeatureString end;
    @SerializedName("top")
    @Expose
    public FeatureInteger top;
    @SerializedName("middle")
    @Expose
    public FeatureInteger middle;
    @SerializedName("bottom")
    @Expose
    public FeatureInteger bottom;
    @SerializedName("day")
    @Expose
    public FeatureListDouble day;
    @SerializedName("week")
    @Expose
    public FeatureListDouble week;
    @SerializedName("month")
    @Expose
    public FeatureListDouble month;
    @SerializedName("year")
    @Expose
    public FeatureListDouble year;
    @SerializedName("unit")
    @Expose
    public FeatureString unit;

    public ArrayList<String> getUsedEntries() {
        ArrayList<String> list = new ArrayList<String>();

        if (value != null) {
            list.add("value");
        }
        if (status != null) {
            list.add("status");
        }
        if (active != null) {
            list.add("active");
        }
        if (name != null) {
            list.add("name");
        }
        if (shift != null) {
            list.add("shift");
        }
        if (slope != null) {
            list.add("slope");
        }
        if (entries != null) {
            list.add("entries");
        }
        if (overlapAllowed != null) {
            list.add("overlapAllowed");
        }
        if (temperature != null) {
            list.add("temperature");
        }
        if (start != null) {
            list.add("start");
        }
        if (end != null) {
            list.add("end");
        }
        if (top != null) {
            list.add("top");
        }
        if (middle != null) {
            list.add("middle");
        }
        if (bottom != null) {
            list.add("bottom");
        }
        if (day != null) {
            list.add("day");
        }
        if (week != null) {
            list.add("week");
        }
        if (month != null) {
            list.add("month");
        }
        if (year != null) {
            list.add("year");
        }
        if (unit != null) {
            list.add("unit");
        }

        return list;
    }
}
