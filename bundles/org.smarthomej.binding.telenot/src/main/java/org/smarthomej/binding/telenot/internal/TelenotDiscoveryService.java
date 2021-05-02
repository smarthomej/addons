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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private @Nullable ScheduledFuture<?> scanningJob;
    private @Nullable TelenotBridgeHandler bridgeHandler;
    private @Nullable ThingUID bridgeUID;

    public TelenotDiscoveryService() {
        super(DISCOVERABLE_DEVICE_TYPE_UIDS, 0, true);
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
        stopScan();
        bridgeHandler.getUsedSecurityArea().forEach(this::buildDiscoveryResult);

        // we clear all older results, they are not valid any longer and we created new results
        removeOlderResults(getTimestampOfLastScan());
    }

    @Override
    protected void startBackgroundDiscovery() {
        scheduler.schedule(() -> {
            startScan();
        }, 15, TimeUnit.SECONDS);
        /*
         * final ScheduledFuture<?> scanningJob = this.scanningJob;
         * if (scanningJob == null || scanningJob.isCancelled()) {
         * this.scanningJob = scheduler.scheduleWithFixedDelay(this::startScan, 0, 20, TimeUnit.SECONDS);
         * }
         */
    }

    /*
     * @Override
     * protected void stopBackgroundDiscovery() {
     * final ScheduledFuture<?> scanningJob = this.scanningJob;
     * if (scanningJob != null) {
     * scanningJob.cancel(true);
     * this.scanningJob = null;
     * }
     * }
     */

    private void buildDiscoveryResult(String address) {
        ThingUID bridgeUID = this.bridgeUID;
        if (bridgeUID == null) {
            logger.warn("BridgeUid is not set but a discovery result has been produced. This should not happen.");
            return;
        }

        ThingUID uid = new ThingUID(THING_TYPE_SB, bridgeUID, address);
        Map<String, Object> properties = Map.ofEntries( //
                entry(PROPERTY_ADDRESS, Integer.parseInt(address)), //
                entry(PROPERTY_ID, address));
        String label = "Telenot Security Area " + address;
        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID).withProperties(properties)
                .withRepresentationProperty(PROPERTY_ID).withLabel(label).build();
        thingDiscovered(result);
        logger.debug("Discovered SB {}", uid);
    }
}
