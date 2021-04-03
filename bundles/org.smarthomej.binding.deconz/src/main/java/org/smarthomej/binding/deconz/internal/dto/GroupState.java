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
package org.smarthomej.binding.deconz.internal.dto;

import com.google.gson.annotations.SerializedName;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link GroupState} is send by the websocket connection as well as the Rest API.
 * It is part of a {@link GroupMessage}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class GroupState {
    @SerializedName(value = "all_on")
    public boolean allOn;
    @SerializedName(value = "any_on")
    public boolean anyOn;

    @Override
    public String toString() {
        return "GroupState{" + "all_on=" + allOn + ", any_on=" + anyOn + '}';
    }
}
