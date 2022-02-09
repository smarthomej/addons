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

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.config.SerialBridgeConfig;
import org.smarthomej.binding.telenot.internal.protocol.TelenotCommand;

/**
 * Handler responsible for communicating via a serial port with the Telenot device.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class SerialBridgeHandler extends TelenotBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(SerialBridgeHandler.class);

    private SerialBridgeConfig config = new SerialBridgeConfig();
    private final SerialPortManager serialPortManager;
    private @NonNullByDefault({}) SerialPortIdentifier portIdentifier;
    private @Nullable SerialPort serialPort;

    public SerialBridgeHandler(Bridge bridge, SerialPortManager serialPortManager) {
        super(bridge);
        this.serialPortManager = serialPortManager;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing serial bridge handler");
        config = getConfigAs(SerialBridgeConfig.class);
        discovery = config.discovery;

        if (config.serialPort.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "no serial port configured");
            return;
        }

        portIdentifier = serialPortManager.getIdentifier(config.serialPort);
        if (portIdentifier == null) {
            logger.debug("Serial Error: Port {} does not exist.", config.serialPort);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Configured serial port does not exist");
            return;
        }

        connect();

        logger.trace("Finished initializing serial bridge handler");
    }

    @Override
    protected synchronized void connect() {
        disconnect(); // make sure we are disconnected
        try {
            SerialPort serialPort = portIdentifier.open("org.smarthomej.binding.telenot", 100);
            serialPort.setSerialPortParams(SERIAL_PORT_SPEED, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

            this.serialPort = serialPort;

            outputStream = serialPort.getOutputStream();
            inputStream = serialPort.getInputStream();

            logger.debug("connected to serial port: {}", config.serialPort);
            startMsgReader();

            updateStatus(ThingStatus.ONLINE);

            refreshSendDataJob = scheduler.scheduleWithFixedDelay(this::refreshSendData, config.refreshData,
                    config.refreshData, TimeUnit.MINUTES);
            if (config.updateClock > 0) {
                updateTelenotClockJob = scheduler.scheduleWithFixedDelay(this::updateClock, 0, config.updateClock,
                        TimeUnit.HOURS);
            }
        } catch (PortInUseException e) {
            logger.debug("Cannot open serial port: {}, it is already in use", config.serialPort);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Serial port already in use");
        } catch (UnsupportedCommOperationException | IOException | IllegalStateException e) {
            logger.debug("Error connecting to serial port: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    protected synchronized void refreshSendData() {
        logger.debug("Start refreshing data to eventbus");
        refresh = true;
    }

    protected synchronized void updateClock() {
        boolean wait = true;
        while (!TelenotThingHandler.readyToSendData.get()) {
            if (wait) {
                logger.debug("waiting for ready to send data");
                wait = false;
            }
        }
        logger.debug("Start updating Telenot system clock");
        sendTelenotCommand(TelenotCommand.setDateTime());
    }

    @Override
    protected synchronized void disconnect() {
        logger.trace("Disconnecting");

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

        SerialPort sp = serialPort;
        if (sp != null) {
            logger.trace("Closing serial port");
            sp.close();
            serialPort = null;
        }

        stopMsgReader();

        outputStream = null;
        inputStream = null;
    }
}
