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
package org.smarthomej.binding.tcpudp.internal;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.commons.SimpleDynamicStateDescriptionProvider;
import org.smarthomej.commons.itemvalueconverter.ContentWrapper;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link TcpThingHandler} is a thing handler implementation for UDP connections
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TcpThingHandler extends TcpUdpBaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(TcpThingHandler.class);

    public TcpThingHandler(Thing thing, ValueTransformationProvider valueTransformationProvider,
            SimpleDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        super(thing, valueTransformationProvider, dynamicStateDescriptionProvider);
    }

    @Override
    protected void doAsyncSend(String command) {
        scheduler.execute(() -> {
            try (Socket socket = new Socket(config.host, config.port);
                    PrintWriter out = new PrintWriter(socket.getOutputStream())) {
                socket.setSoTimeout(config.timeout);
                out.println(command);

                updateStatus(ThingStatus.ONLINE);
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                logger.debug("Failed sending '{}' in thing '{}': {}", command, thing.getUID(), e.getMessage());
            }
        });
    }

    @Override
    protected Optional<ContentWrapper> doSyncRequest(String request) {
        try (Socket socket = new Socket(config.host, config.port);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                ByteArrayOutputStream outputByteArrayStream = new ByteArrayOutputStream()) {
            socket.setSoTimeout(config.timeout);
            out.write(request.getBytes(Objects.requireNonNullElse(config.encoding, StandardCharsets.UTF_8.name())));
            out.flush();
            byte[] buffer = new byte[config.bufferSize];
            int len;

            do {
                len = in.read(buffer);
                if (len != -1) {
                    outputByteArrayStream.write(buffer, 0, len);
                }
            } while (len == config.bufferSize);
            outputByteArrayStream.flush();

            ContentWrapper contentWrapper = new ContentWrapper(outputByteArrayStream.toByteArray(),
                    Objects.requireNonNullElse(config.encoding, StandardCharsets.UTF_8.name()), null);

            updateStatus(ThingStatus.ONLINE);
            return Optional.of(contentWrapper);
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            logger.debug("Failed to request '{}' in thing '{}': {}", request, thing.getUID(), e.getMessage());
        }

        return Optional.empty();
    }
}
