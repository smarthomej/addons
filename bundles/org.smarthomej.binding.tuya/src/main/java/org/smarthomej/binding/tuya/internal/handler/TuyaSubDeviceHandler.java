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
package org.smarthomej.binding.tuya.internal.handler;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusInfo;
import org.smarthomej.binding.tuya.internal.local.TuyaDeviceManager;
import org.smarthomej.binding.tuya.internal.util.SchemaDp;
import org.smarthomej.commons.SimpleDynamicCommandDescriptionProvider;

/**
 * The {@link TuyaSubDeviceHandler} handles commands and state updates for sub device
 *
 * @author Vitalii Herhel - Initial contribution
 */
@NonNullByDefault
public class TuyaSubDeviceHandler extends BaseTuyaDeviceHandler {

    public TuyaSubDeviceHandler(Thing thing, @Nullable List<SchemaDp> schemaDps,
            SimpleDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider) {
        super(thing, schemaDps, dynamicCommandDescriptionProvider);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (tuyaDeviceManager != null) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        Bridge bridge = getBridge();
        if (bridge != null) {
            TuyaGatewayHandler gatewayHandler = (TuyaGatewayHandler) bridge.getHandler();
            if (gatewayHandler != null) {
                if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
                    gatewayHandler.registerSubDevice(this);
                } else {
                    gatewayHandler.deregisterSubDevice(this);
                }
            }
        }
    }

    public BaseTuyaDeviceHandler setTuyaDeviceManager(@Nullable TuyaDeviceManager tuyaDeviceManager) {
        this.tuyaDeviceManager = tuyaDeviceManager;
        if (tuyaDeviceManager == null) {
            updateStatus(ThingStatus.OFFLINE);
        } else {
            updateStatus(ThingStatus.ONLINE);
        }
        return this;
    }
}
