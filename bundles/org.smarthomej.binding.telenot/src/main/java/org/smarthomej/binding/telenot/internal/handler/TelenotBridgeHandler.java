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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.TelenotDiscoveryService;
import org.smarthomej.binding.telenot.internal.actions.BridgeActions;
import org.smarthomej.binding.telenot.internal.protocol.EMAStateMessage;
import org.smarthomej.binding.telenot.internal.protocol.InputMessage;
import org.smarthomej.binding.telenot.internal.protocol.MBDMessage;
import org.smarthomej.binding.telenot.internal.protocol.MBMessage;
import org.smarthomej.binding.telenot.internal.protocol.MPMessage;
import org.smarthomej.binding.telenot.internal.protocol.SBMessage;
import org.smarthomej.binding.telenot.internal.protocol.SBStateMessage;
import org.smarthomej.binding.telenot.internal.protocol.TelenotCommand;
import org.smarthomej.binding.telenot.internal.protocol.TelenotMessage;
import org.smarthomej.binding.telenot.internal.protocol.TelenotMsgType;
import org.smarthomej.binding.telenot.internal.protocol.UsedContactInfoMessage;
import org.smarthomej.binding.telenot.internal.protocol.UsedMbMessage;

/**
 * Abstract base class for bridge handlers responsible for communicating with
 * the Telenot devices.
 *
 * @author Ronny Grun - Initial contribution
 * 
 */
@NonNullByDefault
public abstract class TelenotBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(TelenotBridgeHandler.class);

    // protected @Nullable ByteArrayOutputStream baos;
    protected @Nullable InputStream inputStream = null;
    protected @Nullable OutputStream outputStream = null;
    protected @Nullable BufferedReader reader = null;
    protected @Nullable InputStreamReader ireader = null;
    protected @Nullable BufferedWriter writer = null;
    protected @Nullable Thread msgReaderThread = null;
    private final Object msgReaderThreadLock = new Object();
    protected @Nullable TelenotDiscoveryService discoveryService;
    protected boolean discovery;
    protected boolean refresh;
    protected volatile @Nullable Date lastReceivedTime;
    protected volatile boolean writeException;

    protected volatile ArrayList<String> usedInputContact = new ArrayList<String>();
    protected volatile ArrayList<String> usedOutputContact = new ArrayList<String>();
    protected volatile ArrayList<String> usedSecurityArea = new ArrayList<String>();
    protected volatile ArrayList<String> usedSecurityAreaContact = new ArrayList<String>();
    protected volatile ArrayList<String> usedReportingArea = new ArrayList<String>();

    protected volatile String lastMsgReverseBinaryArrayMP[] = { "0" };
    protected volatile String lastMsgReverseBinaryArraySB[] = { "0", "0", "0", "0", "0", "0", "0", "0" };
    protected volatile String lastMsgReverseBinaryArrayMB[] = { "0" };
    protected volatile String lastMsgReverseBinaryArrayMBD[] = { "0" };

    protected @Nullable ScheduledFuture<?> connectionCheckJob;
    protected @Nullable ScheduledFuture<?> refreshSendDataJob;
    protected @Nullable ScheduledFuture<?> connectRetryJob;

    public TelenotBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void dispose() {
        logger.trace("dispose called");
        disconnect();
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singletonList(BridgeActions.class);
    }

    public void setDiscoveryService(TelenotDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Accepts no commands, so do nothing.
    }

    /**
     * Send a command to Telenot.
     *
     * @param command Command string to send including terminator
     */
    public void sendTelenotCommand(TelenotCommand command) {
        logger.debug("Sending Telenot command: {}", command.logMsg);
        try {
            OutputStream bw = outputStream;
            if (bw != null) {
                bw.write(hexStringToByteArray(command.toString()));
            }
        } catch (IOException e) {
            logger.info("Exception while sending command: {}", e.getMessage());
            writeException = true;
        }
    }

    protected abstract void connect();

    protected abstract void disconnect();

    protected void scheduleConnectRetry(long waitMinutes) {
        logger.debug("Scheduling connection retry in {} minutes", waitMinutes);
        connectRetryJob = scheduler.schedule(this::connect, waitMinutes, TimeUnit.MINUTES);
    }

    protected void startMsgReader() {
        synchronized (msgReaderThreadLock) {
            Thread mrt = new Thread(this::readerThread, "OH-binding-" + getThing().getUID() + "-TelenotReader");
            mrt.setDaemon(true);
            mrt.start();
            msgReaderThread = mrt;
        }
    }

    protected void stopMsgReader() {
        synchronized (msgReaderThreadLock) {
            Thread mrt = msgReaderThread;
            if (mrt != null) {
                logger.trace("Stopping reader thread.");
                mrt.interrupt();
                msgReaderThread = null;
            }
        }
    }

    /**
     * Method executed by message reader thread
     */
    private void readerThread() {
        logger.debug("Message reader thread started");
        String message = null;
        try {
            // read from the stream
            if (discovery) {
                sendTelenotCommand(TelenotCommand.sendUsedState());
                Configuration conf = editConfiguration();
                conf.put("discovery", false);
                updateConfiguration(conf);
                logger.info("Starting discovery");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] content = new byte[2048];
            int bytesRead = -1;
            InputStream is = this.inputStream;
            while (!Thread.interrupted() && is != null && (bytesRead = is.read(content)) != -1) {
                baos.reset();
                baos.write(content, 0, bytesRead);

                message = toHexString(baos.toByteArray());

                TelenotMsgType msgType = TelenotMsgType.getMsgType(message);
                if (msgType != TelenotMsgType.INVALID) {
                    logger.debug("Received {} message", msgType);
                    lastReceivedTime = new Date();
                }

                try {
                    switch (msgType) {
                        case SEND_NORM:
                            // Check for new channel and description
                            if (!usedInputContact.isEmpty() && TelenotThingHandler.readyToSendData.get()) {
                                String address = usedInputContact.get(0);
                                sendTelenotCommand(TelenotCommand.getContactInfo(address));
                            } else if (!usedReportingArea.isEmpty() && TelenotThingHandler.readyToSendData.get()) {
                                String address = usedReportingArea.get(0);
                                sendTelenotCommand(TelenotCommand.getContactInfo(address));
                            } else {
                                if (TelenotThingHandler.readyToSendData.get()) {
                                    TelenotThingHandler.readyToSendData.set(false);
                                    logger.trace("Disable send data");
                                }
                                sendTelenotCommand(TelenotCommand.confirmACK());
                            }
                            break;
                        case CONF_ACK:
                            TelenotThingHandler.readyToSendData.set(false);
                            break;
                        case MP:
                            parseMpMessage(msgType, message);
                            sendTelenotCommand(TelenotCommand.confirmACK());
                            break;
                        case SB:
                            parseSbMessage(msgType, message);
                            sendTelenotCommand(TelenotCommand.confirmACK());
                            TelenotThingHandler.readyToSendData.set(true);
                            refresh = false;
                            logger.trace("Ready to send data");
                            break;
                        case SYS_INT_ARMED:
                        case SYS_EXT_ARMED:
                        case SYS_DISARMED:
                        case ALARM:
                            parseSbStateMessage(msgType, message);
                            sendTelenotCommand(TelenotCommand.confirmACK());
                            break;
                        case INTRUSION:
                        case BATTERY_MALFUNCTION:
                        case POWER_OUTAGE:
                        case OPTICAL_FLASHER_MALFUNCTION:
                        case HORN_1_MALFUNCTION:
                        case HORN_2_MALFUNCTION:
                            parseEmaStateMessage(msgType, message);
                            sendTelenotCommand(TelenotCommand.confirmACK());
                            TelenotThingHandler.readyToSendData.set(true);
                            logger.trace("Ready to send data");
                            break;
                        case USED_INPUTS:
                            parseUsedInputsMessage(msgType, message);
                            sendTelenotCommand(TelenotCommand.confirmACK());
                            break;
                        case USED_OUTPUTS:
                            parseUsedOutputsMessage(msgType, message);
                            sendTelenotCommand(TelenotCommand.confirmACK());
                            TelenotThingHandler.readyToSendData.set(true);
                            logger.trace("Ready to send data");
                            break;
                        case USED_CONTACTS_INFO:
                        case USED_OUTPUT_CONTACTS_INFO:
                        case USED_SB_CONTACTS_INFO:
                        case USED_MB_CONTACTS_INFO:
                            parseUsedContactInfoMessage(msgType, message);
                            sendTelenotCommand(TelenotCommand.confirmACK());
                            TelenotThingHandler.readyToSendData.set(true);
                            break;
                        case RESTART:
                            sendTelenotCommand(TelenotCommand.confirmACK());
                            TelenotThingHandler.readyToSendData.set(true);
                            logger.trace("Ready to send data");
                            break;
                        case INVALID:
                            logger.warn("Received {} MsgType | hexString: {}", msgType, message);
                            sendTelenotCommand(TelenotCommand.confirmACK());
                            TelenotThingHandler.readyToSendData.set(true);
                            logger.trace("Ready to send data");
                            break;
                        default:
                            break;
                    }
                } catch (MessageParseException e) {
                    logger.warn("Error {} while parsing message {}. Please report bug.", e.getMessage(), message);
                }
            }

            if (message == null) {
                logger.info("End of input stream detected");
                // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Connection lost");
            }
        } catch (IOException e) {
            logger.debug("I/O error while reading from stream: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (RuntimeException e) {
            logger.warn("Runtime exception in reader thread", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } finally {
            logger.debug("Message reader thread exiting");
        }
    }

    /**
     * Parse and handle MP messages. The MP messages have
     * identical format.
     *
     * @param mt message type of incoming message
     * @param msg string containing incoming message payload
     * @throws MessageParseException
     */
    private void parseMpMessage(TelenotMsgType mt, String msg) throws MessageParseException {
        // mt is unused at the moment
        MPMessage mpMsg;
        InputMessage inpMsg;
        StringBuilder sb = new StringBuilder();

        // msg = msg.substring(24, msg.length());
        logger.trace("MP msg: {}", msg);
        msg = msg.substring(24, msg.length() < 84 ? msg.length() : 84);

        String msgReverseBinaryArray[] = hexStringToReverseBinaryArray(msg);
        int addr = 0;
        for (int i = 0; i < msgReverseBinaryArray.length; i++) {
            String d = msgReverseBinaryArray[i];
            for (int a = 1; a <= 8; a++) {
                String value = d.substring(a - 1, a);
                sb.append(addr);
                sb.append(",");
                sb.append(d.substring(a - 1, a));

                try {
                    mpMsg = new MPMessage(sb.toString());
                    inpMsg = new InputMessage(sb.toString());
                } catch (IllegalArgumentException e) {
                    throw new MessageParseException(e.getMessage());
                }
                if (lastMsgReverseBinaryArrayMP.length != msgReverseBinaryArray.length) {
                    notifyChildHandlers(mpMsg);
                    notifyChildHandlers(inpMsg);
                } else {
                    String lastValue = lastMsgReverseBinaryArrayMP[i].substring(a - 1, a);
                    if (!lastValue.equals(value) || refresh) {
                        notifyChildHandlers(mpMsg);
                        notifyChildHandlers(inpMsg);
                    }
                }
                sb.setLength(0);
                addr++;
            }
        }
        lastMsgReverseBinaryArrayMP = msgReverseBinaryArray;
    }

    /**
     * Parse and handle SB messages. The SB messages have
     * identical format.
     *
     * @param mt message type of incoming message
     * @param msg string containing incoming message payload
     * @throws MessageParseException
     */
    private void parseSbMessage(TelenotMsgType mt, String msg) throws MessageParseException {
        // mt is unused at the moment
        SBMessage sbMsg;
        StringBuilder strBuilder = new StringBuilder();

        logger.trace("SB msg: {}", msg);

        if (msg.length() < 33) {
            throw new MessageParseException("wrong SB msg length");
        }
        String msgSb = msg.substring(36, msg.length() < 52 ? msg.length() : 52);

        String msgReverseBinaryArraySb[] = hexStringToReverseBinaryArray(msgSb);
        int addr = 1;
        for (int i = 0; i < msgReverseBinaryArraySb.length; i++) {
            String d = msgReverseBinaryArraySb[i];
            String value = lastMsgReverseBinaryArraySB[i];
            if (!value.equals(d) || refresh) {
                strBuilder.append(addr);
                for (int a = 1; a <= 8; a++) {
                    strBuilder.append(",");
                    strBuilder.append(d.substring(a - 1, a));
                }
                try {
                    sbMsg = new SBMessage(strBuilder.toString());
                } catch (IllegalArgumentException e) {
                    throw new MessageParseException(e.getMessage());
                }
                notifyChildHandlers(sbMsg);
                strBuilder.setLength(0);
            }
            addr++;
        }
        lastMsgReverseBinaryArraySB = msgReverseBinaryArraySb;

        MBMessage mbMsg;
        String msgMb = msg.substring(52, 84);

        String msgReverseBinaryArrayMb[] = hexStringToReverseBinaryArray(msgMb);
        int addrMb = 1;
        for (int i = 0; i < msgReverseBinaryArrayMb.length; i++) {
            String d = msgReverseBinaryArrayMb[i];
            for (int a = 1; a <= 8; a++) {
                String value = d.substring(a - 1, a);
                strBuilder.append(addrMb);
                strBuilder.append(",");
                strBuilder.append(value);
                try {
                    mbMsg = new MBMessage(strBuilder.toString());
                } catch (IllegalArgumentException e) {
                    throw new MessageParseException(e.getMessage());
                }
                if (lastMsgReverseBinaryArrayMB.length != msgReverseBinaryArrayMb.length) {
                    notifyChildHandlers(mbMsg);
                } else {
                    String lastValue = lastMsgReverseBinaryArrayMB[i].substring(a - 1, a);
                    if (!lastValue.equals(value) || refresh) {
                        notifyChildHandlers(mbMsg);
                    }
                }
                strBuilder.setLength(0);
                addrMb++;
            }
        }
        lastMsgReverseBinaryArrayMB = msgReverseBinaryArrayMb;

        MBDMessage mbdMsg;
        String msgMbd = msg.substring(84, 116);

        String msgReverseBinaryArrayMbd[] = hexStringToReverseBinaryArray(msgMbd);
        int addrMbd = 1;
        for (int i = 0; i < msgReverseBinaryArrayMbd.length; i++) {
            String d = msgReverseBinaryArrayMbd[i];
            for (int a = 1; a <= 8; a++) {
                String value = d.substring(a - 1, a);
                strBuilder.append(addrMbd);
                strBuilder.append(",");
                strBuilder.append(value);
                try {
                    mbdMsg = new MBDMessage(strBuilder.toString());
                } catch (IllegalArgumentException e) {
                    throw new MessageParseException(e.getMessage());
                }
                if (lastMsgReverseBinaryArrayMBD.length != msgReverseBinaryArrayMbd.length) {
                    notifyChildHandlers(mbdMsg);
                } else {
                    String lastValue = lastMsgReverseBinaryArrayMBD[i].substring(a - 1, a);
                    if (!lastValue.equals(value) || refresh) {
                        notifyChildHandlers(mbdMsg);
                    }
                }
                strBuilder.setLength(0);
                addrMbd++;
            }
        }
        lastMsgReverseBinaryArrayMBD = msgReverseBinaryArrayMbd;
    }

    /**
     * Parse and handle SB State messages. The SB state messages have
     * identical format.
     *
     * @param mt message type of incoming message
     * @param msg string containing incoming message payload
     * @throws MessageParseException
     */
    private void parseSbStateMessage(TelenotMsgType mt, String msg) throws MessageParseException {
        SBStateMessage sbStateMessage;
        StringBuilder sb = new StringBuilder();
        sb.append(mt);
        sb.append(":");
        sb.append(msg);

        try {
            sbStateMessage = new SBStateMessage(sb.toString());
        } catch (IllegalArgumentException e) {
            throw new MessageParseException(e.getMessage());
        }
        notifyChildHandlers(sbStateMessage);
    }

    /**
     * Parse and handle used input contacts messages. The used input contact messages have
     * identical format.
     *
     * @param mt message type of incoming message
     * @param msg string containing incoming message payload
     * @throws MessageParseException
     */
    private void parseUsedInputsMessage(TelenotMsgType mt, String msg) throws MessageParseException {
        logger.trace("MSG: {}", msg);

        String msgInputContacts = msg.substring(24, 16 + (Integer.parseInt(msg.substring(12, 14), 16) * 2));
        logger.trace("UsedContact: {}", msgInputContacts);

        String msgReverseBinaryArray[] = hexStringToReverseBinaryArray(msgInputContacts);
        int address = 0;
        String hexAddr = "";
        for (int i = 0; i < msgReverseBinaryArray.length; i++) {
            String d = msgReverseBinaryArray[i];
            for (int a = 1; a <= 8; a++) {
                if (Integer.parseInt(d.substring(a - 1, a)) == 0) {
                    hexAddr = Integer.toHexString(address);
                    hexAddr = String.format("%s" + "%0" + (4 - hexAddr.length()) + "d%s", "0x", 0, hexAddr);
                    usedInputContact.add(hexAddr);
                }
                address++;
            }
        }
    }

    /**
     * Parse and handle used output contacts messages. The used output contact messages have
     * identical format.
     *
     * @param mt message type of incoming message
     * @param msg string containing incoming message payload
     * @throws MessageParseException
     */
    private void parseUsedOutputsMessage(TelenotMsgType mt, String msg) throws MessageParseException {
        logger.trace("MSG: {}", msg);

        String msgOutputContacts = msg.substring(24, 16 + (Integer.parseInt(msg.substring(12, 14), 16) * 2));
        logger.trace("UsedContact: {}", msgOutputContacts);

        String msgReverseBinaryArray[] = hexStringToReverseBinaryArray(msgOutputContacts);
        int address = 1280;
        String hexAddr = "";
        for (int i = 0; i < msgReverseBinaryArray.length; i++) {
            String d = msgReverseBinaryArray[i];
            for (int a = 1; a <= 8; a++) {
                if (Integer.parseInt(d.substring(a - 1, a)) == 0) {
                    hexAddr = Integer.toHexString(address);
                    hexAddr = String.format("%s" + "%0" + (4 - hexAddr.length()) + "d%s", "0x", 0, hexAddr);
                    if (address >= 1280 && address <= 1327) {
                        usedOutputContact.add(hexAddr);
                    } else if (address >= 1328 && address <= 1391) {
                        double b = address - 1327;
                        double sbNumber = Math.ceil(b / 8);
                        int number = (int) sbNumber;
                        String sbNum = String.format("%s", number);
                        ArrayList<String> num = new ArrayList<String>();
                        num.add(sbNum);
                        if (!usedSecurityArea.equals(num)) {
                            usedSecurityArea.add(sbNum);
                        }
                        usedSecurityAreaContact.add(hexAddr);
                    } else if (address >= 1392 && address <= 1519) {
                        usedReportingArea.add(hexAddr);
                    }
                }
                address++;
            }
        }
        TelenotDiscoveryService ds = discoveryService;
        if (discovery && ds != null) {
            for (String i : usedSecurityArea) {
                int sbNum = Integer.parseInt(i);
                ds.processSB(sbNum);
            }
        }
    }

    /**
     * Parse and handle used inputs messages. The inputs messages have
     * identical format.
     *
     * @param mt message type of incoming message
     * @param msg string containing incoming message payload
     * @throws MessageParseException
     */
    private void parseUsedContactInfoMessage(TelenotMsgType mt, String msg) throws MessageParseException {
        logger.trace("MSG: {}", msg);
        if (mt == TelenotMsgType.USED_CONTACTS_INFO && !usedInputContact.isEmpty()) {
            UsedContactInfoMessage uciStateMessage;
            String address = usedInputContact.get(0);
            try {
                uciStateMessage = new UsedContactInfoMessage(address + ":" + msg);
            } catch (IllegalArgumentException e) {
                throw new MessageParseException(e.getMessage());
            }
            notifyChildHandlersChannel(uciStateMessage);
            usedInputContact.remove(0);
        }

        if (mt == TelenotMsgType.USED_MB_CONTACTS_INFO && !usedReportingArea.isEmpty()) {
            UsedMbMessage umbStateMessage;
            String address = usedReportingArea.get(0);
            try {
                umbStateMessage = new UsedMbMessage(address + ":" + msg);
            } catch (IllegalArgumentException e) {
                throw new MessageParseException(e.getMessage());
            }
            notifyChildHandlersChannel(umbStateMessage);
            usedReportingArea.remove(0);
        }
    }

    /**
     * Parse and handle EMA State messages. The SB messages have
     * identical format.
     *
     * @param mt message type of incoming message
     * @param msg string containing incoming message payload
     * @throws MessageParseException
     */
    private void parseEmaStateMessage(TelenotMsgType mt, String msg) throws MessageParseException {
        EMAStateMessage emaMessage;
        StringBuilder sb = new StringBuilder();
        sb.append(mt);
        sb.append(":");
        sb.append(msg);

        try {
            emaMessage = new EMAStateMessage(sb.toString());
        } catch (IllegalArgumentException e) {
            throw new MessageParseException(e.getMessage());
        }
        notifyChildHandlers(emaMessage);
    }

    /**
     * Notify appropriate child thing handlers of an Telenot message by calling their handleUpdate() methods.
     *
     * @param msg message to forward to child handler(s)
     */
    private void notifyChildHandlers(TelenotMessage msg) {
        for (Thing thing : getThing().getThings()) {
            TelenotThingHandler handler = (TelenotThingHandler) thing.getHandler();
            //@formatter:off
            if (handler != null && ((handler instanceof SBHandler && msg instanceof SBMessage) ||
                                    (handler instanceof SBHandler && msg instanceof SBStateMessage) ||
                                    (handler instanceof MBHandler && msg instanceof MBMessage) ||
                                    (handler instanceof MBHandler && msg instanceof MBDMessage) ||
                                    (handler instanceof MPHandler && msg instanceof MPMessage) ||
                                    (handler instanceof InputHandler && msg instanceof InputMessage) ||
                                    (handler instanceof OutputHandler && msg instanceof MBMessage) ||
                                    (handler instanceof OutputHandler && msg instanceof MBDMessage) ||
                                    (handler instanceof EMAStateHandler && msg instanceof EMAStateMessage))) {
                handler.handleUpdate(msg);
            }
            //@formatter:on
        }
    }

    /**
     * Notify appropriate child thing handlers of an Telenot message by calling their handleUpdateChannel() methods.
     *
     * @param msg message to forward to child handler(s)
     */
    private void notifyChildHandlersChannel(TelenotMessage msg) {
        for (Thing thing : getThing().getThings()) {
            TelenotThingHandler handler = (TelenotThingHandler) thing.getHandler();
            //@formatter:off
            if (handler != null && ((handler instanceof InputHandler && msg instanceof UsedContactInfoMessage) ||
                                    (handler instanceof OutputHandler && msg instanceof UsedMbMessage))) {
                handler.handleUpdateChannel(msg);
            }
            //@formatter:on
        }
    }

    /**
     * Converts bytes into a hex string
     */
    public static String toHexString(byte @Nullable [] bytes) {
        StringBuilder sb = new StringBuilder();
        if (bytes != null) {
            for (byte b : bytes) {
                final String hexString = Integer.toHexString(b & 0xff);

                if (hexString.length() == 1) {
                    sb.append('0');
                }
                sb.append(hexString);
            }
        }
        return sb.toString();
    }

    /**
     * Converts hex string to binary array
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length() - 2;
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Converts hex string into an array
     */
    public static String[] hexStringToArray(String s) {
        int len = s.length();
        String[] data = new String[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = s.substring(i, i + 2);
        }
        return data;
    }

    /**
     * Converts a hex string into a reversed binary array
     */
    public static String[] hexStringToReverseBinaryArray(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        int dlen;
        String[] data = new String[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = hexToBinary(s.substring(i, i + 2));
            dlen = data[i / 2].length();
            if (dlen <= 7) {
                for (int a = dlen; a < 8; a++) {
                    sb.append('0');
                }
                sb.append(data[i / 2]);
            } else {
                sb.append(data[i / 2]);
            }
            data[i / 2] = sb.reverse().toString();
            sb.setLength(0);
        }
        return data;
    }

    /**
     * Converts hex into binary
     */
    public static String hexToBinary(String hex) {
        int i = Integer.parseInt(hex, 16);
        String bin = Integer.toBinaryString(i);
        return bin;
    }

    /**
     * Exception thrown by message parsing code when it encounters a malformed message
     */
    private static class MessageParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public MessageParseException(@Nullable String msg) {
            super(msg);
        }
    }
}
