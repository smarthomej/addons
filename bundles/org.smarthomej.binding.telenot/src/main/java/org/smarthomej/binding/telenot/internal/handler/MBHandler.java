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
package org.smarthomej.binding.telenot.internal.handler;

import static org.smarthomej.binding.telenot.internal.TelenotBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.config.MBConfig;
import org.smarthomej.binding.telenot.internal.protocol.MBDMessage;
import org.smarthomej.binding.telenot.internal.protocol.MBMessage;
import org.smarthomej.binding.telenot.internal.protocol.TelenotCommand;
import org.smarthomej.binding.telenot.internal.protocol.TelenotMessage;

/**
 * The {@link MBHandler} is responsible for handling MB
 **
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class MBHandler extends TelenotThingHandler {

    private final Logger logger = LoggerFactory.getLogger(MBHandler.class);

    private MBConfig config = new MBConfig();

    public MBHandler(Thing thing) {
        super(thing);
    }

    /** Construct zone id from address */
    public static final String mbID(int address) {
        return String.format("%d", address);
    }

    @Override
    public void initialize() {
        config = getConfigAs(MBConfig.class);

        if (config.address < 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid address setting");
            return;
        }
        logger.debug("MB handler initializing for address {}", config.address);

        String id = mbID(config.address);
        updateProperty(PROPERTY_ID, id); // set representation property used by discovery

        initDeviceState();
        logger.trace("MB handler finished initializing");
    }

    /**
     * Set contact channel state to "UNDEF" at init time. The real state will be set either when the first message
     * arrives for the zone, or it should be set to "CLOSED" the first time the panel goes into the "READY" state.
     */
    @Override
    public void initChannelState() {
        UnDefType state = UnDefType.UNDEF;
        updateState(CHANNEL_CONTACT_MB, state);
        updateState(CHANNEL_DISABLE_MB, state);
        firstUpdateReceived.set(false);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_DISABLE_MB)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.OFF) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: ENABLE_REPORTING_POINT");
                    sendCommand(TelenotCommand.disableReportingPoint(config.address, 0));
                } else if (command == OnOffType.ON) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: DISABLE_REPORTING_POINT");
                    sendCommand(TelenotCommand.disableReportingPoint(config.address, 1));
                }
            }
        }
    }

    @Override
    public void handleUpdateChannel(TelenotMessage msg) {
        logger.trace("handleUpdateChannel");
    }

    @Override
    public void handleUpdate(TelenotMessage msg) {
        if (msg instanceof MBMessage) {
            MBMessage mbMsg = (MBMessage) msg;

            if (config.address == mbMsg.address) {
                logger.trace("MB handler for {} received update: {}", config.address, mbMsg.data);

                firstUpdateReceived.set(true);
                OpenClosedType state = (mbMsg.data == 0 ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
                updateState(CHANNEL_CONTACT_MB, state);
            }
        } else if (msg instanceof MBDMessage) {
            MBDMessage mbdMsg = (MBDMessage) msg;

            if (config.address == mbdMsg.address) {
                logger.trace("MBD handler for {} received update: {}", config.address, mbdMsg.data);

                firstUpdateReceived.set(true);
                updateState(CHANNEL_DISABLE_MB, mbdMsg.data == 0 ? OnOffType.ON : OnOffType.OFF);
            }
        } else {
            return;
        }
    }
}
