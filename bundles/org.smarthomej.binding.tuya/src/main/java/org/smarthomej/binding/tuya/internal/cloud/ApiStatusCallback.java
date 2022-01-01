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
package org.smarthomej.binding.tuya.internal.cloud;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ApiStatusCallback} is an interface for reporting API call results
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ApiStatusCallback {

    /**
     * report the status of the connection if it changes
     *
     * @param status true -> established, false -> disconnected/failed
     */
    void tuyaOpenApiStatus(boolean status);
}
