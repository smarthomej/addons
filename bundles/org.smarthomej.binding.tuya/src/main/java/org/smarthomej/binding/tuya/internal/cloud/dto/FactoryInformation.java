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
package org.smarthomej.binding.tuya.internal.cloud.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link FactoryInformation} encapsulates the reported factory information
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class FactoryInformation {
    public String id = "";
    public String mac = "";
    public String uuid = "";

    @Override
    public String toString() {
        return "FactoryInformation{id='" + id + "', mac='" + mac + "', uuid='" + uuid + "'}";
    }
}
