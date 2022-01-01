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
package org.smarthomej.commons;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.smarthomej.commons.UpdatingBaseThingHandler.PROPERTY_THING_TYPE_VERSION;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;

/**
 * The {@link UpdatingBaseBridgeHandlerTest} contains test cases for the {@link UpdatingBaseBridgeHandler}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UpdatingBaseBridgeHandlerTest {
    private static final ThingTypeUID THING_TYPE_ADD_UID = new ThingTypeUID("testBinding", "testThingTypeAdd");
    private static final ThingTypeUID THING_TYPE_REMOVE_UID = new ThingTypeUID("testBinding", "testThingTypeRemove");
    private static final ThingTypeUID THING_TYPE_UPDATE_UID = new ThingTypeUID("testBinding", "testThingTypeUpdate");
    private static final ThingTypeUID THING_TYPE_MULTIPLE_UID = new ThingTypeUID("testBinding",
            "testThingTypeMultiple");

    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID("testBinding", "testChannelId");
    private static final ChannelTypeUID UPDATED_CHANNEL_TYPE_UID = new ChannelTypeUID("testBinding",
            "testChannelNewId");

    @Mock
    private @NonNullByDefault({}) ThingHandlerCallback callback;

    @Captor
    private @NonNullByDefault({}) ArgumentCaptor<Bridge> thingCaptor;

    private @NonNullByDefault({}) AutoCloseable closeable;

    @BeforeEach
    public void init() {
        closeable = MockitoAnnotations.openMocks(this);
        Mockito.when(callback.createChannelBuilder(any(), any())).thenAnswer(invocation -> {
            ChannelUID channelUID = (ChannelUID) invocation.getArguments()[0];
            ChannelTypeUID channelTypeUID = (ChannelTypeUID) invocation.getArguments()[1];

            return ChannelBuilder.create(channelUID).withType(channelTypeUID);
        });
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void addChannelTest() {
        Bridge bridge = BridgeBuilder.create(THING_TYPE_ADD_UID, "testThingId").build();

        TestBridgeHandler testBridgeHandler = new TestBridgeHandler(bridge);
        testBridgeHandler.setCallback(callback);

        Mockito.verify(callback, timeout(2000)).thingUpdated(thingCaptor.capture());
        Assertions.assertEquals(2, testBridgeHandler.initializeCounter);
        Bridge newBridge = thingCaptor.getValue();

        Channel newChannel = newBridge.getChannel("testChannel1");
        Assertions.assertNotNull(newChannel);
        Objects.requireNonNull(newChannel);
        Assertions.assertNull(newChannel.getLabel());
        Assertions.assertNull(newChannel.getDescription());
        Assertions.assertEquals(CHANNEL_TYPE_UID, newChannel.getChannelTypeUID());

        newChannel = newBridge.getChannel("testChannel2");
        Assertions.assertNotNull(newChannel);
        Objects.requireNonNull(newChannel);
        Assertions.assertEquals("Test Label", newChannel.getLabel());
        Assertions.assertNull(newChannel.getDescription());
        Assertions.assertEquals(CHANNEL_TYPE_UID, newChannel.getChannelTypeUID());

        newChannel = newBridge.getChannel("testChannel3");
        Assertions.assertNotNull(newChannel);
        Objects.requireNonNull(newChannel);
        Assertions.assertEquals("Test Label", newChannel.getLabel());
        Assertions.assertEquals("Test Description", newChannel.getDescription());
        Assertions.assertEquals(CHANNEL_TYPE_UID, newChannel.getChannelTypeUID());
        Assertions.assertEquals("1", newBridge.getProperties().get(PROPERTY_THING_TYPE_VERSION));
    }

    @Test
    public void removeChannelTest() {
        ThingUID thingUID = new ThingUID(THING_TYPE_REMOVE_UID, "testThingId");
        Bridge bridge = BridgeBuilder.create(THING_TYPE_REMOVE_UID, thingUID)
                .withChannel(ChannelBuilder.create(new ChannelUID(thingUID, "testChannel"), "String").build()).build();

        TestBridgeHandler testBridgeHandler = new TestBridgeHandler(bridge);
        testBridgeHandler.setCallback(callback);

        Mockito.verify(callback, timeout(2000)).thingUpdated(thingCaptor.capture());
        Assertions.assertEquals(2, testBridgeHandler.initializeCounter);
        Bridge newBridge = thingCaptor.getValue();

        Assertions.assertEquals(0, newBridge.getChannels().size());

        Assertions.assertEquals("1", newBridge.getProperties().get(PROPERTY_THING_TYPE_VERSION));
    }

    @Test
    public void updateChannelTest() {
        ThingUID thingUID = new ThingUID(THING_TYPE_UPDATE_UID, "testThingId");
        Bridge bridge = BridgeBuilder.create(THING_TYPE_UPDATE_UID, thingUID)
                .withChannel(ChannelBuilder.create(new ChannelUID(thingUID, "testChannel"), "Number").build()).build();

        TestBridgeHandler testBridgeHandler = new TestBridgeHandler(bridge);
        testBridgeHandler.setCallback(callback);

        Mockito.verify(callback, timeout(2000)).thingUpdated(thingCaptor.capture());
        Assertions.assertEquals(2, testBridgeHandler.initializeCounter);
        Bridge newBridge = thingCaptor.getValue();

        Channel newChannel = newBridge.getChannel("testChannel");
        Assertions.assertNotNull(newChannel);
        Objects.requireNonNull(newChannel);

        Assertions.assertEquals("Test Label", newChannel.getLabel());
        Assertions.assertEquals(UPDATED_CHANNEL_TYPE_UID, newChannel.getChannelTypeUID());

        Assertions.assertEquals("1", newBridge.getProperties().get(PROPERTY_THING_TYPE_VERSION));
    }

    @Test
    public void multipleChannelTest() {
        ThingUID thingUID = new ThingUID(THING_TYPE_UPDATE_UID, "testThingId");
        Bridge bridge = BridgeBuilder.create(THING_TYPE_MULTIPLE_UID, thingUID)
                .withChannel(ChannelBuilder.create(new ChannelUID(thingUID, "testChannel0"), "Number").build())
                .withChannel(ChannelBuilder.create(new ChannelUID(thingUID, "testChannel1"), "Number").build()).build();

        TestBridgeHandler testBridgeHandler = new TestBridgeHandler(bridge);
        testBridgeHandler.setCallback(callback);

        Mockito.verify(callback, timeout(2000)).thingUpdated(thingCaptor.capture());
        Assertions.assertEquals(2, testBridgeHandler.initializeCounter);
        Bridge newBridge = thingCaptor.getValue();

        // added channel
        Channel newChannel = newBridge.getChannel("testChannel2");
        Assertions.assertNotNull(newChannel);
        Objects.requireNonNull(newChannel);
        Assertions.assertEquals("TestLabel", newChannel.getLabel());
        Assertions.assertEquals(CHANNEL_TYPE_UID, newChannel.getChannelTypeUID());

        // updated channel
        Channel updatedChannel = newBridge.getChannel("testChannel1");
        Assertions.assertNotNull(updatedChannel);
        Objects.requireNonNull(updatedChannel);
        Assertions.assertEquals("Test Label", updatedChannel.getLabel());
        Assertions.assertEquals(UPDATED_CHANNEL_TYPE_UID, updatedChannel.getChannelTypeUID());

        // removed channel
        Channel removedChannel = newBridge.getChannel("testChannel0");
        Assertions.assertNull(removedChannel);

        Assertions.assertEquals("3", newBridge.getProperties().get(PROPERTY_THING_TYPE_VERSION));
    }

    @Test
    public void noUpdateIfHigherVersionTest() {
        ThingUID thingUID = new ThingUID(THING_TYPE_UPDATE_UID, "testThingId");
        Bridge bridge = BridgeBuilder.create(THING_TYPE_UPDATE_UID, thingUID)
                .withProperties(Map.of(PROPERTY_THING_TYPE_VERSION, "2"))
                .withChannel(ChannelBuilder.create(new ChannelUID(thingUID, "testChannel"), "Number").build()).build();

        TestBridgeHandler testBridgeHandler = new TestBridgeHandler(bridge);
        testBridgeHandler.setCallback(callback);

        Mockito.verify(callback, never()).thingUpdated(any());
    }

    private static class TestBridgeHandler extends UpdatingBaseBridgeHandler {
        public TestBridgeHandler(Bridge bridge) {
            super(bridge);
        }

        private int initializeCounter = 0;

        @Override
        protected boolean isInitialized() {
            return ++initializeCounter > 1;
        }

        @Override
        public void initialize() {
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
        }
    }
}
