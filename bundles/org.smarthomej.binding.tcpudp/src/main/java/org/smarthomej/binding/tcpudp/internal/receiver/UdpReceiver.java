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
package org.smarthomej.binding.tcpudp.internal.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UdpReceiver} is a receiver for UDP connections
 *
 * @author Jan N. Klug - Initial contribution
 */
public class UdpReceiver implements Receiver {
    private final Logger logger = LoggerFactory.getLogger(UdpReceiver.class);

    private @Nullable DatagramSocket socket;
    private final SocketAddress socketAddress;
    private final ReceiverListener receiverListener;
    private byte[] buf;

    private boolean reconnect;

    public UdpReceiver(ReceiverListener receiverListener, String localAddress, int port, int bufferSize) {
        this.socketAddress = new InetSocketAddress(localAddress, port);
        this.receiverListener = receiverListener;
        this.buf = new byte[bufferSize];
        reconnect = true;
    }

    private boolean enabled() {
        return reconnect && !Thread.currentThread().isInterrupted();
    }

    @Override
    public void run() {
        while (enabled()) {
            try (DatagramSocket socket = new DatagramSocket(null)) {
                this.socket = socket;
                socket.setReuseAddress(true);
                socket.bind(socketAddress);
                receiverListener.reportConnectionState(true, null);
                while (enabled()) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String sender = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

                    receiverListener.onReceive(sender, data);
                }
            } catch (IOException e) {
                receiverListener.reportConnectionState(false, e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        reconnect = false;
        DatagramSocket socket = this.socket;
        if (socket != null) {
            socket.close();
        }
    }
}
