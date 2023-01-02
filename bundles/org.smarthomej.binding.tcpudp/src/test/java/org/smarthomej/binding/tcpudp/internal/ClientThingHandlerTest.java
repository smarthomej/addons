/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.smarthomej.binding.tcpudp.internal.TcpUdpBindingConstants.BINDING_ID;
import static org.smarthomej.binding.tcpudp.internal.TcpUdpBindingConstants.THING_TYPE_UID_CLIENT;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
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
import org.smarthomej.binding.tcpudp.internal.test.EchoServer;
import org.smarthomej.binding.tcpudp.internal.test.TestUtil;
import org.smarthomej.commons.SimpleDynamicStateDescriptionProvider;
import org.smarthomej.commons.itemvalueconverter.ChannelMode;
import org.smarthomej.commons.transform.NoOpValueTransformation;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link ClientThingHandlerTest} is a test class for {@link ClientThingHandler}
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

    @Mock
    private @NonNullByDefault({}) ThingHandlerCallback thingHandlerCallback;

    @Mock
    private @NonNullByDefault({}) ValueTransformationProvider valueTransformationProvider;

    @Mock
    private @NonNullByDefault({}) SimpleDynamicStateDescriptionProvider simpleDynamicStateDescriptionProvider;

    @BeforeEach
    public void startUp() {
        Mockito.doReturn(NoOpValueTransformation.getInstance()).when(valueTransformationProvider)
                .getValueTransformation(any());
    }

    @Test
    public void tcpRequestTest() {
        requestTest(ClientConfiguration.Protocol.TCP);
    }

    @Test
    public void udpRequestTest() {
        requestTest(ClientConfiguration.Protocol.UDP);
    }

    @Test
    public void udpSendTest() {
        sendTest(ClientConfiguration.Protocol.UDP);
    }

    @Test
    public void tcpSendTest() {
        sendTest(ClientConfiguration.Protocol.TCP);
    }

    private void requestTest(ClientConfiguration.Protocol protocol) {
        EchoServer echoServer = new EchoServer(protocol);
        waitForAssert(() -> assertNotEquals(0, echoServer.getPort(), "Could not start EchoServer"));

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.host = "127.0.0.1";
        clientConfiguration.port = echoServer.getPort();
        clientConfiguration.refresh = 1;
        clientConfiguration.protocol = protocol;

        TcpUdpChannelConfig tcpUdpChannelConfig = new TcpUdpChannelConfig();
        tcpUdpChannelConfig.stateContent = TEST_STATE_CONTENT;

        ClientThingHandler clientThingHandler = getClientThingHandler(clientConfiguration, tcpUdpChannelConfig);

        // check that the thing status is set to unknown
        verify(thingHandlerCallback).statusUpdated(clientThingHandler.getThing(),
                new ThingStatusInfo(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, null));

        // wait until we have at least three calls and stop the thing handler
        waitForAssert(() -> assertEquals(3, echoServer.getReceivedValues().size()));
        verify(thingHandlerCallback, timeout(200).times(3)).stateUpdated(eq(TEST_CHANNEL_UID),
                eq(new StringType(TEST_STATE_CONTENT)));

        clientThingHandler.dispose();
        echoServer.stop();
    }

    private void sendTest(ClientConfiguration.Protocol protocol) {
        EchoServer echoServer = new EchoServer(protocol);
        waitForAssert(() -> assertNotEquals(0, echoServer.getPort(), "Could not start EchoServer"));

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.host = "127.0.0.1";
        clientConfiguration.port = echoServer.getPort();
        clientConfiguration.protocol = protocol;

        TcpUdpChannelConfig tcpUdpChannelConfig = new TcpUdpChannelConfig();
        tcpUdpChannelConfig.mode = ChannelMode.WRITEONLY;

        ClientThingHandler clientThingHandler = getClientThingHandler(clientConfiguration, tcpUdpChannelConfig);

        clientThingHandler.handleCommand(TEST_CHANNEL_UID, new StringType(TEST_STATE_CONTENT));

        // check that the thing status is set to unknown
        verify(thingHandlerCallback).statusUpdated(clientThingHandler.getThing(),
                new ThingStatusInfo(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, null));

        // wait until we have at least one calls and stop the thing handler
        waitForAssert(() -> assertEquals(1, echoServer.getReceivedValues().size()));
        clientThingHandler.dispose();

        // check the contents are all what we send
        List<String> receivedValues = echoServer.getReceivedValues();
        assertTrue(receivedValues.stream().allMatch(TEST_STATE_CONTENT::equals));

        // no state updates
        verify(thingHandlerCallback, never()).stateUpdated(any(), any());

        echoServer.stop();
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

        ClientThingHandler testClientThingHandler = new ClientThingHandler(thing, valueTransformationProvider,
                simpleDynamicStateDescriptionProvider);

        testClientThingHandler.setCallback(thingHandlerCallback);

        testClientThingHandler.initialize();

        return testClientThingHandler;
    }
}
