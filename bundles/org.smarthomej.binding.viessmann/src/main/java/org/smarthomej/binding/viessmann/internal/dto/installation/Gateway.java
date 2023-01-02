/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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
package org.smarthomej.binding.viessmann.internal.dto.installation;

import java.util.List;

/**
 * The {@link Gateway} provides all data of the gateway
 *
 * @author Ronny Grun - Initial contribution
 */
public class Gateway {
    public String serial;
    public String version;
    public Integer firmwareUpdateFailureCounter;
    public Boolean autoUpdate;
    public String createdAt;
    public String producedAt;
    public String lastStatusChangedAt;
    public String aggregatedStatus;
    public String targetRealm;
    public List<Device> devices = null;
    public String gatewayType;
    public Integer installationId;
    public String registeredAt;
    public Object description;
}
