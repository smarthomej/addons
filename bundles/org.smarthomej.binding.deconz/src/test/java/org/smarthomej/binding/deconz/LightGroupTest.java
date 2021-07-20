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
package org.smarthomej.binding.deconz;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.smarthomej.binding.deconz.internal.BindingConstants.*;
import static org.smarthomej.commons.UpdatingBaseBridgeHandler.PROPERTY_THING_TYPE_VERSION;

import java.io.IOException;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.smarthomej.binding.deconz.internal.DeconzDynamicCommandDescriptionProvider;
import org.smarthomej.binding.deconz.internal.dto.GroupMessage;
import org.smarthomej.binding.deconz.internal.handler.GroupThingHandler;
import org.smarthomej.binding.deconz.internal.types.GroupType;
import org.smarthomej.binding.deconz.internal.types.GroupTypeDeserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class provides tests for deconz light groups
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class LightGroupTest {
    private @NonNullByDefault({}) Gson gson;

    private @Mock @NonNullByDefault({}) ThingHandlerCallback thingHandlerCallback;
    private @Mock @NonNullByDefault({}) DeconzDynamicCommandDescriptionProvider commandDescriptionProvider;

    @BeforeEach
    public void initialize() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(GroupType.class, new GroupTypeDeserializer());
        gson = gsonBuilder.create();
    }

    @Test
    public void lightGroupUpdateTest() throws IOException {
        GroupMessage lightMessage = DeconzTest.getObjectFromJson("group.json", GroupMessage.class, gson);
        assertNotNull(lightMessage);

        ThingUID thingUID = new ThingUID("deconz", "lightgroup");
        ChannelUID channelUIDAllOn = new ChannelUID(thingUID, CHANNEL_ALL_ON);
        ChannelUID channelUIDAnyOn = new ChannelUID(thingUID, CHANNEL_ANY_ON);

        Thing group = ThingBuilder.create(THING_TYPE_LIGHTGROUP, thingUID)
                .withProperties(Map.of(PROPERTY_THING_TYPE_VERSION, "1"))
                .withChannel(ChannelBuilder.create(channelUIDAllOn, CoreItemFactory.SWITCH).build())
                .withChannel(ChannelBuilder.create(channelUIDAnyOn, CoreItemFactory.SWITCH).build()).build();
        GroupThingHandler groupThingHandler = new GroupThingHandler(group, gson, commandDescriptionProvider);
        groupThingHandler.setCallback(thingHandlerCallback);

        groupThingHandler.messageReceived(lightMessage);
        Mockito.verify(thingHandlerCallback).stateUpdated(eq(channelUIDAllOn), eq(OnOffType.OFF));
        Mockito.verify(thingHandlerCallback).stateUpdated(eq(channelUIDAnyOn), eq(OnOffType.OFF));
    }
}
