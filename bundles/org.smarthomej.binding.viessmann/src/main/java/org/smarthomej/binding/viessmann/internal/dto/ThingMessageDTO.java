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
    private String feature;
    private String featureClear;
    private String featureName;
    private String featureDescription;
    private String deviceId;
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
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getFeatureClear() {
        return featureClear;
    }

    public void setFeatureClear(String featureClear) {
        this.featureClear = featureClear;
    }

    public String getFeatureName() {
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

    public String getChannelId() {
        StringBuilder sb = new StringBuilder();
        String f = feature.replace(".", ";");
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
        return sb.toString();
    }
}
