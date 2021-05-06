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
package org.smarthomej.binding.notificationsforfiretv.internal;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Tom Blum - Initial contribution
 */
@NonNullByDefault
public class NotificationsForFireTVConnection {

    private static final String PROTOCOL = "http";
    private static final String LINE = "\r\n";
    private static final String QUOTE = "\"";

    private URI uri;
    private String boundary;
    private HttpClient httpClient;
    private List<byte[]> byteArrays = new ArrayList<>();

    /**
     * This constructor initializes a new HTTP POST request with content
     * type is set to multipart/form-data
     *
     * @param ip
     * @param port
     * @throws IOException
     */
    public NotificationsForFireTVConnection(String ip, int port) throws IOException {
        uri = URI.create(PROTOCOL + "://" + ip + ":" + port);
        boundary = UUID.randomUUID().toString();
        httpClient = HttpClient.newBuilder().build();
    }

    /**
     * Adds a form field to the request
     *
     * @param name
     * @param value
     */
    public void addFormField(String name, String value) {
        byteArrays.add((QUOTE + name + QUOTE + LINE + LINE + value + LINE).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Adds a upload file section to the request
     *
     * @param name
     * @param file
     * @throws IOException
     */
    public void addFilePart(String name, File file) throws IOException {
        byte[] separator = ("--" + boundary + LINE + "Content-Disposition: form-data; name=")
                .getBytes(StandardCharsets.UTF_8);
        byteArrays.add(separator);
        byteArrays.add((QUOTE + name + QUOTE + "; filename=" + QUOTE + file.toPath().getFileName() + QUOTE + LINE
                + "Content-Type: application/octet-stream" + LINE + LINE).getBytes(StandardCharsets.UTF_8));
        byteArrays.add(Files.readAllBytes(file.toPath()));
        byteArrays.add(LINE.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return String as response in case the server returned status OK,
     *         otherwise an exception is thrown.
     * @throws IOException
     * @throws InterruptedException
     */
    public String finish() throws IOException, InterruptedException {
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .header("Content-Type", "multipart/form-data;boundary=" + boundary)
                .POST(BodyPublishers.ofByteArrays(byteArrays)).uri(uri).build();
        HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString());
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            return response.body();
        } else {
            throw new IOException("Server returned non-OK status: " + response.statusCode());
        }
    }
}
