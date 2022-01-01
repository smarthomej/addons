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
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.TelenotCommandException;
import org.smarthomej.binding.telenot.internal.config.ThingsConfig;
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

    private ThingsConfig config = new ThingsConfig();

    public SBHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(ThingsConfig.class);

        if (config.address < 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid address setting");
            return;
        }
        logger.debug("SB handler initializing for address {}", config.address);

        updateProperty(PROPERTY_ID, String.valueOf(config.address)); // set representation property used by discovery

        initDeviceState();
        logger.trace("SB handler finished initializing");
    }

    @Override
    public void initChannelState() {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_DISARM)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.ON) {
                    logger.debug("Received command: DISARM security area");
                    try {
                        sendCommand(TelenotCommand.disarmArea(config.address));
                    } catch (TelenotCommandException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        } else if (channelUID.getId().equals(CHANNEL_INTERNAL_ARM)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.OFF) {
                    logger.debug("Received command: DISARM security area");
                    try {
                        sendCommand(TelenotCommand.disarmArea(config.address));
                    } catch (TelenotCommandException e) {
                        logger.error(e.getMessage());
                    }
                } else if (command == OnOffType.ON) {
                    logger.debug("Received command: INT_ARM security area");
                    try {
                        sendCommand(TelenotCommand.intArmArea(config.address));
                    } catch (TelenotCommandException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        } else if (channelUID.getId().equals(CHANNEL_EXTERNAL_ARM)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.OFF) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: DISARM security area");
                    try {
                        sendCommand(TelenotCommand.disarmArea(config.address));
                    } catch (TelenotCommandException e) {
                        logger.error(e.getMessage());
                    }
                } else if (command == OnOffType.ON) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: EXT_ARM security area");
                    try {
                        sendCommand(TelenotCommand.extArmArea(config.address));
                    } catch (TelenotCommandException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        } else if (channelUID.getId().equals(CHANNEL_RESET_ALARM)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.ON) {
                    // sendCommand(TelenotCommand.sendNorm());
                    logger.debug("Received command: RESET_ALARM security area");
                    try {
                        sendCommand(TelenotCommand.resetAlarm(config.address));
                    } catch (TelenotCommandException e) {
                        logger.error(e.getMessage());
                    }
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

                updateState(CHANNEL_DISARMED, OnOffType.from(sbMsg.disarmed));
                updateState(CHANNEL_DISARM, OnOffType.from(sbMsg.disarmed));

                updateState(CHANNEL_INTERNALLY_ARMED, OnOffType.from(sbMsg.internallyArmed));
                updateState(CHANNEL_INTERNAL_ARM, OnOffType.from(sbMsg.internallyArmed));

                updateState(CHANNEL_EXTERNALLY_ARMED, OnOffType.from(sbMsg.externallyArmed));
                updateState(CHANNEL_EXTERNAL_ARM, OnOffType.from(sbMsg.externallyArmed));

                updateState(CHANNEL_ALARM, OnOffType.from(sbMsg.alarm));
                updateState(CHANNEL_MALFUNCTION, OnOffType.from(sbMsg.malfunction));
                updateState(CHANNEL_READY_TO_ARM_INTERNALLY, OnOffType.from(sbMsg.readyToArmInternally));
                updateState(CHANNEL_READY_TO_ARM_EXTERNALLY, OnOffType.from(sbMsg.readyToArmExternally));
                updateState(CHANNEL_STATE_INTERNAL_SIGNAL_HORN, OnOffType.from(sbMsg.statusInternalSignalHorn));
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
                        updateState(CHANNEL_ALARM_SET_CLEAR, OnOffType.from(emaMsg.alarmSetClear));
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
