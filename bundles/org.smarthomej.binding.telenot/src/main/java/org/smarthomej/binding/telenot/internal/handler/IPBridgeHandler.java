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

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.config.IPBridgeConfig;
import org.smarthomej.binding.telenot.internal.protocol.TelenotCommand;

/**
 * Handler responsible for communicating via TCP with the Telenot IP Serial device.
 *
 * @author Ronny Grun - Initial contribution
 * 
 */
@NonNullByDefault
public class IPBridgeHandler extends TelenotBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(IPBridgeHandler.class);

    private IPBridgeConfig config = new IPBridgeConfig();

    private @Nullable Socket socket = null;

    private @Nullable ScheduledFuture<?> connectJob = null;

    public IPBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing IP bridge handler");
        config = getConfigAs(IPBridgeConfig.class);
        discovery = config.discovery;

        if (config.hostname == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "hostname not configured");
            return;
        }
        if (config.tcpPort <= 0 || config.tcpPort > 65535) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "invalid port number configured");
            return;
        }

        // set the thing status to UNKNOWN temporarily and let the background connect task decide the real status.
        updateStatus(ThingStatus.UNKNOWN);

        scheduler.submit(this::connect); // start the async connect task
    }

    @Override
    protected synchronized void connect() {
        disconnect();
        ScheduledFuture<?> connectJob = this.connectJob;
        if (connectJob == null || connectJob.isDone()) {
            connectJob = scheduler.schedule(this::internalConnect, 2, TimeUnit.SECONDS);
        }
    }

    protected synchronized void internalConnect() {
        writeException = false;
        try {
            Socket socket = new Socket(config.hostname, config.tcpPort);
            this.socket = socket;
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();

            logger.debug("connected to {}:{}", config.hostname, config.tcpPort);

            startMsgReader();
            updateStatus(ThingStatus.ONLINE);

            // Start connection check job
            logger.debug("Scheduling connection check job with interval {} minutes.", config.reconnect);
            lastReceivedTime = new Date();
            connectionCheckJob = scheduler.scheduleWithFixedDelay(this::connectionCheck, config.reconnect,
                    config.reconnect, TimeUnit.MINUTES);
            refreshSendDataJob = scheduler.scheduleWithFixedDelay(this::refreshSendData, config.refreshData,
                    config.refreshData, TimeUnit.MINUTES);
            if (config.updateClock > 0) {
                updateTelenotClockJob = scheduler.scheduleWithFixedDelay(this::updateClock, 0, config.updateClock,
                        TimeUnit.HOURS);
            }

        } catch (ConnectException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            disconnect();
            scheduleConnectRetry(config.reconnect); // Possibly a retryable error. Try again later.
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            disconnect();
            scheduleConnectRetry(config.reconnect); // Possibly a retryable error. Try again later.
        }
    }

    protected synchronized void connectionCheck() {
        logger.trace("Connection check job running");

        Thread mrThread = msgReaderThread;
        if (mrThread != null && !mrThread.isAlive()) {
            logger.debug("Reader thread has exited abnormally. Restarting.");
            scheduler.submit(this::connect);
        } else if (writeException) {
            logger.debug("Write exception encountered. Resetting connection.");
            scheduler.submit(this::connect);
        } else {
            Date now = new Date();
            Date last = lastReceivedTime;
            if (last != null && config.timeout > 0
                    && ((last.getTime() + (config.timeout * 60 * 1000)) < now.getTime())) {
                logger.warn("Last valid message received at {}. Resetting connection.", last);
                scheduler.submit(this::connect);
            }
        }
    }

    protected synchronized void refreshSendData() {
        logger.debug("Start refreshing data to eventbus");
        refresh = true;
    }

    protected synchronized void updateClock() {
        boolean wait = true;
        long timeOut = System.currentTimeMillis() + 20 * 1000;
        while (!TelenotThingHandler.readyToSendData.get()) {
            if (wait) {
                logger.debug("waiting for ready to send data");
                wait = false;
            }
            if (System.currentTimeMillis() > timeOut) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "The bridge connection timed out");
                return;
            }
        }
        logger.debug("Start updating Telenot system clock");
        sendTelenotCommand(TelenotCommand.setDateTime());
    }

    @Override
    protected synchronized void disconnect() {
        logger.trace("Disconnecting");
        // stop scheduled connection check and retry jobs
        ScheduledFuture<?> crJob = connectRetryJob;
        if (crJob != null) {
            // use cancel(false) so we don't kill ourselves when connect retry job calls disconnect()
            crJob.cancel(false);
            connectRetryJob = null;
        }
        ScheduledFuture<?> ccJob = connectionCheckJob;
        if (ccJob != null) {
            // use cancel(false) so we don't kill ourselves when reconnect job calls disconnect()
            ccJob.cancel(false);
            connectionCheckJob = null;
        }

        ScheduledFuture<?> rfJob = refreshSendDataJob;
        if (rfJob != null) {
            // use cancel(false) so we don't kill ourselves when reconnect job calls disconnect()
            rfJob.cancel(false);
            refreshSendDataJob = null;
        }

        ScheduledFuture<?> ucJob = updateTelenotClockJob;
        if (ucJob != null) {
            // use cancel(false) so we don't kill ourselves when reconnect job calls disconnect()
            ucJob.cancel(false);
            updateTelenotClockJob = null;
        }

        ScheduledFuture<?> connectJob = this.connectJob;
        if (connectJob != null) {
            // use cancel(false) so we don't kill ourselves when reconnect job calls disconnect()
            connectJob.cancel(false);
            connectJob = null;
        }

        // Must close the socket first so the message reader thread will exit properly.
        Socket s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                logger.debug("error closing socket: {}", e.getMessage());
            }
        }
        socket = null;

        stopMsgReader();

        outputStream = null;
        inputStream = null;
    }
}
