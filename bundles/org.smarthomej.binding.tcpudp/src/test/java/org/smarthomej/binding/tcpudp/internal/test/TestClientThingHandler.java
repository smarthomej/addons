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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;
import org.smarthomej.binding.tcpudp.internal.ClientThingHandler;
import org.smarthomej.commons.SimpleDynamicStateDescriptionProvider;
import org.smarthomej.commons.itemvalueconverter.ContentWrapper;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link TestClientThingHandler} is a subclass of the ClientThingHandler for testing
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TestClientThingHandler extends ClientThingHandler {
    private final Consumer<Map.Entry<CallType, String>> reporter;

    public TestClientThingHandler(Thing thing, ValueTransformationProvider valueTransformationProvider,
            SimpleDynamicStateDescriptionProvider dynamicStateDescriptionProvider,
            Consumer<Map.Entry<CallType, String>> reporter) {
        super(thing, valueTransformationProvider, dynamicStateDescriptionProvider);
        this.reporter = reporter;
    }

    @Override
    protected void doTcpAsyncSend(String command) {
        reporter.accept(Map.entry(CallType.TCP_ASYNC, command));
    }

    @Override
    protected void doUdpAsyncSend(String command) {
        reporter.accept(Map.entry(CallType.UDP_ASYNC, command));
    }

    @Override
    protected Optional<ContentWrapper> doTcpSyncRequest(String command) {
        reporter.accept(Map.entry(CallType.TCP_SYNC, command));
        return Optional
                .of(new ContentWrapper(command.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8.name(), null));
    }

    @Override
    protected Optional<ContentWrapper> doUdpSyncRequest(String command) {
        reporter.accept(Map.entry(CallType.UDP_SYNC, command));
        return Optional
                .of(new ContentWrapper(command.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8.name(), null));
    }

    public enum CallType {
        TCP_SYNC,
        UDP_SYNC,
        TCP_ASYNC,
        UDP_ASYNC
    }
}
