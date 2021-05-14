/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 * Copyright (c) 2021 Contributors to the SmartHome/J project
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
package org.smarthomej.binding.tr064.internal.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.smarthomej.binding.tr064.internal.dto.additions.Call;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
class CallListEntryTest {

    private static final String INTERNAL_PHONE_NUMBER = "999";
    private static final String OTHER_PHONE_NUMBER = "555-456";

    public static class ParameterSet {
        public final String dateString;
        public final String resultingDateString;

        public ParameterSet(String dateString, String resultingDateString) {
            this.dateString = dateString;
            this.resultingDateString = resultingDateString;
        }
    }

    public static Collection<Object[]> parameters() {
        return List.of(new Object[][] { //
                { new ParameterSet("15.02.21 08:20", "2021-02-15T08:20:00+01") }, //
                { new ParameterSet("15.02.21 12:20", "2021-02-15T12:20:00+01") }, //
                { new ParameterSet("15.02.21 16:20", "2021-02-15T16:20:00+01") }, //
        });
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testCallParsing(ParameterSet parameterSet) {
        Call call = new Call();
        call.setDate(parameterSet.dateString);
        call.setDuration("1:12");
        call.setType("3");
        call.setCalledNumber(OTHER_PHONE_NUMBER);
        call.setCalled(INTERNAL_PHONE_NUMBER);

        CallListEntry parsedCall = new CallListEntry(call);
        assertEquals(72, parsedCall.duration);
        assertEquals(3, parsedCall.type);

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssX").serializeNulls().create();
        assertEquals("{\"localNumber\":null,\"remoteNumber\":\"999\",\"date\":\"" + parameterSet.resultingDateString
                + "\",\"type\":3,\"duration\":72}", gson.toJson(parsedCall));
    }
}
