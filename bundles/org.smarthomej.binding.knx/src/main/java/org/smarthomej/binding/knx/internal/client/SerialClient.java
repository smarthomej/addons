/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.knx.internal.client;

import java.util.Enumeration;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.RXTXVersion;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkFT12;
import tuwien.auto.calimero.link.medium.TPSettings;

/**
 * Serial specific {@link AbstractKNXClient} implementation.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
public class SerialClient extends AbstractKNXClient {

    private final Logger logger = LoggerFactory.getLogger(SerialClient.class);

    private final String serialPort;

    public SerialClient(int autoReconnectPeriod, ThingUID thingUID, int responseTimeout, int readingPause,
            int readRetriesLimit, ScheduledExecutorService knxScheduler, String serialPort,
            StatusUpdateCallback statusUpdateCallback) {
        super(autoReconnectPeriod, thingUID, responseTimeout, readingPause, readRetriesLimit, knxScheduler,
                statusUpdateCallback);
        this.serialPort = serialPort;
    }

    @Override
    protected KNXNetworkLink establishConnection() throws KNXException, InterruptedException {
        try {
            RXTXVersion.getVersion();
            logger.debug("Establishing connection to KNX bus through FT1.2 on serial port {}.", serialPort);
            return new KNXNetworkLinkFT12(serialPort, new TPSettings());
        } catch (NoClassDefFoundError e) {
            throw new KNXException(
                    "The serial FT1.2 KNX connection requires the RXTX libraries to be available, but they could not be found!",
                    e);
        } catch (KNXException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("can not open serial port")) {
                StringBuilder sb = new StringBuilder("Available ports are:\n");
                Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
                while (portList.hasMoreElements()) {
                    CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
                    if (id != null && id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                        sb.append(id.getName());
                        sb.append("\n");
                    }
                }
                sb.deleteCharAt(sb.length() - 1);
                throw new KNXException("Serial port '" + serialPort + "' could not be opened. " + sb.toString());
            } else {
                throw e;
            }
        }
    }
}
