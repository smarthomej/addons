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

/**
 * The {@link ExtendedPayload} is responsible for
 *
 * @author Ronny Grun - Initial contribution
 */
public class ExtendedPayload {

    private String reason;

    private String name;

    private Integer requestCountLimit;

    private String clientId;

    private String userId;

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
}
