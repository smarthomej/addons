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

    public InputHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        initDeviceState();
        logger.trace("Input handler finished initializing");
    }

    @Override
    public void initChannelState() {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(GET_USED_STATE)) {
            if (OnOffType.ON.equals(command)) {
                sendCommand(TelenotCommand.sendUsedState());
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
