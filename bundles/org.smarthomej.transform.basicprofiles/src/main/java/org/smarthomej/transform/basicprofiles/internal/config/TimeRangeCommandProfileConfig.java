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
package org.smarthomej.transform.basicprofiles.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.transform.basicprofiles.internal.profiles.TimeRangeCommandProfile;

/**
 * Configuration for {@link TimeRangeCommandProfile}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class TimeRangeCommandProfileConfig {
    public int inRangeValue = 100;
    public int outOfRangeValue = 30;
    public @NonNullByDefault({}) String start;
    public @NonNullByDefault({}) String end;
    public String restoreValue = TimeRangeCommandProfile.CONFIG_RESTORE_VALUE_OFF;
}
