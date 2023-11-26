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
package org.smarthomej.binding.amazonechocontrol.internal.dto.request;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The {@link SendConversationDTO} encapsulates a new message to all devices
 *
 * @author Jan N. Klug - Initial contribution
 */
public class SendConversationDTO {
    public String conversationId;
    public String clientMessageId;
    public int messageId;
    public String time;
    public String sender;
    public String type = "message/text";
    public Map<String, Object> payload = new HashMap<>();
    public int status = 1;

    @Override
    public @NonNull String toString() {
        return "SendConversationDTO{conversationId='" + conversationId + "', clientMessageId='" + clientMessageId
                + "', messageId=" + messageId + ", nextAlarmTime='" + time + "', sender='" + sender + "', type='" + type
                + "', payload=" + payload + ", status=" + status + "}";
    }
}
