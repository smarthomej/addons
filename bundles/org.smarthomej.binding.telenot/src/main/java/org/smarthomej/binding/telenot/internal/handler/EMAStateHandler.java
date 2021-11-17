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
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.protocol.EMAStateMessage;
import org.smarthomej.binding.telenot.internal.protocol.TelenotMessage;

/**
 * The {@link EMAStateHandler} is responsible for handling state of internally armed.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class EMAStateHandler extends TelenotThingHandler {

    private final Logger logger = LoggerFactory.getLogger(EMAStateHandler.class);

    public EMAStateHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        initDeviceState();
        logger.trace("emaState handler finished initializing");
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
    }

    @Override
    public void handleUpdate(TelenotMessage msg) {
        if (!(msg instanceof EMAStateMessage)) {
            return;
        }
        EMAStateMessage emaMsg = (EMAStateMessage) msg;

        logger.trace("emaState handler for received update: {},{}", emaMsg.date, emaMsg.contact);

        firstUpdateReceived.set(true);

        switch (emaMsg.messagetype) {
            case "INTRUSION":
                updateState(CHANNEL_INTRUSION_DATETIME, emaMsg.date);
                updateState(CHANNEL_INTRUSION_CONTACT, new StringType(emaMsg.contact));
                updateState(CHANNEL_INTRUSION_SET_CLEAR, OnOffType.from(emaMsg.alarmSetClear));
                break;
            case "BATTERY_MALFUNCTION":
                updateState(CHANNEL_BATTERY_MALFUNCTION_DATETIME, emaMsg.date);
                updateState(CHANNEL_BATTERY_MALFUNCTION_CONTACT, new StringType(emaMsg.contact));
                updateState(CHANNEL_BATTERY_MALFUNCTION_SET_CLEAR, OnOffType.from(emaMsg.alarmSetClear));
                break;
            case "POWER_OUTAGE":
                updateState(CHANNEL_POWER_OUTAGE_DATETIME, emaMsg.date);
                updateState(CHANNEL_POWER_OUTAGE_CONTACT, new StringType(emaMsg.contact));
                updateState(CHANNEL_POWER_OUTAGE_SET_CLEAR, OnOffType.from(emaMsg.alarmSetClear));
                break;
            case "OPTICAL_FLASHER_MALFUNCTION":
                updateState(CHANNEL_OPTICAL_FLASHER_MALFUNCTION_DATETIME, emaMsg.date);
                updateState(CHANNEL_OPTICAL_FLASHER_MALFUNCTION_CONTACT, new StringType(emaMsg.contact));
                updateState(CHANNEL_OPTICAL_FLASHER_MALFUNCTION_SET_CLEAR, OnOffType.from(emaMsg.alarmSetClear));
                break;
            case "HORN_1_MALFUNCTION":
                updateState(CHANNEL_HORN_1_MALFUNCTION_DATETIME, emaMsg.date);
                updateState(CHANNEL_HORN_1_MALFUNCTION_CONTACT, new StringType(emaMsg.contact));
                updateState(CHANNEL_HORN_1_MALFUNCTION_SET_CLEAR, OnOffType.from(emaMsg.alarmSetClear));
                break;
            case "HORN_2_MALFUNCTION":
                updateState(CHANNEL_HORN_2_MALFUNCTION_DATETIME, emaMsg.date);
                updateState(CHANNEL_HORN_2_MALFUNCTION_CONTACT, new StringType(emaMsg.contact));
                updateState(CHANNEL_HORN_2_MALFUNCTION_SET_CLEAR, OnOffType.from(emaMsg.alarmSetClear));
                break;
            case "COM_FAULT":
                updateState(CHANNEL_COM_FAULT_DATETIME, emaMsg.date);
                updateState(CHANNEL_COM_FAULT_CONTACT, new StringType(emaMsg.contact));
                updateState(CHANNEL_COM_FAULT_SET_CLEAR, OnOffType.from(emaMsg.alarmSetClear));
                break;
            default:
                break;
        }
    }
}
