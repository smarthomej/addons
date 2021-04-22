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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.protocol.MBDMessage;
import org.smarthomej.binding.telenot.internal.protocol.MBMessage;
import org.smarthomej.binding.telenot.internal.protocol.TelenotCommand;
import org.smarthomej.binding.telenot.internal.protocol.TelenotMessage;
import org.smarthomej.binding.telenot.internal.protocol.UsedMbMessage;

/**
 * The {@link OutputHandler} is responsible for handling OutputHandler
 **
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class OutputHandler extends TelenotThingHandler {

    private final Logger logger = LoggerFactory.getLogger(OutputHandler.class);
    private static String regexDisable = "(0x)([0-9A-Fa-f]){4}";

    // private MBConfig config = new MBConfig();

    public OutputHandler(Thing thing) {
        super(thing);
    }

    /** Construct zone id from address */
    public static final String mbID(int address) {
        return String.format("%d", address);
    }

    @Override
    public void initialize() {
        // config = getConfigAs(MBConfig.class);

        // if (config.address < 0) {
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid address setting");
        // return;
        // }
        // logger.debug("Output handler initializing for address {}", config.address);

        // String id = mbID(config.address);
        // updateProperty(PROPERTY_ID, id); // set representation property used by discovery

        initDeviceState();
        logger.trace("Input handler finished initializing");
    }

    /**
     * Set contact channel state to "UNDEF" at init time. The real state will be set either when the first message
     * arrives for the zone, or it should be set to "CLOSED" the first time the panel goes into the "READY" state.
     */
    @Override
    public void initChannelState() {
        // UnDefType state = UnDefType.UNDEF;
        // updateState(GET_USED_STATE, state);
        // updateState(CHANNEL_DISABLE_MB, state);
        // firstUpdateReceived.set(false);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String cuid = channelUID.getId();
        if (cuid.matches(regexDisable)) {
            String parts[] = cuid.split("0x");
            if (command instanceof OnOffType) {
                if (command == OnOffType.OFF) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: ENABLE_REPORTING_POINT for address: {}", parts[1]);
                    sendCommand(TelenotCommand.disableHexReportingPoint(parts[1], 0));
                } else if (command == OnOffType.ON) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: DISABLE_REPORTING_POINT for address: {}", parts[1]);
                    sendCommand(TelenotCommand.disableHexReportingPoint(parts[1], 1));
                }
            }
        }
        logger.trace("CUID: {}", cuid);
        if (channelUID.getId().equals(GET_USED_STATE)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.ON) {
                    sendCommand(TelenotCommand.sendUsedState());
                }
            }
        }
    }

    @Override
    public void handleUpdateChannel(TelenotMessage msg) {
        if (!(msg instanceof UsedMbMessage)) {
            return;
        }
        UsedMbMessage mbMsg = (UsedMbMessage) msg;
        createChannel(mbMsg.address, mbMsg.name);
        logger.trace("handleUpdateChannel: {}", mbMsg.address);
    }

    @Override
    public void handleUpdate(TelenotMessage msg) {
        // InputMessage inpMsg = (InputMessage) msg;
        // logger.trace("Output handler received update: {}", inpMsg.address);
        // firstUpdateReceived.set(true);
        // OpenClosedType state = (inpMsg.data == 0 ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
        // updateState(inpMsg.address, state);

        if (msg instanceof MBMessage) {
            MBMessage mbMsg = (MBMessage) msg;
            String addr = Integer.toHexString(mbMsg.address + 1391);
            String hex = String.format("%s" + "%0" + (4 - addr.length()) + "d%s", "0x", 0, addr);
            OpenClosedType state = (mbMsg.data == 0 ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
            updateState(hex, state);
        } else if (msg instanceof MBDMessage) {
            MBDMessage mbdMsg = (MBDMessage) msg;
            String addr = Integer.toHexString(mbdMsg.address + 1519);
            String hex = String.format("%s" + "%0" + (4 - addr.length()) + "d%s", "0x", 0, addr);
            OnOffType state = (mbdMsg.data == 0 ? OnOffType.ON : OnOffType.OFF);
            updateState(hex, state);
        } else {
            return;
        }
    }

    /**
     * Creates new channels for the thing.
     *
     * @param channelId ID of the channel to be created.
     */
    private void createChannel(String channelId, @Nullable String label) {
        if (label == null) {
            label = "Contact " + channelId;
        }
        ThingHandlerCallback callback = getCallback();
        if (callback != null) {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), channelId);
            Channel channel = callback.createChannelBuilder(channelUID, CHANNEL_TYPE_CONTACT).withLabel(label).build();
            updateThing(editThing().withoutChannel(channelUID).withChannel(channel).build());

            String parts[] = channelId.split("0x");
            String addr = Integer.toHexString(Integer.parseInt(parts[1], 16) + 128);
            String hex = String.format("%s" + "%0" + (4 - addr.length()) + "d%s", "0x", 0, addr);

            channelUID = new ChannelUID(thing.getUID(), hex);
            channel = callback.createChannelBuilder(channelUID, CHANNEL_TYPE_SWITCH).withLabel("Disable " + label)
                    .build();
            updateThing(editThing().withoutChannel(channelUID).withChannel(channel).build());

        }
    }
}
