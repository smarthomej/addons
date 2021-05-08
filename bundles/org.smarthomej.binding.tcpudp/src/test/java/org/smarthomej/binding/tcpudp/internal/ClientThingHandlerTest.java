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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.smarthomej.binding.tcpudp.internal.TcpUdpBindingConstants.BINDING_ID;
import static org.smarthomej.binding.tcpudp.internal.TcpUdpBindingConstants.THING_TYPE_UID_CLIENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.library.types.StringType;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.smarthomej.binding.tcpudp.internal.config.ClientConfiguration;
import org.smarthomej.binding.tcpudp.internal.config.TcpUdpChannelConfig;
import org.smarthomej.binding.tcpudp.internal.test.TestClientThingHandler;
import org.smarthomej.binding.tcpudp.internal.test.TestUtil;
import org.smarthomej.commons.SimpleDynamicStateDescriptionProvider;
import org.smarthomej.commons.itemvalueconverter.ChannelMode;
import org.smarthomej.commons.transform.NoOpValueTransformation;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link ClientThingHandlerTest} is the thing handler for client type things
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ClientThingHandlerTest extends JavaTest {
    private static final String TEST_STATE_CONTENT = "testStateContent";
    private static final ThingUID TEST_THING_UID = new ThingUID(THING_TYPE_UID_CLIENT, "testThing");
    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "string");
    private static final ChannelUID TEST_CHANNEL_UID = new ChannelUID(TEST_THING_UID, "testChannel");

    private final List<Map.Entry<TestClientThingHandler.CallType, String>> reports = new ArrayList<>();

    @Mock
    private @NonNullByDefault({}) ThingHandlerCallback thingHandlerCallback;

    @Mock
    private @NonNullByDefault({}) ValueTransformationProvider valueTransformationProvider;

    @Mock
    private @NonNullByDefault({}) SimpleDynamicStateDescriptionProvider simpleDynamicStateDescriptionProvider;

    @BeforeEach
    public void startUp() {
        Mockito.doNothing().when(thingHandlerCallback).stateUpdated(any(), any());
        Mockito.doNothing().when(thingHandlerCallback).statusUpdated(any(), any());
        Mockito.doReturn(NoOpValueTransformation.getInstance()).when(valueTransformationProvider)
                .getValueTransformation(any());
    }

    @AfterEach
    public void cleanUp() {
        reports.clear();
    }

    @Test
    public void tcpRequestTest() {
        requestTest(ClientConfiguration.Protocol.TCP, TestClientThingHandler.CallType.TCP_SYNC);
    }

    @Test
    public void udpRequestTest() {
        requestTest(ClientConfiguration.Protocol.UDP, TestClientThingHandler.CallType.UDP_SYNC);
    }

    @Test
    public void udpSendTest() {
        sendTest(ClientConfiguration.Protocol.UDP, TestClientThingHandler.CallType.UDP_ASYNC);
    }

    @Test
    public void tcpSendTest() {
        sendTest(ClientConfiguration.Protocol.TCP, TestClientThingHandler.CallType.TCP_ASYNC);
    }

    private void requestTest(ClientConfiguration.Protocol protocol, TestClientThingHandler.CallType expectedCallType) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.host = "localhost";
        clientConfiguration.port = 1;
        clientConfiguration.refresh = 1;
        clientConfiguration.protocol = protocol;

        TcpUdpChannelConfig tcpUdpChannelConfig = new TcpUdpChannelConfig();
        tcpUdpChannelConfig.stateContent = TEST_STATE_CONTENT;

        ClientThingHandler clientThingHandler = getClientThingHandler(clientConfiguration, tcpUdpChannelConfig);

        // check that the thing status is set to unknown
        verify(thingHandlerCallback).statusUpdated(clientThingHandler.getThing(),
                new ThingStatusInfo(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, null));

        // wait until we have at least three calls and stop the thing handler
        waitForAssert(() -> assertEquals(3, reports.size()));
        clientThingHandler.dispose();

        // get the exact number of calls and check if the channel was updated the same number of times
        int calls = reports.size();
        verify(thingHandlerCallback, times(calls)).stateUpdated(eq(TEST_CHANNEL_UID),
                eq(new StringType(TEST_STATE_CONTENT)));

        // check these were all UDP Sync Requests
        assertTrue(reports.stream().map(Map.Entry::getKey).allMatch(expectedCallType::equals));

        verifyNoMoreInteractions(thingHandlerCallback);
    }

    private void sendTest(ClientConfiguration.Protocol protocol, TestClientThingHandler.CallType expectedCallType) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.host = "localhost";
        clientConfiguration.port = 1;
        clientConfiguration.refresh = 1;
        clientConfiguration.protocol = protocol;

        TcpUdpChannelConfig tcpUdpChannelConfig = new TcpUdpChannelConfig();
        tcpUdpChannelConfig.mode = ChannelMode.WRITEONLY;

        ClientThingHandler clientThingHandler = getClientThingHandler(clientConfiguration, tcpUdpChannelConfig);
        clientThingHandler.handleCommand(TEST_CHANNEL_UID, new StringType(TEST_STATE_CONTENT));

        // check that the thing status is set to unknown
        verify(thingHandlerCallback).statusUpdated(clientThingHandler.getThing(),
                new ThingStatusInfo(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, null));

        // wait until we have at least one calls and stop the thing handler
        waitForAssert(() -> assertEquals(1, reports.size()));
        clientThingHandler.dispose();

        // check these were all UDP Async Requests
        assertTrue(reports.stream().map(Map.Entry::getKey).allMatch(expectedCallType::equals));

        // check the contents are all what we send
        assertTrue(reports.stream().map(Map.Entry::getValue).allMatch(TEST_STATE_CONTENT::equals));

        verifyNoMoreInteractions(thingHandlerCallback);
    }

    /**
     * create a ClientThingHandler and initialize it
     *
     * @param clientConfiguration the thing configuration
     * @param tcpUdpChannelConfig the channel configuration
     * @return the initialized ClientThingHandler
     */
    private ClientThingHandler getClientThingHandler(ClientConfiguration clientConfiguration,
            TcpUdpChannelConfig tcpUdpChannelConfig) {
        Channel channel = ChannelBuilder.create(TEST_CHANNEL_UID).withAcceptedItemType("String")
                .withType(CHANNEL_TYPE_UID)
                .withConfiguration(TestUtil.getConfigurationFromInstance(tcpUdpChannelConfig)).build();
        Thing thing = ThingBuilder.create(THING_TYPE_UID_CLIENT, TEST_THING_UID)
                .withConfiguration(TestUtil.getConfigurationFromInstance(clientConfiguration)).withChannel(channel)
                .build();

        TestClientThingHandler testClientThingHandler = new TestClientThingHandler(thing, valueTransformationProvider,
                simpleDynamicStateDescriptionProvider, reports::add);

        testClientThingHandler.setCallback(thingHandlerCallback);
        testClientThingHandler.initialize();

        return testClientThingHandler;
    }
}
