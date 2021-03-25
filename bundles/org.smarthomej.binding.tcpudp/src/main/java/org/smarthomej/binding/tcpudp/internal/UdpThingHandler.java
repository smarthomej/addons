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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
 * The {@link UdpThingHandler} is a thing handler implementation for UDP connections
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UdpThingHandler extends TcpUdpBaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(UdpThingHandler.class);

    public UdpThingHandler(Thing thing, ValueTransformationProvider valueTransformationProvider,
            SimpleDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        super(thing, valueTransformationProvider, dynamicStateDescriptionProvider);
    }

    @Override
    protected void doAsyncSend(String command) {
        scheduler.execute(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(config.timeout);
                InetAddress inetAddress = InetAddress.getByName(config.host);

                byte[] buffer = command.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, config.port);

                socket.send(packet);
                updateStatus(ThingStatus.ONLINE);
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                logger.debug("Failed sending '{}' in thing '{}': {}", command, thing.getUID(), e.getMessage());
            }
        });
    }

    @Override
    protected Optional<ContentWrapper> doSyncRequest(String request) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(config.timeout);
            InetAddress inetAddress = InetAddress.getByName(config.host);

            byte[] buffer = request.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, config.port);
            socket.send(packet);

            byte[] receiveBuffer = new byte[config.bufferSize];
            packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(packet);

            ContentWrapper contentWrapper = new ContentWrapper(Arrays.copyOf(packet.getData(), packet.getLength()),
                    Objects.requireNonNullElse(config.encoding, StandardCharsets.UTF_8.name()), null);

            updateStatus(ThingStatus.ONLINE);
            return Optional.of(contentWrapper);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            logger.debug("Failed to request '{}' in thing '{}': {}", request, thing.getUID(), e.getMessage());
        }

        return Optional.empty();
    }
}
