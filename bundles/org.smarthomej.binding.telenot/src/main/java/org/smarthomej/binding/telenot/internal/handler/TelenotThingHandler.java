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
package org.smarthomej.binding.telenot.internal.handler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.protocol.TelenotCommand;
import org.smarthomej.binding.telenot.internal.protocol.TelenotMessage;

/**
 * {@link TelenotThingHandler} is the abstract base class for all Telenot thing handlers.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public abstract class TelenotThingHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(TelenotThingHandler.class);
    protected final AtomicBoolean firstUpdateReceived = new AtomicBoolean(false);
    protected static AtomicBoolean readyToSendData = new AtomicBoolean(false);

    public TelenotThingHandler(Thing thing) {
        super(thing);
    }

    /**
     * Initialize device state and set status for handler. Should be called at the end of initialize(). Also called by
     * bridgeStatusChanged() when bridge status changes from OFFLINE to ONLINE. Calls initChannelState() to initialize
     * channels if setting status to ONLINE.
     */
    protected void initDeviceState() {
        logger.trace("Initializing device state");
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge configured");
        } else if (bridge.getStatus() == ThingStatus.ONLINE) {
            initChannelState();
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    /**
     * Initialize channel states if necessary
     */
    public abstract void initChannelState();

    /**
     * Notify handler of a message from the Telenot via the bridge
     *
     * @param msg The TelenotMessage to handle
     */
    public abstract void handleUpdate(TelenotMessage msg);

    /**
     * Notify handler of a channel message from the Telenot via the bridge
     *
     * @param msg The TelenotMessage to handle
     */
    public abstract void handleUpdateChannel(TelenotMessage msg);

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        ThingStatus bridgeStatus = bridgeStatusInfo.getStatus();
        logger.debug("Bridge status changed to {} for Telenot handler", bridgeStatus);

        if (bridgeStatus == ThingStatus.ONLINE
                && getThing().getStatusInfo().getStatusDetail() == ThingStatusDetail.BRIDGE_OFFLINE) {
            initDeviceState();
        } else if (bridgeStatus == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    /**
     * Send a command via the bridge
     *
     * @param command command to send
     */
    public void sendCommand(TelenotCommand command) {
        boolean wait = true;
        long timeOut = System.currentTimeMillis() + 20 * 1000;
        while (!readyToSendData.get()) {
            if (wait) {
                logger.debug("waiting for ready to send data");
                wait = false;
            }
            if (System.currentTimeMillis() > timeOut) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "The bridge connection timed out");
                return;
            }
        }
        Bridge bridge = getBridge();
        TelenotBridgeHandler bridgeHandler = bridge == null ? null : (TelenotBridgeHandler) bridge.getHandler();

        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_MISSING_ERROR, "No bridge associated");
        } else {
            bridgeHandler.sendTelenotCommand(command);
        }
    }
}
