/**
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
package org.smarthomej.transform.basicprofiles.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.transform.basicprofiles.internal.profiles.DebounceCountingStateProfile;

/**
 * Configuration for {@link DebounceCountingStateProfile}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DebounceTimeStateProfileConfig {
    public int toHandlerDelay = 0;
    public int toItemDelay = 0;
}
