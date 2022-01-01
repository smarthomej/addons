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
package org.smarthomej.binding.tcpudp.internal.receiver;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TcpReceiver} is a receiver for TCP connections
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TcpReceiver implements Receiver {
    private final Logger logger = LoggerFactory.getLogger(TcpReceiver.class);

    private @Nullable ServerSocket socket;
    private final SocketAddress socketAddress;
    private final ReceiverListener receiverListener;
    private byte[] buf;

    private boolean reconnect;

    public TcpReceiver(ReceiverListener receiverListener, String localAddress, int port, int bufferSize) {
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
            try (ServerSocket serverSocket = new ServerSocket()) {
                this.socket = serverSocket;
                serverSocket.setReuseAddress(true);
                serverSocket.bind(socketAddress);
                receiverListener.reportConnectionState(true, null);
                while (enabled()) {
                    try (Socket clientSocket = serverSocket.accept(); InputStream in = clientSocket.getInputStream()) {
                        String sender = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();

                        int byteCount = in.read(buf);
                        if (byteCount == -1) {
                            logger.warn("Did not receive data from {}", sender);
                        } else {
                            byte[] data = Arrays.copyOfRange(buf, 0, byteCount);

                            logger.trace("Received {} bytes from {}: {}", byteCount, sender, data);
                            receiverListener.onReceive(sender, data);
                        }
                    }
                }
            } catch (IOException e) {
                receiverListener.reportConnectionState(false, e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        reconnect = false;
        ServerSocket socket = this.socket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn("Could not close connection: {}", e.getMessage());
            }
        }
    }
}
