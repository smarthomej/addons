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
package org.smarthomej.binding.amazonechocontrol.internal.jsons;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link JsonCustomerHistoryRecords} encapsulate the GSON data of the push command for push customer history record
 *
 * @author Tom Blum - Initial contribution
 */
@NonNullByDefault
public class JsonCustomerHistoryRecords {

    public @Nullable List<CustomerHistoryRecord> customerHistoryRecords;

    public static class CustomerHistoryRecord {
        public @Nullable String recordKey;
        public @Nullable String recordType;
        public @Nullable Long timestamp;
        public @Nullable String customerId;
        public @Nullable Object device;
        public @Nullable Boolean isBinaryFeedbackProvided;
        public @Nullable Boolean isFeedbackPositive;
        public @Nullable String utteranceType;
        public @Nullable String domain;
        public @Nullable String intent;
        public @Nullable String skillName;
        public @Nullable List<VoiceHistoryRecordItem> voiceHistoryRecordItems;
        public @Nullable List<Object> personsInfo;

        public static class VoiceHistoryRecordItem {
            public @Nullable String recordItemKey;
            public @Nullable String recordItemType;
            public @Nullable String utteranceId;
            public @Nullable Long timestamp;
            public @Nullable String transcriptText;
            public @Nullable String agentVisualName;
            public @Nullable List<Object> personsInfo;
        }

        public long getTimestamp() {
            return Objects.requireNonNullElse(timestamp, 0L);
        }
    }
}
