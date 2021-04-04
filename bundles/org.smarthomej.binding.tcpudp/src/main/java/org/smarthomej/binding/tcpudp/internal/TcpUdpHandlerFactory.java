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

import static org.smarthomej.binding.tcpudp.internal.TcpUdpBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smarthomej.commons.SimpleDynamicStateDescriptionProvider;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link TcpUdpHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.tcpudp", service = ThingHandlerFactory.class)
public class TcpUdpHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_UID_CLIENT);

    private final ValueTransformationProvider valueTransformationProvider;
    private final SimpleDynamicStateDescriptionProvider dynamicStateDescriptionProvider;

    @Activate
    public TcpUdpHandlerFactory(@Reference ValueTransformationProvider valueTransformationProvider,
            @Reference SimpleDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        this.valueTransformationProvider = valueTransformationProvider;
        this.dynamicStateDescriptionProvider = dynamicStateDescriptionProvider;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_UID_CLIENT.equals(thingTypeUID)) {
            return new ClientThingHandler(thing, valueTransformationProvider, dynamicStateDescriptionProvider);
        }

        return null;
    }
}
