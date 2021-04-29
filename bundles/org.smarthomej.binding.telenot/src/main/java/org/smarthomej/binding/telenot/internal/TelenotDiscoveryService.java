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
package org.smarthomej.binding.telenot.internal;

import static org.smarthomej.binding.telenot.internal.TelenotBindingConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.handler.TelenotBridgeHandler;

/**
 * The {@link TelenotDiscoveryService} handles discovery of devices as they are identified by the bridge handler.
 * Requests from the framework to startScan() are ignored, since no active scanning is possible.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class TelenotDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(TelenotDiscoveryService.class);

    private @Nullable TelenotBridgeHandler bridgeHandler;
    private final Set<String> discoveredSBSet = new HashSet<>();

    public TelenotDiscoveryService(TelenotBridgeHandler bridgeHandler) {
        super(DISCOVERABLE_DEVICE_TYPE_UIDS, 0, false);
        this.bridgeHandler = (TelenotBridgeHandler) bridgeHandler;
    }

    @Override
    public void setThingHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof TelenotBridgeHandler) {
            this.bridgeHandler = (TelenotBridgeHandler) thingHandler;
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return DISCOVERABLE_DEVICE_TYPE_UIDS;
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    public void activate() {
        super.activate(null);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void startScan() {
        // Ignore start scan requests
    }

    public void processSB(int address) {
        String token = String.valueOf(address);
        if (!discoveredSBSet.contains(token)) {
            notifyDiscoveryOfSB(address, token);
            discoveredSBSet.add(token);
        }
    }

    private void notifyDiscoveryOfSB(int address, String idString) {
        TelenotBridgeHandler bridgeHandler = this.bridgeHandler;
        if (bridgeHandler == null) {
            logger.warn("Bridgehandler is null but discovery result has been produced. This should not happen.");
            return;
        }
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingUID uid = new ThingUID(THING_TYPE_SB, bridgeUID, idString);

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_ADDRESS, address);
        properties.put(PROPERTY_ID, idString);
        String label = "Telenot Security Area " + idString;
        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID).withProperties(properties)
                .withRepresentationProperty(PROPERTY_ID).withLabel(label).build();
        thingDiscovered(result);
        logger.debug("Discovered SB {}", uid);
    }
}
