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
package org.smarthomej.binding.telenot.internal;

import static org.smarthomej.binding.telenot.internal.TelenotBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smarthomej.binding.telenot.internal.handler.EMAStateHandler;
import org.smarthomej.binding.telenot.internal.handler.IPBridgeHandler;
import org.smarthomej.binding.telenot.internal.handler.InputHandler;
import org.smarthomej.binding.telenot.internal.handler.MBHandler;
import org.smarthomej.binding.telenot.internal.handler.MPHandler;
import org.smarthomej.binding.telenot.internal.handler.OutputHandler;
import org.smarthomej.binding.telenot.internal.handler.SBHandler;
import org.smarthomej.binding.telenot.internal.handler.SerialBridgeHandler;

/**
 * The {@link TelenotHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.telenot", service = ThingHandlerFactory.class)
public class TelenotHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_IPBRIDGE,
            THING_TYPE_SERIALBRIDGE, THING_TYPE_SB, THING_TYPE_MP, THING_TYPE_MB, THING_TYPE_EMA_STATE,
            THING_TYPE_INPUT, THING_TYPE_OUTPUT);

    private final SerialPortManager serialPortManager;

    @Activate
    public TelenotHandlerFactory(final @Reference SerialPortManager serialPortManager) {
        // Obtain the serial port manager service using an OSGi reference
        this.serialPortManager = serialPortManager;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_IPBRIDGE.equals(thingTypeUID)) {
            return new IPBridgeHandler((Bridge) thing);
        } else if (THING_TYPE_SERIALBRIDGE.equals(thingTypeUID)) {
            return new SerialBridgeHandler((Bridge) thing, serialPortManager);
        } else if (THING_TYPE_SB.equals(thingTypeUID)) {
            return new SBHandler(thing);
        } else if (THING_TYPE_MB.equals(thingTypeUID)) {
            return new MBHandler(thing);
        } else if (THING_TYPE_MP.equals(thingTypeUID)) {
            return new MPHandler(thing);
        } else if (THING_TYPE_EMA_STATE.equals(thingTypeUID)) {
            return new EMAStateHandler(thing);
        } else if (THING_TYPE_INPUT.equals(thingTypeUID)) {
            return new InputHandler(thing);
        } else if (THING_TYPE_OUTPUT.equals(thingTypeUID)) {
            return new OutputHandler(thing);
        }
        return null;
    }
}
