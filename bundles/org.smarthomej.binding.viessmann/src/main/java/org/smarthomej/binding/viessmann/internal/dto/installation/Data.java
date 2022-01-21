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
package org.smarthomej.binding.viessmann.internal.dto.installation;

import java.util.List;

/**
 * * The {@link Data} provides all data of the installation
 *
 * @author Ronny Grun - Initial contribution
 */
public class Data {
    public Integer id;
    public String description;
    public Address address;
    public List<Gateway> gateways = null;
    public String registeredAt;
    public String updatedAt;
    public String aggregatedStatus;
    public Object servicedBy;
    public Object heatingType;
    public Boolean ownedByMaintainer;
    public Boolean endUserWlanCommissioned;
    public Boolean withoutViCareUser;
    public String installationType;
}
