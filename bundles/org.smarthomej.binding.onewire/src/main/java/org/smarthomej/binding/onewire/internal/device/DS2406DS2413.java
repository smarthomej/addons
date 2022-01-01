/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.onewire.internal.device;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.binding.onewire.internal.DigitalIoConfig;
import org.smarthomej.binding.onewire.internal.OwException;
import org.smarthomej.binding.onewire.internal.SensorId;
import org.smarthomej.binding.onewire.internal.handler.OwBaseThingHandler;
import org.smarthomej.binding.onewire.internal.owserver.OwserverDeviceParameter;

/**
 * The {@link DS2406DS2413} class defines an DS2406 or DS2413 device
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DS2406DS2413 extends AbstractDigitalOwDevice {
    public DS2406DS2413(SensorId sensorId, OwBaseThingHandler callback) {
        super(sensorId, callback);
    }

    @Override
    public void configureChannels() throws OwException {
        ioConfig.clear();
        ioConfig.add(new DigitalIoConfig(callback.getThing(), 0, new OwserverDeviceParameter("uncached/", "/sensed.A"),
                new OwserverDeviceParameter("/PIO.A")));
        ioConfig.add(new DigitalIoConfig(callback.getThing(), 1, new OwserverDeviceParameter("uncached/", "/sensed.B"),
                new OwserverDeviceParameter("/PIO.B")));

        fullInParam = new OwserverDeviceParameter("uncached/", "/sensed.BYTE");
        fullOutParam = new OwserverDeviceParameter("/PIO.BYTE");

        super.configureChannels();
    }
}
