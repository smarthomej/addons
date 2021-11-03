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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link ExtendedPayload} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class ExtendedPayload {

    @SerializedName("reason")
    @Expose
    private String reason;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("requestCountLimit")
    @Expose
    private Integer requestCountLimit;
    @SerializedName("clientId")
    @Expose
    private String clientId;
    @SerializedName("userId")
    @Expose
    private String userId;
    @SerializedName("limitReset")
    @Expose
    private Long limitReset;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRequestCountLimit() {
        return requestCountLimit;
    }

    public void setRequestCountLimit(Integer requestCountLimit) {
        this.requestCountLimit = requestCountLimit;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getLimitReset() {
        return limitReset;
    }

    public String getLimitRestetDateTime() {
        ZonedDateTime d = Instant.ofEpochMilli(limitReset).atZone(ZoneId.systemDefault());
        return d.toLocalDateTime().toString();
    }

    public void setLimitReset(Long limitReset) {
        this.limitReset = limitReset;
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ExtendedPayload.class.getName()).append('@')
                .append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("reason");
        sb.append('=');
        sb.append(((this.reason == null) ? "<null>" : this.reason));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null) ? "<null>" : this.name));
        sb.append(',');
        sb.append("requestCountLimit");
        sb.append('=');
        sb.append(((this.requestCountLimit == null) ? "<null>" : this.requestCountLimit));
        sb.append(',');
        sb.append("clientId");
        sb.append('=');
        sb.append(((this.clientId == null) ? "<null>" : this.clientId));
        sb.append(',');
        sb.append("userId");
        sb.append('=');
        sb.append(((this.userId == null) ? "<null>" : this.userId));
        sb.append(',');
        sb.append("limitReset");
        sb.append('=');
        sb.append(((this.limitReset == null) ? "<null>" : this.limitReset));
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
        result = ((result * 31) + ((this.reason == null) ? 0 : this.reason.hashCode()));
        result = ((result * 31) + ((this.name == null) ? 0 : this.name.hashCode()));
        result = ((result * 31) + ((this.clientId == null) ? 0 : this.clientId.hashCode()));
        result = ((result * 31) + ((this.limitReset == null) ? 0 : this.limitReset.hashCode()));
        result = ((result * 31) + ((this.requestCountLimit == null) ? 0 : this.requestCountLimit.hashCode()));
        result = ((result * 31) + ((this.userId == null) ? 0 : this.userId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ExtendedPayload)) {
            return false;
        }
        ExtendedPayload rhs = ((ExtendedPayload) other);
        return (((((((this.reason.equals(rhs.reason)) || ((this.reason != null) && this.reason.equals(rhs.reason)))
                && ((this.name.equals(rhs.name)) || ((this.name != null) && this.name.equals(rhs.name)))
                && ((this.clientId.equals(rhs.clientId))
                        || ((this.clientId != null) && this.clientId.equals(rhs.clientId))))
                && ((this.limitReset.equals(rhs.limitReset))
                        || ((this.limitReset != null) && this.limitReset.equals(rhs.limitReset))))
                && ((this.requestCountLimit.equals(rhs.requestCountLimit))
                        || ((this.requestCountLimit != null) && this.requestCountLimit.equals(rhs.requestCountLimit))))
                && ((this.userId.equals(rhs.userId)) || ((this.userId != null) && this.userId.equals(rhs.userId)))));
    }
}
