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
import org.smarthomej.binding.telenot.internal.protocol.InputMessage;
import org.smarthomej.binding.telenot.internal.protocol.TelenotCommand;
import org.smarthomej.binding.telenot.internal.protocol.TelenotMessage;
import org.smarthomej.binding.telenot.internal.protocol.UsedContactInfoMessage;

/**
 * The {@link InputHandler} is responsible for handling OutputHandler
 **
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class InputHandler extends TelenotThingHandler {

    private final Logger logger = LoggerFactory.getLogger(InputHandler.class);

    // private InputConfig config = new InputConfig();

    public InputHandler(Thing thing) {
        super(thing);
    }

    /** Construct zone id from address */
    public static final String mbID(int address) {
        return String.format("%d", address);
    }

    @Override
    public void initialize() {
        // config = getConfigAs(InputConfig.class);

        // if (config.address < 0) {
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid address setting");
        // return;
        // }
        // logger.debug("Output handler initializing for address {}", config.address);

        // String id = mbID(config.address);
        // updateProperty(PROPERTY_ID, id); // set representation property used by discovery
        // if (config.discovery) {
        // logger.info("Start discovery input contacts");
        // sendCommand(TelenotCommand.sendUsedState());
        // Configuration conf = editConfiguration();
        // conf.put("discovery", false);
        // updateConfiguration(conf);
        // }
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
        if (!(msg instanceof UsedContactInfoMessage)) {
            return;
        }
        UsedContactInfoMessage mbMsg = (UsedContactInfoMessage) msg;
        createChannel(mbMsg.address, mbMsg.name);
        logger.trace("handleUpdateChannel: {}", mbMsg.address);
    }

    @Override
    public void handleUpdate(TelenotMessage msg) {
        if (!(msg instanceof InputMessage)) {
            return;
        }
        InputMessage inpMsg = (InputMessage) msg;
        logger.trace("Input handler received update: {}", inpMsg.address);
        firstUpdateReceived.set(true);
        OpenClosedType state = (inpMsg.data == 0 ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
        updateState(inpMsg.address, state);
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
        }
    }
}
