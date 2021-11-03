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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.smarthomej.binding.viessmann.internal.dto.features.FeatureCommands;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Superclass for all Thing message types.
 * 
 * @author Ronny Grun - Initial contribution
 */
public class ThingMessageDTO {

    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("channelType")
    @Expose
    private String channelType;
    @SerializedName("uom")
    @Expose
    private String uom;
    @SerializedName("value")
    @Expose
    private String value;
    @SerializedName("feature")
    @Expose
    private String feature;
    @SerializedName("featureClear")
    @Expose
    private String featureClear;
    @SerializedName("featureName")
    @Expose
    private String featureName;
    @SerializedName("featureDescription")
    @Expose
    private String featureDescription;
    @SerializedName("deviceId")
    @Expose
    private String deviceId;
    @SerializedName("commands")
    @Expose
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
                sb.append(str.substring(0, 1).toUpperCase() + str.substring(1));
            } else {
                sb.append(str);
            }
            count++;
        }
        return sb.toString();
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ThingMessageDTO.class.getName()).append('@')
                .append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null) ? "<null>" : this.type));
        sb.append(',');
        sb.append("uom");
        sb.append('=');
        sb.append(((this.uom == null) ? "<null>" : this.uom));
        sb.append(',');
        sb.append("value");
        sb.append('=');
        sb.append(((this.value == null) ? "<null>" : this.value));
        sb.append(',');
        sb.append("feature");
        sb.append('=');
        sb.append(((this.feature == null) ? "<null>" : this.feature));
        sb.append(',');
        sb.append("featureName");
        sb.append('=');
        sb.append(((this.featureName == null) ? "<null>" : this.featureName));
        sb.append(',');
        sb.append("featureDescription");
        sb.append('=');
        sb.append(((this.featureDescription == null) ? "<null>" : this.featureDescription));
        sb.append(',');
        sb.append("deviceId");
        sb.append('=');
        sb.append(((this.deviceId == null) ? "<null>" : this.deviceId));
        sb.append(',');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.uom == null) ? 0 : this.uom.hashCode()));
        result = ((result * 31) + ((this.type == null) ? 0 : this.type.hashCode()));
        result = ((result * 31) + ((this.feature == null) ? 0 : this.feature.hashCode()));
        result = ((result * 31) + ((this.value == null) ? 0 : this.value.hashCode()));
        result = ((result * 31) + ((this.featureName == null) ? 0 : this.featureName.hashCode()));
        result = ((result * 31) + ((this.featureDescription == null) ? 0 : this.featureDescription.hashCode()));
        result = ((result * 31) + ((this.deviceId == null) ? 0 : this.deviceId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ThingMessageDTO)) {
            return false;
        }
        ThingMessageDTO rhs = ((ThingMessageDTO) other);
        return ((((((((this.uom.equals(rhs.uom)) || ((this.uom != null) && this.uom.equals(rhs.uom)))
                && ((this.feature.equals(rhs.feature)) || ((this.feature != null) && this.feature.equals(rhs.feature))))
                && ((this.featureName.equals(rhs.featureName))
                        || ((this.featureName != null) && this.featureName.equals(rhs.featureName))))
                && ((this.featureDescription.equals(rhs.featureDescription)) || ((this.featureDescription != null)
                        && this.featureDescription.equals(rhs.featureDescription))))
                && ((this.type.equals(rhs.type)) || ((this.type != null) && this.type.equals(rhs.type))))
                && ((this.value.equals(rhs.value)) || ((this.value != null) && this.value.equals(rhs.value))))
                && ((this.deviceId.equals(rhs.deviceId))
                        || ((this.deviceId != null) && this.deviceId.equals(rhs.deviceId))));
    }
}
