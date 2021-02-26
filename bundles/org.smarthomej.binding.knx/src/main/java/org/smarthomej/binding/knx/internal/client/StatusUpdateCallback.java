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
package org.smarthomej.binding.knx.internal.client;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;

/**
 * Callback interface which enables the KNXClient implementations to update the thing status.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
public interface StatusUpdateCallback {

    /**
     * see BaseThingHandler
     *
     * @param status
     */
    void updateStatus(ThingStatus status);

    /**
     * see BaseThingHandler
     *
     * @param status
     */
    void updateStatus(ThingStatus status, ThingStatusDetail thingStatusDetail, String message);
}
