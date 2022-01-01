/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.http.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.smarthomej.commons.itemvalueconverter.ItemValueConverterChannelConfig;

/**
 * The {@link HttpChannelConfig} class contains fields mapping channel configuration parameters.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class HttpChannelConfig extends ItemValueConverterChannelConfig {

    public @Nullable String stateExtension;
    public @Nullable String commandExtension;
    public @Nullable String stateTransformation;
    public @Nullable String commandTransformation;
    public String stateContent = "";
}
