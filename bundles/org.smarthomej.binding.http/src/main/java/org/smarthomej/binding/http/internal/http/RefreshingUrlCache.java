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
package org.smarthomej.binding.http.internal.http;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.http.internal.Util;
import org.smarthomej.binding.http.internal.config.HttpThingConfig;
import org.smarthomej.commons.itemvalueconverter.ContentWrapper;

/**
 * The {@link RefreshingUrlCache} is responsible for requesting from a single URL and passing the content to the
 * channels
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class RefreshingUrlCache {
    private final Logger logger = LoggerFactory.getLogger(RefreshingUrlCache.class);

    private final String url;
    private final RateLimitedHttpClient httpClient;
    private final boolean strictErrorHandling;
    private final int timeout;
    private final int bufferSize;
    private final @Nullable String fallbackEncoding;
    private final Set<Consumer<@Nullable ContentWrapper>> consumers = ConcurrentHashMap.newKeySet();
    private final Map<String, String> headers;
    private final HttpMethod httpMethod;
    private final String httpContent;
    private final HttpStatusListener httpStatusListener;

    private final ScheduledFuture<?> future;
    private @Nullable ContentWrapper lastContent;

    public RefreshingUrlCache(ScheduledExecutorService executor, RateLimitedHttpClient httpClient, String url,
            HttpThingConfig thingConfig, String httpContent, HttpStatusListener httpStatusListener) {
        this.httpClient = httpClient;
        this.url = url;
        this.strictErrorHandling = thingConfig.strictErrorHandling;
        this.timeout = thingConfig.timeout;
        this.bufferSize = thingConfig.bufferSize;
        this.httpMethod = thingConfig.stateMethod;
        this.headers = thingConfig.getHeaders();
        this.httpContent = httpContent;
        this.httpStatusListener = httpStatusListener;
        fallbackEncoding = thingConfig.encoding;

        future = executor.scheduleWithFixedDelay(this::refresh, 1, thingConfig.refresh, TimeUnit.SECONDS);
        logger.trace("Started refresh task for URL '{}' with interval {}s", url, thingConfig.refresh);
    }

    private void refresh() {
        refresh(false);
    }

    private void refresh(boolean isRetry) {
        if (consumers.isEmpty()) {
            // do not refresh if we don't have listeners
            return;
        }

        // format URL
        try {
            URI uri = Util.uriFromString(String.format(this.url, new Date()));
            logger.trace("Requesting refresh (retry={}) from '{}' with timeout {}ms", isRetry, uri, timeout);

            httpClient.newRequest(uri, httpMethod, httpContent, null).thenAccept(request -> {
                request.timeout(timeout, TimeUnit.MILLISECONDS);
                headers.forEach(request::header);

                CompletableFuture<@Nullable ContentWrapper> responseContentFuture = new CompletableFuture<>();
                responseContentFuture.exceptionally(t -> {
                    if (t instanceof HttpAuthException) {
                        if (isRetry || !httpClient.reAuth(uri)) {
                            logger.debug("Authentication failure failed for '{}', retry=", uri, isRetry);
                            httpStatusListener.onHttpError("Authorization failed");
                        } else {
                            refresh(true);
                        }
                    }
                    return null;
                }).thenAccept(this::processResult);

                if (logger.isTraceEnabled()) {
                    logger.trace("Sending to '{}': {}", uri, Util.requestToLogString(request));
                }

                request.send(new HttpResponseListener(responseContentFuture, fallbackEncoding, bufferSize,
                        httpStatusListener));
            }).exceptionally(e -> {
                if (e instanceof CancellationException) {
                    logger.debug("Request to URL {} was cancelled by thing handler.", uri);
                } else {
                    logger.warn("Request to URL {} failed: {}", uri, e.getMessage());
                }
                return null;
            });
        } catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
            logger.warn("Creating request for '{}' failed: {}", url, e.getMessage());
        }
    }

    public void stop() {
        // clearing all listeners to prevent further updates
        consumers.clear();
        future.cancel(false);
        logger.trace("Stopped refresh task for URL '{}'", url);
    }

    public void addConsumer(Consumer<@Nullable ContentWrapper> consumer) {
        consumers.add(consumer);
    }

    public Optional<ContentWrapper> get() {
        return Optional.ofNullable(lastContent);
    }

    private void processResult(@Nullable ContentWrapper content) {
        if (content != null || strictErrorHandling) {
            for (Consumer<@Nullable ContentWrapper> consumer : consumers) {
                try {
                    consumer.accept(content);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    logger.warn("Failed processing result for URL {}: {}", url, e.getMessage());
                }
            }
        }
        lastContent = content;
    }
}
