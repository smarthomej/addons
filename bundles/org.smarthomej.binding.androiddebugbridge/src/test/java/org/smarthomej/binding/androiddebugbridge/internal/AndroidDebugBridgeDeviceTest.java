/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.androiddebugbridge.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
class AndroidDebugBridgeDeviceTest {

    @Test
    void testCurrentPackagePrefixFilter() {
        Matcher currentPackageMatcher = AndroidDebugBridgeDevice.CURRENT_PACKAGE_PREFIX_FILTER_PATTERN
                .matcher("12345:org.android.avod");
        assertTrue(currentPackageMatcher.find());
        assertEquals("org.android.avod", currentPackageMatcher.group(2));

        currentPackageMatcher = AndroidDebugBridgeDevice.CURRENT_PACKAGE_PREFIX_FILTER_PATTERN
                .matcher("org.android.avod");
        assertTrue(currentPackageMatcher.find());
        assertEquals("org.android.avod", currentPackageMatcher.group(2));
    }
}
