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
package org.smarthomej.binding.irobot.internal.handler;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 * Test the MQTT protocol with local iRobot (without openhab running).
 * This class is used to test the binding against a local iRobot instance.
 *
 * @author Florian Binder - Initial contribution
 */

@ExtendWith(MockitoExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
@NonNullByDefault
class RoombaHandlerTest {
    private static final String IP_ADDRESS = "<iRobotIP>";
    private static final String PASSWORD = "<PasswordForIRobot>";

    private @NonNullByDefault({}) RoombaHandler handler;
    private @NonNullByDefault({}) @Mock Thing myThing;
    private @NonNullByDefault({}) ThingHandlerCallback callback;

    @BeforeEach
    void setUp() throws Exception {
        Logger l = LoggerFactory.getLogger(RoombaHandler.class);
        Field f = l.getClass().getDeclaredField("currentLogLevel");
        f.setAccessible(true);
        f.set(l, LocationAwareLogger.TRACE_INT);

        Configuration config = new Configuration();
        config.put("ipaddress", RoombaHandlerTest.IP_ADDRESS);
        config.put("password", RoombaHandlerTest.PASSWORD);

        Mockito.when(myThing.getConfiguration()).thenReturn(config);
        Mockito.when(myThing.getUID()).thenReturn(new ThingUID("mocked", "irobot", "uid"));

        callback = Mockito.mock(ThingHandlerCallback.class);

        handler = new RoombaHandler(myThing);
        handler.setCallback(callback);
    }

    // @Test
    void testInit() throws InterruptedException, IOException {
        handler.initialize();
        Mockito.verify(myThing, Mockito.times(1)).getConfiguration();

        Objects.requireNonNull(System.in).read();
        handler.dispose();
    }

    // @Test
    void testCleanRegion() throws IOException, InterruptedException {
        handler.initialize();

        Objects.requireNonNull(System.in).read();

        ChannelUID cmd = new ChannelUID("my:thi:blabla:command");
        handler.handleCommand(cmd, new StringType("cleanRegions:AABBCCDDEEFFGGHH;2,3"));

        Objects.requireNonNull(System.in).read();
        handler.dispose();
    }

    // @Test
    void testDock() throws IOException, InterruptedException {
        handler.initialize();

        Objects.requireNonNull(System.in).read();

        ChannelUID cmd = new ChannelUID("my:thi:blabla:command");
        handler.handleCommand(cmd, new StringType("dock"));

        Objects.requireNonNull(System.in).read();
        handler.dispose();
    }

    // @Test
    void testStop() throws IOException, InterruptedException {
        handler.initialize();

        Objects.requireNonNull(System.in).read();

        ChannelUID cmd = new ChannelUID("my:thi:blabla:command");
        handler.handleCommand(cmd, new StringType("stop"));

        Objects.requireNonNull(System.in).read();
        handler.dispose();
    }
}
