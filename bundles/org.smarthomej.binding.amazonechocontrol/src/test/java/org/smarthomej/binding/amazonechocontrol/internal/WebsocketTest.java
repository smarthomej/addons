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
package org.smarthomej.binding.amazonechocontrol.internal;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.test.java.JavaTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link WebsocketTest} tests the websocket connection - local use only
 *
 * @author Jan N. Klug - Initial contribution
 *
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@NonNullByDefault
public class WebsocketTest extends JavaTest {
    private final Logger logger = LoggerFactory.getLogger(WebsocketTest.class);

    @Mock
    private @NonNullByDefault({}) IWebSocketCommandHandler commandHandler;

    private final Gson gson = new Gson();

    private final HttpClient httpClient = new HttpClient(new SslContextFactory.Client(true));

    @BeforeAll
    public void init() throws Exception {
        httpClient.start();
    }

    @AfterAll
    public void close() throws Exception {
        httpClient.stop();
    }

    @Disabled
    @Test
    public void connectionFailTest() {
        for (int i = 0; i < 10000; i++) {
            try {
                WebSocketConnection webSocketConnection = new WebSocketConnection("invalid-tld", List.of(),
                        commandHandler, gson, httpClient);

                while (!webSocketConnection.isClosed()) {
                }
            } catch (IOException e) {
                fail("Websocket failed." + e.getMessage());
            }
        }
    }
}
