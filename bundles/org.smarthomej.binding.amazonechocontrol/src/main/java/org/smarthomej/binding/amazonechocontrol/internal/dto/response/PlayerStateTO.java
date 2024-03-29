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

import org.eclipse.jdt.annotation.NonNull;
import org.smarthomej.binding.amazonechocontrol.internal.dto.PlayerStateInfoTO;

/**
 * The {@link PlayerStateTO} encapsulate the response of a request to /api/np/player
 *
 * @author Jan N. Klug - Initial contribution
 */
public class PlayerStateTO {
    public PlayerStateInfoTO playerInfo = new PlayerStateInfoTO();

    @Override
    public @NonNull String toString() {
        return "PlayerStateTO{playerInfo=" + playerInfo + "}";
    }
}
