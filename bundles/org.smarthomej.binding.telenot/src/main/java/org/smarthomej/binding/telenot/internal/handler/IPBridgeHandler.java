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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
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
        disconnect(); // make sure we are disconnected
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
        } catch (UnknownHostException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "unknown host");
            logger.debug("UnknownHostException");
            disconnect();
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

        try {
            OutputStream os = outputStream;
            if (os != null) {
                os.close();
            }
            InputStream is = inputStream;
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            logger.debug("error closing reader/writer: {}", e.getMessage());
        }
        outputStream = null;
        inputStream = null;
    }
}
