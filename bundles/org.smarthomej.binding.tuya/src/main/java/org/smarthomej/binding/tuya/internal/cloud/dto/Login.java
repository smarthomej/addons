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
import org.smarthomej.binding.tuya.internal.config.ProjectConfiguration;
import org.smarthomej.binding.tuya.internal.util.CryptoUtil;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link Login} encapsulates login data
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@SuppressWarnings("unused")
public class Login {
    public String username;
    public String password;

    @SerializedName("country_code")
    public Integer countryCode;
    public String schema;

    public Login(String username, String password, Integer countryCode, String schema) {
        this.username = username;
        this.password = CryptoUtil.md5(password);
        this.countryCode = countryCode;
        this.schema = schema;
    }

    public static Login fromProjectConfiguration(ProjectConfiguration config) {
        return new Login(config.username, config.password, config.countryCode, config.schema);
    }
}
