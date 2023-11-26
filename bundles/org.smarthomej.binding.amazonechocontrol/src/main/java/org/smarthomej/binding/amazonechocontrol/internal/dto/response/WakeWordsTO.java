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
package org.smarthomej.binding.amazonechocontrol.internal.dto.response;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The {@link WakeWordsTO} encapsulate the response of a request to /api/wake-word
 *
 * @author Michael Geramb - Initial contribution
 */
public class WakeWordsTO {
    public List<WakeWordTO> wakeWords = List.of();

    @Override
    public @NonNull String toString() {
        return "WakeWords{wakeWords=" + wakeWords + "}";
    }
}
