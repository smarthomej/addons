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
package org.smarthomej.binding.knx.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.smarthomej.binding.knx.internal.client.KNXClient;
import org.smarthomej.binding.knx.internal.client.NoOpClient;
import org.smarthomej.binding.knx.internal.client.SerialClient;
import org.smarthomej.binding.knx.internal.config.SerialBridgeConfiguration;

/**
 * The {@link IPBridgeThingHandler} is responsible for handling commands, which are
 * sent to one of the channels. It implements a KNX Serial/USB Gateway, that either acts a a
 * conduit for other {@link DeviceThingHandler}s, or for Channels that are
 * directly defined on the bridge
 *
 * @author Karel Goderis - Initial contribution
 * @author Simon Kaufmann - Refactoring & cleanup
 */
@NonNullByDefault
public class SerialBridgeThingHandler extends KNXBridgeBaseThingHandler {

    private @Nullable SerialClient client;

    public SerialBridgeThingHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        SerialBridgeConfiguration config = getConfigAs(SerialBridgeConfiguration.class);
        String serialPort = config.serialPort;
        if (serialPort == null || serialPort.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "serial port not configured");
            return;
        }
        SerialClient client = new SerialClient(config.getAutoReconnectPeriod(), thing.getUID(),
                config.getResponseTimeout(), config.getReadingPause(), config.getReadRetriesLimit(), getScheduler(),
                serialPort, config.useCEMI, this);
        updateStatus(ThingStatus.UNKNOWN);
        client.initialize();
        this.client = client;
    }

    @Override
    public void dispose() {
        super.dispose();
        SerialClient client = this.client;
        if (client != null) {
            client.dispose();
        }
    }

    @Override
    protected KNXClient getClient() {
        KNXClient ret = client;
        if (ret == null) {
            return new NoOpClient();
        }
        return ret;
    }
}
