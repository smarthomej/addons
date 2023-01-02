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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.TelenotDiscoveryService;
import org.smarthomej.binding.telenot.internal.TelenotMessageException;
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
    private final Object lock = new Object();
    protected @Nullable TelenotDiscoveryService discoveryService;
    protected boolean discovery;
    protected boolean discoveryStarted = false;
    protected boolean refresh;
    protected volatile @Nullable Date lastReceivedTime;
    protected volatile boolean writeException;

    protected volatile List<String> usedInputContact = new ArrayList<>();
    protected volatile List<String> usedOutputContact = new ArrayList<>();
    protected volatile List<String> usedSecurityArea = new ArrayList<>();
    protected volatile List<String> usedSecurityAreaContact = new ArrayList<>();
    protected volatile List<String> usedReportingArea = new ArrayList<>();

    protected volatile BitSet lastMsgReverseBinaryArrayMP = new BitSet(8);
    protected volatile BitSet lastMsgReverseBinaryArraySB = new BitSet(64);
    protected volatile BitSet lastMsgReverseBinaryArrayMB = new BitSet(8);
    protected volatile BitSet lastMsgReverseBinaryArrayMBD = new BitSet(8);

    protected @Nullable ScheduledFuture<?> connectionCheckJob;
    protected @Nullable ScheduledFuture<?> refreshSendDataJob;
    protected @Nullable ScheduledFuture<?> connectRetryJob;
    protected @Nullable ScheduledFuture<?> updateTelenotClockJob;

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
        return Set.of(BridgeActions.class, TelenotDiscoveryService.class);
    }

    /**
     * get the used security areas (needed for discovery)
     *
     * @return a list of the used security arreas
     */
    public List<String> getUsedSecurityArea() {
        // return a copy of the list, so we don't run into concurrency problems
        return new ArrayList<>(usedSecurityArea);
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
                bw.write(HexUtils.hexToBytes(command.toString()));
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
        synchronized (lock) {
            Thread mrt = new Thread(this::readerThread, "SHJ-binding-" + getThing().getUID() + "-TelenotReader");
            mrt.setDaemon(true);
            mrt.start();
            msgReaderThread = mrt;
        }
    }

    protected void stopMsgReader() {
        synchronized (lock) {
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
        String message = "";
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
            int count = 0;
            InputStream is = this.inputStream;
            while (!Thread.interrupted() && is != null && (bytesRead = is.read(content)) != -1) {
                baos.reset();
                baos.write(content, 0, bytesRead);

                message += HexUtils.bytesToHex(baos.toByteArray());
                if (message.matches("^68\\w\\w\\w\\w68(.*)")) {
                    logger.trace("HEX String: {}", message);
                    if (message.substring(2, 4).equals(message.substring(4, 6))) {
                        Integer msgLength = Integer.parseInt(message.substring(2, 4), 16) * 2 + 12;
                        if (message.length() >= msgLength) {
                            if (TelenotCommand.isValid(message.substring(0, msgLength))) {
                                processMessage(message);
                            }
                            message = "";
                            count = 0;
                        }
                    }
                }
                if (count >= 100) {
                    message = "";
                    count = 0;
                }
                count++;
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

    private void processMessage(String message) {
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
                    logger.trace("MessageType: {} MSG: {}", msgType, message);
                    parseSbStateMessage(msgType, message);
                    sendTelenotCommand(TelenotCommand.confirmACK());
                    break;
                case INTRUSION:
                case BATTERY_MALFUNCTION:
                case POWER_OUTAGE:
                case OPTICAL_FLASHER_MALFUNCTION:
                case HORN_1_MALFUNCTION:
                case HORN_2_MALFUNCTION:
                    // case COM_FAULT:
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
                case UNKNOWN:
                    logger.warn("Received {} MsgType | hexString: {}", msgType, message);
                    sendTelenotCommand(TelenotCommand.confirmACK());
                    TelenotThingHandler.readyToSendData.set(true);
                    logger.trace("Ready to send data");
                    break;
                case INVALID:
                    logger.debug("Received {} MsgType | hexString: {}", msgType, message);
                    sendTelenotCommand(TelenotCommand.confirmACK());
                    TelenotThingHandler.readyToSendData.set(true);
                    logger.trace("Ready to send data");
                    break;
                case NOT_USED_CONTACT:
                    logger.debug("Received {} MsgType | hexString: {}", msgType, message);
                    if (!usedInputContact.isEmpty()) {
                        logger.info("Contact {} not used. Discovery will skip this contact.", usedInputContact.get(0));
                        usedInputContact.remove(0);
                    }
                    sendTelenotCommand(TelenotCommand.confirmACK());
                    TelenotThingHandler.readyToSendData.set(true);
                    logger.trace("Ready to send data");
                    break;
                case COM_FAULT:
                    logger.debug("Received {} MsgType | hexString: {}", msgType, message);
                    sendTelenotCommand(TelenotCommand.confirmACK());
                    TelenotThingHandler.readyToSendData.set(true);
                    logger.trace("Ready to send data");
                default:
                    break;
            }
        } catch (MessageParseException e) {
            logger.warn("Error {} while parsing message {}. Please report bug.", e.getMessage(), message);
        }
        if (discoveryStarted && usedInputContact.isEmpty() && usedReportingArea.isEmpty()) {
            discoveryStarted = false;
            logger.info("Discovery job completed");
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
        logger.trace("MP msg: {}", msg);

        msg = msg.substring(24, 24 + ((Integer.parseInt(msg.substring(12, 14), 16) - 4) * 2));

        BitSet msgReverseBinaryArray = hexStringToReversedByteOrderBitSet(msg);
        processMessageBitSet(msgReverseBinaryArray, lastMsgReverseBinaryArrayMP, 0,
                List.of(MPMessage::new, InputMessage::new));
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
        logger.trace("SB msg: {}", msg);

        if (msg.length() < 33) {
            throw new MessageParseException("wrong SB msg length");
        }
        String msgSb = msg.substring(36, Math.min(msg.length(), 52));
        BitSet msgReverseBinaryArraySb = hexStringToReversedByteOrderBitSet(msgSb);

        int addr = 1;
        try {
            for (int i = 0; i < msgReverseBinaryArraySb.length() / 8; i++) {
                // iterate over bytes instead of bits
                int startBit = i * 8;
                int endBit = (i + 1) * 8;
                BitSet value = msgReverseBinaryArraySb.get(startBit, endBit);
                BitSet oldValue = lastMsgReverseBinaryArraySB.get(startBit, endBit);
                if (refresh || !oldValue.equals(value)) {
                    String msgStr = addr + "," + IntStream.range(0, 8).mapToObj(j -> booleanToString(value.get(j)))
                            .collect(Collectors.joining(","));
                    notifyChildHandlers(new SBMessage(msgStr));
                }
                addr++;
            }
        } catch (IllegalArgumentException e) {
            throw new MessageParseException(e.getMessage());
        }
        lastMsgReverseBinaryArraySB = msgReverseBinaryArraySb;

        BitSet msgReverseBinaryArrayMb = hexStringToReversedByteOrderBitSet(msg.substring(52, 84));
        processMessageBitSet(msgReverseBinaryArrayMb, lastMsgReverseBinaryArrayMB, 1, List.of(MBMessage::new));
        lastMsgReverseBinaryArrayMB = msgReverseBinaryArrayMb;

        BitSet msgReverseBinaryArrayMbd = hexStringToReversedByteOrderBitSet(msg.substring(84, 116));
        processMessageBitSet(msgReverseBinaryArrayMbd, lastMsgReverseBinaryArrayMBD, 1, List.of(MBDMessage::new));
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
        logger.trace("MessageType: {} MSG: {}", mt, msg);
        SBStateMessage sbStateMessage;
        StringBuilder sb = new StringBuilder();
        sb.append(mt);
        sb.append(":");
        sb.append(msg);
        try {
            sbStateMessage = new SBStateMessage(sb.toString());
        } catch (TelenotMessageException e) {
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

        String msgInputContacts = msg.substring(24, 24 + ((Integer.parseInt(msg.substring(12, 14), 16) - 4) * 2));
        logger.trace("UsedContact: {}", msgInputContacts);

        BitSet msgReverseBinaryArray = hexStringToReversedByteOrderBitSet(msgInputContacts);
        int address = 0;
        for (int i = 0; i < msgReverseBinaryArray.length(); i++) {
            if (!msgReverseBinaryArray.get(i)) {
                String hexAddr = String.format("0x%04x", address);
                usedInputContact.add(hexAddr);
            }
            address++;
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

        String msgOutputContacts = msg.substring(24, 24 + ((Integer.parseInt(msg.substring(12, 14), 16) - 4) * 2));
        logger.trace("UsedContact: {}", msgOutputContacts);

        BitSet msgReverseBinaryArray = hexStringToReversedByteOrderBitSet(msgOutputContacts);

        int address = 1280;
        for (int i = 0; i < msgReverseBinaryArray.length(); i++) {
            if (!msgReverseBinaryArray.get(i)) {
                String hexAddr = String.format("0x%04x", address);
                if (address >= 1280 && address <= 1327) {
                    usedOutputContact.add(hexAddr);
                } else if (address >= 1328 && address <= 1391) {
                    int number = (int) Math.ceil((address - 1328) / 8);
                    String sbNum = String.valueOf(number + 1);
                    if (!usedSecurityArea.contains(sbNum)) {
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

    /**
     * Parse and handle used inputs messages. The inputs messages have
     * identical format.
     *
     * @param mt message type of incoming message
     * @param msg string containing incoming message payload
     * @throws MessageParseException
     */
    private void parseUsedContactInfoMessage(TelenotMsgType mt, String msg) throws MessageParseException {
        discoveryStarted = true;
        logger.trace("MSG: {}", msg);
        if (mt == TelenotMsgType.USED_CONTACTS_INFO && !usedInputContact.isEmpty()) {
            UsedContactInfoMessage uciStateMessage;
            String address = usedInputContact.get(0);
            try {
                uciStateMessage = new UsedContactInfoMessage(address + ":" + msg);
            } catch (TelenotMessageException e) {
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
            } catch (TelenotMessageException e) {
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
        try {
            notifyChildHandlers(new EMAStateMessage(mt + ":" + msg));
        } catch (TelenotMessageException e) {
            throw new MessageParseException(e.getMessage());
        }
    }

    /**
     * process and notify all child handlers
     *
     * @param newValues a BitSet containing the new values
     * @param oldValues a BitSet containing the old values
     * @param startAddress the start address for the messages
     * @param messageCreators a List of methods that create the message
     * @throws MessageParseException if message creation fails
     */
    private void processMessageBitSet(BitSet newValues, BitSet oldValues, int startAddress,
            List<Function<String, TelenotMessage>> messageCreators) throws MessageParseException {
        // if the size of the old and new BitSet is different, we need to send the value
        boolean needsRefresh = refresh || (newValues.size() != oldValues.size());

        try {
            for (int i = 0; i < newValues.length(); i++) {
                int address = startAddress + i;
                boolean value = newValues.get(i);
                if (needsRefresh || (newValues.get(i) != oldValues.get(i))) {
                    String message = address + "," + booleanToString(value);
                    messageCreators.forEach(m -> notifyChildHandlers(m.apply(message)));
                }
            }
        } catch (IllegalArgumentException e) {
            throw new MessageParseException(e.getMessage());
        }
    }

    /**
     * convert a boolean to a 0/1 String
     * 
     * @param b the input value
     * @return "0" if false, "1" if true
     */
    private String booleanToString(boolean b) {
        return b ? "1" : "0";
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
     * Converts a hex string into a reversed
     */
    private BitSet hexStringToReversedByteOrderBitSet(String s) {
        byte[] bytes = HexUtils.hexToBytes(s);
        int byteCount = bytes.length;
        byte[] reversedBytes = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            reversedBytes[i] = (byte) ((Integer.reverseBytes(bytes[i]) >> 24) & 0xff);
        }
        return BitSet.valueOf(reversedBytes);
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
