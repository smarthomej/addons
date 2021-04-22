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
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.config.SBConfig;
import org.smarthomej.binding.telenot.internal.protocol.SBMessage;
import org.smarthomej.binding.telenot.internal.protocol.SBStateMessage;
import org.smarthomej.binding.telenot.internal.protocol.TelenotCommand;
import org.smarthomej.binding.telenot.internal.protocol.TelenotMessage;

/**
 * The {@link SBHandler} is responsible for handling state of SB.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class SBHandler extends TelenotThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SBHandler.class);

    private SBConfig config = new SBConfig();

    public SBHandler(Thing thing) {
        super(thing);
    }

    /** Construct SB id from address */
    public static final String sbID(int address) {
        return String.format("%d", address);
    }

    @Override
    public void initialize() {
        config = getConfigAs(SBConfig.class);

        if (config.address < 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid address setting");
            return;
        }
        logger.debug("SB handler initializing for address {}", config.address);

        String id = sbID(config.address);
        updateProperty(PROPERTY_ID, id); // set representation property used by discovery

        initDeviceState();
        logger.trace("SB handler finished initializing");
    }

    /**
     * Set contact channel state to "UNDEF" at init time. The real state will be set either when the first message
     * arrives for the zone, or it should be set to "CLOSED" the first time the panel goes into the "READY" state.
     */
    @Override
    public void initChannelState() {
        UnDefType state = UnDefType.UNDEF;
        updateState(CHANNEL_INT_ARMED_DATETIME, state);
        updateState(CHANNEL_EXT_ARMED_DATETIME, state);
        updateState(CHANNEL_DISARMED_DATETIME, state);
        updateState(CHANNEL_ALARM_DATETIME, state);

        updateState(CHANNEL_INT_ARMED_CONTACT, state);
        updateState(CHANNEL_EXT_ARMED_CONTACT, state);
        updateState(CHANNEL_DISARMED_CONTACT, state);
        updateState(CHANNEL_ALARM_CONTACT, state);

        updateState(CHANNEL_ALARM_SET_CLEAR, state);

        updateState(CHANNEL_DISARMED, state);
        updateState(CHANNEL_INTERNALLY_ARMED, state);
        updateState(CHANNEL_EXTERNALLY_ARMED, state);
        updateState(CHANNEL_ALARM, state);
        updateState(CHANNEL_MALFUNCTION, state);
        updateState(CHANNEL_READY_TO_ARM_INTERNALLY, state);
        updateState(CHANNEL_READY_TO_ARM_EXTERNALLY, state);
        updateState(CHANNEL_STATE_INTERNAL_SIGNAL_HORN, state);

        firstUpdateReceived.set(false);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_DISARM)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.ON) {
                    logger.debug("Received command: DISARM security area");
                    sendCommand(TelenotCommand.disarmArea(config.address));
                    // setChannelState(OnOffType.ON);
                }
            }
        } else if (channelUID.getId().equals(CHANNEL_INTERNAL_ARM)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.OFF) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: DISARM security area");
                    sendCommand(TelenotCommand.disarmArea(config.address));
                } else if (command == OnOffType.ON) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: INT_ARM security area");
                    sendCommand(TelenotCommand.intArmArea(config.address));
                }
            }
        } else if (channelUID.getId().equals(CHANNEL_EXTERNAL_ARM)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.OFF) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: DISARM security area");
                    sendCommand(TelenotCommand.disarmArea(config.address));
                } else if (command == OnOffType.ON) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: EXT_ARM security area");
                    sendCommand(TelenotCommand.extArmArea(config.address));
                }
            }
        } else if (channelUID.getId().equals(CHANNEL_RESET_ALARM)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.ON) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: RESET_ALARM security area");
                    sendCommand(TelenotCommand.resetAlarm(config.address));
                    updateState(CHANNEL_RESET_ALARM, OnOffType.OFF);
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
        if (msg instanceof SBMessage) {
            SBMessage sbMsg = (SBMessage) msg;
            if (config.address == sbMsg.address) {
                logger.trace("SB handler for {} received update: {},{},{},{},{},{},{},{}", config.address,
                        sbMsg.disarmed, sbMsg.internallyArmed, sbMsg.externallyArmed, sbMsg.alarm, sbMsg.malfunction,
                        sbMsg.readyToArmInternally, sbMsg.readyToArmExternally, sbMsg.statusInternalSignalHorn);

                firstUpdateReceived.set(true);

                updateState(CHANNEL_DISARMED, sbMsg.disarmed == 0 ? OnOffType.ON : OnOffType.OFF);
                updateState(CHANNEL_DISARM, sbMsg.disarmed == 0 ? OnOffType.ON : OnOffType.OFF);

                updateState(CHANNEL_INTERNALLY_ARMED, sbMsg.internallyArmed == 0 ? OnOffType.ON : OnOffType.OFF);
                updateState(CHANNEL_INTERNAL_ARM, sbMsg.internallyArmed == 0 ? OnOffType.ON : OnOffType.OFF);

                updateState(CHANNEL_EXTERNALLY_ARMED, sbMsg.externallyArmed == 0 ? OnOffType.ON : OnOffType.OFF);
                updateState(CHANNEL_EXTERNAL_ARM, sbMsg.externallyArmed == 0 ? OnOffType.ON : OnOffType.OFF);

                updateState(CHANNEL_ALARM, sbMsg.alarm == 0 ? OnOffType.ON : OnOffType.OFF);
                updateState(CHANNEL_MALFUNCTION, sbMsg.malfunction == 0 ? OnOffType.ON : OnOffType.OFF);
                updateState(CHANNEL_READY_TO_ARM_INTERNALLY,
                        sbMsg.readyToArmInternally == 0 ? OnOffType.ON : OnOffType.OFF);
                updateState(CHANNEL_READY_TO_ARM_EXTERNALLY,
                        sbMsg.readyToArmExternally == 0 ? OnOffType.ON : OnOffType.OFF);
                updateState(CHANNEL_STATE_INTERNAL_SIGNAL_HORN,
                        sbMsg.statusInternalSignalHorn == 0 ? OnOffType.ON : OnOffType.OFF);
            }
        } else if (msg instanceof SBStateMessage) {
            SBStateMessage emaMsg = (SBStateMessage) msg;
            if (config.address == emaMsg.address) {
                switch (emaMsg.messagetype) {
                    case "SYS_EXT_ARMED":
                        updateState(CHANNEL_EXT_ARMED_DATETIME, emaMsg.date);
                        updateState(CHANNEL_EXT_ARMED_CONTACT, new StringType(emaMsg.contact));
                        break;
                    case "SYS_INT_ARMED":
                        updateState(CHANNEL_INT_ARMED_DATETIME, emaMsg.date);
                        updateState(CHANNEL_INT_ARMED_CONTACT, new StringType(emaMsg.contact));
                        break;
                    case "SYS_DISARMED":
                        updateState(CHANNEL_DISARMED_DATETIME, emaMsg.date);
                        updateState(CHANNEL_DISARMED_CONTACT, new StringType(emaMsg.contact));
                        break;
                    case "ALARM":
                        updateState(CHANNEL_ALARM_DATETIME, emaMsg.date);
                        updateState(CHANNEL_ALARM_CONTACT, new StringType(emaMsg.contact));
                        updateState(CHANNEL_ALARM_SET_CLEAR, emaMsg.alarmSetClear ? OnOffType.ON : OnOffType.OFF);
                        if (!emaMsg.alarmSetClear) {
                            updateState(CHANNEL_RESET_ALARM, OnOffType.OFF);
                        }
                        break;
                }
            }
        } else {
            return;
        }
    }
}
