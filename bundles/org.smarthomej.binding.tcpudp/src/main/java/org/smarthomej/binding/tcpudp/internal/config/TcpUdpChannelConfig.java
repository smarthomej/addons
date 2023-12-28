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
package org.smarthomej.binding.tcpudp.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.generic.ChannelValueConverterConfig;

/**
 * The {@link TcpUdpChannelConfig} class contains fields mapping channel configuration parameters.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TcpUdpChannelConfig extends ChannelValueConverterConfig {

    public @Nullable String stateTransformation;

    // used by client channels
    public @Nullable String commandTransformation;
    public String stateContent = "";

    // used by receiver channels
    public String addressFilter = "*";
}
