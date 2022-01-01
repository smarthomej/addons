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
package org.smarthomej.binding.amazonechocontrol.internal.websocket;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonPushCommand;

/**
 * The {@link WebsocketMessageContent} encapsulates the content of an incoming websocket message
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class WebsocketMessageContent {
    public String messageType = "";
    public String protocolVersion = "";
    public String connectionUUID = "";
    public long established;
    public long timestampINI;
    public long timestampACK;
    public String subMessageType = "";
    public long channel;
    public String destinationIdentityUrn = "";
    public String deviceIdentityUrn = "";
    public @Nullable String payload;
    public byte[] payloadData = new byte[0];
    public @Nullable JsonPushCommand pushCommand;
}
