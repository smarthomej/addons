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
package org.smarthomej.binding.viessmann.internal.dto.error;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link ViErrorDTO} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class ViErrorDTO {

    @SerializedName("viErrorId")
    @Expose
    private String viErrorId;
    @SerializedName("statusCode")
    @Expose
    private Integer statusCode;
    @SerializedName("errorType")
    @Expose
    private String errorType;
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("extendedPayload")
    @Expose
    private ExtendedPayload extendedPayload;

    public String getViErrorId() {
        return viErrorId;
    }

    public void setViErrorId(String viErrorId) {
        this.viErrorId = viErrorId;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ExtendedPayload getExtendedPayload() {
        return extendedPayload;
    }

    public void setExtendedPayload(ExtendedPayload extendedPayload) {
        this.extendedPayload = extendedPayload;
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ViErrorDTO.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
                .append('[');
        sb.append("viErrorId");
        sb.append('=');
        sb.append(((this.viErrorId == null) ? "<null>" : this.viErrorId));
        sb.append(',');
        sb.append("statusCode");
        sb.append('=');
        sb.append(((this.statusCode == null) ? "<null>" : this.statusCode));
        sb.append(',');
        sb.append("errorType");
        sb.append('=');
        sb.append(((this.errorType == null) ? "<null>" : this.errorType));
        sb.append(',');
        sb.append("message");
        sb.append('=');
        sb.append(((this.message == null) ? "<null>" : this.message));
        sb.append(',');
        sb.append("extendedPayload");
        sb.append('=');
        sb.append(((this.extendedPayload == null) ? "<null>" : this.extendedPayload));
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
        result = ((result * 31) + ((this.message == null) ? 0 : this.message.hashCode()));
        result = ((result * 31) + ((this.errorType == null) ? 0 : this.errorType.hashCode()));
        result = ((result * 31) + ((this.extendedPayload == null) ? 0 : this.extendedPayload.hashCode()));
        result = ((result * 31) + ((this.viErrorId == null) ? 0 : this.viErrorId.hashCode()));
        result = ((result * 31) + ((this.statusCode == null) ? 0 : this.statusCode.hashCode()));
        return result;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ViErrorDTO)) {
            return false;
        }
        ViErrorDTO rhs = ((ViErrorDTO) other);
        return ((((((this.message.equals(rhs.message)) || ((this.message != null) && this.message.equals(rhs.message)))
                && ((this.errorType.equals(rhs.errorType))
                        || ((this.errorType != null) && this.errorType.equals(rhs.errorType))))
                && ((this.extendedPayload.equals(rhs.extendedPayload))
                        || ((this.extendedPayload != null) && this.extendedPayload.equals(rhs.extendedPayload))))
                && ((this.viErrorId.equals(rhs.viErrorId))
                        || ((this.viErrorId != null) && this.viErrorId.equals(rhs.viErrorId))))
                && ((this.statusCode.equals(rhs.statusCode))
                        || ((this.statusCode != null) && this.statusCode.equals(rhs.statusCode))));
    }
}
