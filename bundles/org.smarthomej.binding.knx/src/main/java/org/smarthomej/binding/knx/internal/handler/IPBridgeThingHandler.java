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

import java.net.InetSocketAddress;
import java.text.MessageFormat;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.knx.internal.KNXBindingConstants;
import org.smarthomej.binding.knx.internal.client.CustomKNXNetworkLinkIP;
import org.smarthomej.binding.knx.internal.client.IPClient;
import org.smarthomej.binding.knx.internal.client.KNXClient;
import org.smarthomej.binding.knx.internal.client.NoOpClient;
import org.smarthomej.binding.knx.internal.config.IPBridgeConfiguration;

/**
 * The {@link IPBridgeThingHandler} is responsible for handling commands, which are
 * sent to one of the channels. It implements a KNX/IP Gateway, that either acts as a
 * conduit for other {@link DeviceThingHandler}s, or for Channels that are
 * directly defined on the bridge
 *
 * @author Karel Goderis - Initial contribution
 * @author Simon Kaufmann - Refactoring & cleanup
 */
@NonNullByDefault
public class IPBridgeThingHandler extends KNXBridgeBaseThingHandler {
    private static final String MODE_ROUTER = "ROUTER";
    private static final String MODE_TUNNEL = "TUNNEL";

    private final Logger logger = LoggerFactory.getLogger(IPBridgeThingHandler.class);

    private @Nullable IPClient client;
    private final NetworkAddressService networkAddressService;

    public IPBridgeThingHandler(Bridge bridge, NetworkAddressService networkAddressService) {
        super(bridge);
        this.networkAddressService = networkAddressService;
    }

    @Override
    public void initialize() {
        IPBridgeConfiguration config = getConfigAs(IPBridgeConfiguration.class);
        int autoReconnectPeriod = config.getAutoReconnectPeriod();
        if (autoReconnectPeriod != 0 && autoReconnectPeriod < 30) {
            logger.info("autoReconnectPeriod for {} set to {}s, allowed range is 0 (never) or >30", thing.getUID(),
                    autoReconnectPeriod);
            autoReconnectPeriod = 30;
            config.setAutoReconnectPeriod(autoReconnectPeriod);
        }
        String localSource = config.getLocalSourceAddr();
        String connectionTypeString = config.getType();
        int port = config.getPortNumber();
        String ip = config.getIpAddress();
        InetSocketAddress localEndPoint = null;
        boolean useNAT = false;
        int ipConnectionType;
        if (MODE_TUNNEL.equalsIgnoreCase(connectionTypeString)) {
            useNAT = config.getUseNAT();
            ipConnectionType = CustomKNXNetworkLinkIP.TUNNELING;
        } else if (MODE_ROUTER.equalsIgnoreCase(connectionTypeString)) {
            if (ip == null || ip.isEmpty()) {
                ip = KNXBindingConstants.DEFAULT_MULTICAST_IP;
            }
            ipConnectionType = CustomKNXNetworkLinkIP.ROUTING;
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    MessageFormat.format("Unknown IP connection type {0}. Known types are either 'TUNNEL' or 'ROUTER'",
                            connectionTypeString));
            return;
        }
        if (ip == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The 'ipAddress' of the gateway must be configured in 'TUNNEL' mode");
            return;
        }

        String localIp = config.getLocalIp();
        if (localIp != null && !localIp.isEmpty()) {
            localEndPoint = new InetSocketAddress(localIp, 0);
        } else {
            localEndPoint = new InetSocketAddress(networkAddressService.getPrimaryIpv4HostAddress(), 0);
        }

        updateStatus(ThingStatus.UNKNOWN);
        IPClient client = new IPClient(ipConnectionType, ip, localSource, port, localEndPoint, useNAT,
                autoReconnectPeriod, thing.getUID(), config.getResponseTimeout(), config.getReadingPause(),
                config.getReadRetriesLimit(), getScheduler(), this);
        client.initialize();

        this.client = client;
    }

    @Override
    public void dispose() {
        super.dispose();
        IPClient client = this.client;
        if (client != null) {
            client.dispose();
            this.client = null;
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
