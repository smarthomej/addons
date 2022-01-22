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
package org.smarthomej.binding.viessmann.internal.dto.features;

import java.util.ArrayList;

/**
 * The {@link FeatureCommands} provides command of features
 *
 * @author Ronny Grun - Initial contribution
 */
public class FeatureCommands {
    public FeatureSetName setName;
    public FeatureSetCurve setCurve;
    public FeatureSetSchedule setSchedule;
    public FeatureSetMode setMode;
    public FeatureSetTemperature setTemperature;
    public FeatureDefaultCommands activate;
    public FeatureDefaultCommands deactivate;
    public FeatureChangeEndDate changeEndDate;
    public FeatureSchedule schedule;
    public FeatureDefaultCommands unschedule;
    public FeatureSetTargetTemperature setTargetTemperature;
    public FeatureSetTargetTemperature setMin;
    public FeatureSetTargetTemperature setMax;
    public FeatureSetLevels setLevels;
    public FeatureSetHysteresis setHysteresis;

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
        if (setHysteresis != null) {
            list.add("setHysteresis");
        }
        return list;
    }
}
