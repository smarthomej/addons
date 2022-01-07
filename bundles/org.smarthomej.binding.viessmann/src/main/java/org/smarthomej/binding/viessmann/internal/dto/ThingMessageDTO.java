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
package org.smarthomej.binding.viessmann.internal.dto;

import org.smarthomej.binding.viessmann.internal.dto.features.FeatureCommands;

/**
 * Superclass for all Thing message types.
 * 
 * @author Ronny Grun - Initial contribution
 */
public class ThingMessageDTO {
    private String type;
    private String channelType;
    private String uom;
    private String value;
    private String featureClear;
    private String featureName;
    private String featureDescription;
    private String deviceId;
    private String suffix;
    private String unit;
    private FeatureCommands commands;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getChannelType() {
        return channelType;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getFeature() {
        if (suffix.isEmpty()) {
            return featureClear;
        } else {
            return featureClear + "#" + suffix;
        }
    }

    public String getFeatureClear() {
        return featureClear;
    }

    public void setFeatureClear(String featureClear) {
        this.featureClear = featureClear;
    }

    public String getFeatureName() {
        if (suffix.isEmpty() || "schedule".equals(suffix)) {
            return featureName;
        }
        return featureName + " " + suffix;
    }

    public String getFeatureNameClear() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public String getFeatureDescription() {
        return featureDescription;
    }

    public void setFeatureDescription(String featureDescription) {
        this.featureDescription = featureDescription;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public FeatureCommands getCommands() {
        return commands;
    }

    public void setCommands(FeatureCommands commands) {
        this.commands = commands;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        if ("value".equals(suffix) || "name".equals(suffix) || "entries".equals(suffix)
                || "overlapAllowed".equals(suffix)) {
            this.suffix = "";
        } else {
            this.suffix = suffix;
        }
    }

    public String getChannelId() {
        StringBuilder sb = new StringBuilder();
        String f = featureClear.replace(".", ";");
        String parts[] = f.split(";");
        int count = 0;
        for (String str : parts) {
            if (count != 0) {
                sb.append(str.substring(0, 1).toUpperCase()).append(str.substring(1));
            } else {
                sb.append(str);
            }
            count++;
        }
        if (!suffix.isEmpty()) {
            sb.append("#" + suffix);
        }
        return sb.toString();
    }

    public String getSubChannelId() {
        StringBuilder sb = new StringBuilder();
        String f = featureClear.replace(".", ";");
        String parts[] = f.split(";");
        int count = 0;
        for (String str : parts) {
            if (count != 0) {
                sb.append(str.substring(0, 1).toUpperCase()).append(str.substring(1));
            } else {
                sb.append(str);
            }
            count++;
        }
        sb.append("#" + suffix);
        return sb.toString();
    }
}
