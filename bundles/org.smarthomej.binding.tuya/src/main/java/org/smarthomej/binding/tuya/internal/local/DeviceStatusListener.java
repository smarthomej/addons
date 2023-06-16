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
package org.smarthomej.binding.tuya.internal.local;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingStatusDetail;

/**
 * The {@link DeviceStatusListener} encapsulates device status data
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface DeviceStatusListener {
    void processDeviceStatus(@Nullable String cid, Map<Integer, Object> deviceStatus);

    void onConnected();

    void onDisconnected(ThingStatusDetail thingStatusDetail, @Nullable String reason);
}
