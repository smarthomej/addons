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
package org.smarthomej.binding.amazonechocontrol.internal.websocket;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.amazonechocontrol.internal.connection.Connection;
import org.smarthomej.binding.amazonechocontrol.internal.jsons.JsonRegisterAppResponse.MacDms;

import com.google.gson.Gson;

/**
 * The {@link WebSocketConnection} encapsulate the Web Socket connection to the amazon server.
 * The code is based on
 * https://github.com/Apollon77/alexa-remote/blob/master/alexa-wsmqtt.js
 *
 * @author Michael Geramb - Initial contribution
 * @author Ingo Fischer - (https://github.com/Apollon77/alexa-remote/blob/master/alexa-wsmqtt.js)
 */
@NonNullByDefault
public class WebSocketConnection {
    private final Logger logger = LoggerFactory.getLogger(WebSocketConnection.class);

    private final WebSocketClient webSocketClient;
    private final AlexaWebSocket alexaWebSocket;
    private final String adpToken;
    private final PrivateKey privateKey;

    private @Nullable Timer pingTimer;
    private @Nullable Timer pongTimeoutTimer;
    private @Nullable Future<?> sessionFuture;

    private boolean closed = false;

    public WebSocketConnection(Connection connection, WebSocketCommandHandler webSocketCommandHandler, Gson gson,
            HttpClient httpClient) throws WebsocketException {
        String amazonSite = connection.getAmazonSite();
        List<HttpCookie> sessionCookies = connection.getSessionCookies(connection.getAlexaServer());
        MacDms macDms = connection.getMacDms();
        if (macDms == null) {
            throw new WebsocketException("Web socket failed: Could not get macDMS.");
        }

        this.adpToken = Objects.requireNonNullElse(macDms.adpToken, "");

        try {
            byte[] encoded = Base64.getMimeDecoder().decode(macDms.devicePrivateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            this.privateKey = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new WebsocketException("Could not determine private key");
        }

        alexaWebSocket = new AlexaWebSocket(this, webSocketCommandHandler, gson);
        webSocketClient = new WebSocketClient(httpClient);

        try {
            String host;
            if ("amazon.com".equalsIgnoreCase(amazonSite)) {
                host = "dp-gw-na-js." + amazonSite;
            } else {
                host = "dp-gw-na." + amazonSite;
            }

            List<HttpCookie> cookiesForWs = new ArrayList<>();
            for (HttpCookie cookie : sessionCookies) {
                // Clone the cookie without the security attribute, because the web socket implementation ignore secure
                // cookies
                String value = cookie.getValue().replaceAll("^\"|\"$", "");
                HttpCookie cookieForWs = new HttpCookie(cookie.getName(), value);
                cookiesForWs.add(cookieForWs);
            }
            URI uri;

            uri = new URI("wss://" + host + "/tcomm/");

            try {
                webSocketClient.start();
            } catch (Exception e) {
                logger.warn("Web socket start failed", e);
                throw new WebsocketException("Web socket start failed.");
            }

            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("Host", host);
            request.setHeader("Origin", "https://alexa." + amazonSite);
            request.setHeader("x-dp-comm-tuning", "A:F;A:H");
            request.setHeader("x-dp-reason", "ClientInitiated;1");
            request.setHeader("x-dp-tcomm-purpose", "Regular");
            // 'x-dp-deviceVersion': 'motorola/osprey_reteu_2gb/osprey_u2:6.0.1/MPI24.107-55/33:user/release-keys',
            // 'x-dp-networkType': 'WIFI',
            // 'x-dp-tcomm-versionCode': '894920010',
            // 'x-dp-oui': 'dca632',
            request.setHeader("x-dp-obfuscatedBssid", "-2019514039");
            request.setHeader("x-dp-tcomm-versionName", "2.2.443692.0");
            request.setHeader("x-adp-signature", sign("GET", "/tcomm/", ""));
            request.setHeader("x-adp-token", adpToken);
            request.setHeader("x-adp-alg", "SHA256WithRSA:1.0");
            request.setCookies(cookiesForWs);

            initPongTimeoutTimer();

            sessionFuture = webSocketClient.connect(alexaWebSocket, uri, request);
        } catch (URISyntaxException | IOException e) {
            throw new WebsocketException("Failed to initialize websocket.", e);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        closed = true;
        final Future<?> sessionFuture = this.sessionFuture;
        if (sessionFuture != null) {
            sessionFuture.cancel(true);
        }

        Timer pingTimer = this.pingTimer;
        if (pingTimer != null) {
            pingTimer.cancel();
        }
        clearPongTimeoutTimer();
        logger.trace("Connect future = {}", sessionFuture);
        try {
            webSocketClient.stop();
        } catch (InterruptedException e) {
            // Just ignore
        } catch (Exception e) {
            logger.warn("Stopping websocket failed", e);
        }

        webSocketClient.destroy();
    }

    void onConnect() {
        Timer pingTimer = new Timer();
        this.pingTimer = pingTimer;
        pingTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                alexaWebSocket.sendPing();
            }
        }, 180000, 180000);
    }

    void clearPongTimeoutTimer() {
        Timer pongTimeoutTimer = this.pongTimeoutTimer;
        this.pongTimeoutTimer = null;
        if (pongTimeoutTimer != null) {
            logger.trace("Cancelling pong timeout");
            pongTimeoutTimer.cancel();
        }
    }

    void initPongTimeoutTimer() {
        clearPongTimeoutTimer();
        Timer pongTimeoutTimer = new Timer();
        this.pongTimeoutTimer = pongTimeoutTimer;
        logger.trace("Scheduling pong timeout");
        pongTimeoutTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                logger.trace("Pong timeout reached. Closing connection.");
                close();
            }
        }, 60000);
    }

    private @Nullable String sign(String method, String path, String body) {
        try {
            Signature privateSignature = Signature.getInstance("SHA256withRSA");
            String now = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
                    .format(DateTimeFormatter.ISO_INSTANT);

            privateSignature.initSign(privateKey);
            privateSignature.update((method + "\n").getBytes(StandardCharsets.UTF_8));
            privateSignature.update((path + "\n").getBytes(StandardCharsets.UTF_8));
            privateSignature.update((now + "\n").getBytes(StandardCharsets.UTF_8));
            privateSignature.update((body + "\n").getBytes(StandardCharsets.UTF_8));
            privateSignature.update((adpToken).getBytes(StandardCharsets.UTF_8));

            byte[] signature = privateSignature.sign();
            return Base64.getEncoder().encodeToString(signature) + ":" + now;
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            return null;
        }
    }
}
