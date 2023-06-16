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
package org.smarthomej.binding.tuya.internal.local;

import java.util.concurrent.ScheduledExecutorService;

import org.openhab.core.thing.Thing;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.smarthomej.binding.tuya.internal.config.DeviceConfiguration;

import com.google.gson.Gson;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Has all the required dependencies and creates {@link TuyaDeviceManager}
 *
 * @author Vitalii Herhel - Initial contribution
 */
@Component(service = TuyaDeviceManagerFactory.class, configurationPid = "tuya.deviceManagerFactory")
public class TuyaDeviceManagerFactory {

    private final Gson gson = new Gson();
    private final EventLoopGroup eventLoopGroup;
    private final UdpDiscoveryListener udpDiscoveryListener;

    @Activate
    public TuyaDeviceManagerFactory() {
        this.eventLoopGroup = new NioEventLoopGroup();
        this.udpDiscoveryListener = new UdpDiscoveryListener(eventLoopGroup);
    }

    public TuyaDeviceManager create(Thing thing, DeviceStatusListener deviceStatusListener,
            ScheduledExecutorService scheduler) {
        return new TuyaDeviceManager(thing, deviceStatusListener, gson, eventLoopGroup,
                thing.getConfiguration().as(DeviceConfiguration.class), udpDiscoveryListener, scheduler);
    }

    @Deactivate
    public void deactivate() {
        udpDiscoveryListener.deactivate();
        eventLoopGroup.shutdownGracefully();
    }
}
