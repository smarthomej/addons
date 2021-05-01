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

import static java.util.Map.entry;
import static org.smarthomej.binding.telenot.internal.TelenotBindingConstants.*;

import java.util.List;
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
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class TelenotDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(TelenotDiscoveryService.class);

    private @Nullable TelenotBridgeHandler bridgeHandler;
    private @Nullable ThingUID bridgeUID;

    public TelenotDiscoveryService(TelenotBridgeHandler bridgeHandler) {
        super(DISCOVERABLE_DEVICE_TYPE_UIDS, 0, false);
        this.bridgeHandler = (TelenotBridgeHandler) bridgeHandler;
    }

    @Override
    public void setThingHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof TelenotBridgeHandler) {
            this.bridgeHandler = (TelenotBridgeHandler) thingHandler;
            this.bridgeUID = thingHandler.getThing().getUID();
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
        TelenotBridgeHandler bridgeHandler = this.bridgeHandler;
        if (bridgeHandler == null) {
            logger.warn("Tried to scan for results but bridge handler is not set in discovery service");
            return;
        }
        buildDiscoveryResults(bridgeHandler.getUsedSecurityArea());

        // we clear all older results, they are not valid any longer and we created new results
        removeOlderResults(getTimestampOfLastScan());
    }

    private void buildDiscoveryResults(List<String> addresses) {
        TelenotBridgeHandler bridgeHandler = this.bridgeHandler;
        ThingUID bridgeUID = this.bridgeUID;
        if (bridgeHandler == null || bridgeUID == null) {
            logger.warn("Bridgehandler is null but discovery result has been produced. This should not happen.");
            return;
        }

        addresses.forEach(address -> {
            ThingUID uid = new ThingUID(THING_TYPE_SB, bridgeUID, address);

            Map<String, Object> properties = Map.ofEntries( //
                    entry(PROPERTY_ADDRESS, Integer.parseInt(address)), //
                    entry(PROPERTY_ID, address));
            String label = "Telenot Security Area " + address;
            DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID).withProperties(properties)
                    .withRepresentationProperty(PROPERTY_ID).withLabel(label).build();
            thingDiscovered(result);
            logger.debug("Discovered SB {}", uid);
        });
    }
}
