/**
 * Copyright (c) 2021-2022 Contributors to the SmartHome/J project
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
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.config.ThingsConfig;
import org.smarthomej.binding.telenot.internal.protocol.MPMessage;
import org.smarthomej.binding.telenot.internal.protocol.TelenotMessage;

/**
 * The {@link MPHandler} is responsible for handling MP (conventional contacts).
 **
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class MPHandler extends TelenotThingHandler {

    private final Logger logger = LoggerFactory.getLogger(MPHandler.class);

    // private MPConfig config = new MPConfig();
    private ThingsConfig config = new ThingsConfig();

    public MPHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(ThingsConfig.class);

        if (config.address < 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid address setting");
            return;
        }
        logger.debug("MP handler initializing for address {}", config.address);

        updateProperty(PROPERTY_ID, String.valueOf(config.address)); // set representation property used by discovery

        initDeviceState();
        logger.trace("MP handler finished initializing");
    }

    @Override
    public void initChannelState() {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // All channels are read-only, so ignore all commands.
    }

    @Override
    public void handleUpdateChannel(TelenotMessage msg) {
        logger.trace("handleUpdateChannel");
    }

    @Override
    public void handleUpdate(TelenotMessage msg) {
        if (!(msg instanceof MPMessage)) {
            return;
        }
        MPMessage mpMsg = (MPMessage) msg;

        if (config.address == mpMsg.address) {
            logger.trace("MP handler for {} received update: {}", config.address, mpMsg.data);

            firstUpdateReceived.set(true);
            OpenClosedType state = (mpMsg.data == 0 ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
            updateState(CHANNEL_CONTACT, state);
        }
    }
}
