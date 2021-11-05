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
package org.smarthomej.binding.knx.internal.factory;

import static org.smarthomej.binding.knx.internal.KNXBindingConstants.*;

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smarthomej.binding.knx.internal.handler.DeviceThingHandler;
import org.smarthomej.binding.knx.internal.handler.IPBridgeThingHandler;
import org.smarthomej.binding.knx.internal.handler.SerialBridgeThingHandler;

/**
 * The {@link KNXHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Simon Kaufmann - Initial contribution and API
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.knx")
@NonNullByDefault
public class KNXHandlerFactory extends BaseThingHandlerFactory {

    public static final Collection<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_DEVICE,
            THING_TYPE_IP_BRIDGE, THING_TYPE_SERIAL_BRIDGE);

    private final NetworkAddressService networkAddressService;

    @Activate
    public KNXHandlerFactory(@Reference NetworkAddressService networkAddressService) {
        this.networkAddressService = networkAddressService;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        if (THING_TYPE_IP_BRIDGE.equals(thingTypeUID)) {
            ThingUID ipBridgeThingUID = getIPBridgeThingUID(thingTypeUID, thingUID, configuration);
            return super.createThing(thingTypeUID, configuration, ipBridgeThingUID, null);
        }
        if (THING_TYPE_SERIAL_BRIDGE.equals(thingTypeUID)) {
            ThingUID serialBridgeUID = getSerialBridgeThingUID(thingTypeUID, thingUID, configuration);
            return super.createThing(thingTypeUID, configuration, serialBridgeUID, null);
        }
        if (THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        }
        return null;
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        if (thing.getThingTypeUID().equals(THING_TYPE_IP_BRIDGE)) {
            return new IPBridgeThingHandler((Bridge) thing, networkAddressService);
        } else if (thing.getThingTypeUID().equals(THING_TYPE_SERIAL_BRIDGE)) {
            return new SerialBridgeThingHandler((Bridge) thing);
        } else if (thing.getThingTypeUID().equals(THING_TYPE_DEVICE)) {
            return new DeviceThingHandler(thing);
        }
        return null;
    }

    private ThingUID getIPBridgeThingUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID,
            Configuration configuration) {
        if (thingUID != null) {
            return thingUID;
        }
        String ipAddress = (String) configuration.get(IP_ADDRESS);
        return new ThingUID(thingTypeUID, ipAddress);
    }

    private ThingUID getSerialBridgeThingUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID,
            Configuration configuration) {
        if (thingUID != null) {
            return thingUID;
        }
        String serialPort = (String) configuration.get(SERIAL_PORT);
        return new ThingUID(thingTypeUID, serialPort);
    }
}
