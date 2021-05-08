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
package org.smarthomej.binding.tcpudp.internal.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tcpudp.internal.config.ClientConfiguration;

/**
 * The {@link EchoServer} is an echo server for UDP or TCP connections
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class EchoServer {
    private final Logger logger = LoggerFactory
            .getLogger(org.smarthomej.binding.tcpudp.internal.receiver.TcpReceiver.class);

    private @Nullable ServerSocket tcpSocket;
    private @Nullable DatagramSocket udpSocket;

    private final SocketAddress socketAddress;
    private byte[] buf = new byte[2048];

    private final List<String> receivedValues = new ArrayList<>();
    private Thread thread;

    private boolean serverReady;

    public EchoServer(int port, ClientConfiguration.Protocol protocol) {
        this.socketAddress = new InetSocketAddress("0.0.0.0", port);
        serverReady = false;
        if (protocol == ClientConfiguration.Protocol.TCP) {
            thread = new Thread(this::runTcp);
        } else {
            thread = new Thread(this::runUdp);
        }
        thread.start();
    }

    public boolean getServerReady() {
        return serverReady;
    }

    public List<String> getReceivedValues() {
        return receivedValues;
    }

    public void runUdp() {
        try (DatagramSocket socket = new DatagramSocket(null)) {
            this.udpSocket = socket;
            socket.setReuseAddress(true);
            socket.bind(socketAddress);
            serverReady = true;
            while (1 == 1) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                receivedValues.add(new String(data));
                socket.send(packet);
            }
        } catch (IOException e) {
        }
    }

    public void runTcp() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            this.tcpSocket = serverSocket;
            serverSocket.setReuseAddress(true);
            serverSocket.bind(socketAddress);
            serverReady = true;
            while (1 == 1) {
                try (Socket clientSocket = serverSocket.accept();
                        InputStream in = clientSocket.getInputStream();
                        OutputStream out = clientSocket.getOutputStream()) {
                    int byteCount = in.read(buf);
                    if (byteCount == -1) {
                        logger.warn("Did not receive data");
                    } else {
                        byte[] data = Arrays.copyOfRange(buf, 0, byteCount);
                        receivedValues.add(new String(data));
                        out.write(data);
                        out.flush();
                    }
                }
            }
        } catch (IOException e) {
        }
    }

    public void stop() {
        ServerSocket tcpSocket = this.tcpSocket;
        if (tcpSocket != null) {
            try {
                tcpSocket.close();
            } catch (IOException e) {
                logger.warn("Could not close connection: {}", e.getMessage());
            }
        }
        DatagramSocket udpSocket = this.udpSocket;
        if (udpSocket != null) {
            udpSocket.close();
        }
    }
}
