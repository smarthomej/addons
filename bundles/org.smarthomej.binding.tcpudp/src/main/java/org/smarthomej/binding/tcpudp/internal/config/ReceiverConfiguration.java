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

/**
 * The {@link ReceiverConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ReceiverConfiguration {
    public String localAddress = "0.0.0.0";
    public int port = 0;

    public int bufferSize = 2048;

    public Protocol protocol = Protocol.TCP;

    public @Nullable String encoding = null;

    public enum Protocol {
        UDP,
        TCP
    }
}
