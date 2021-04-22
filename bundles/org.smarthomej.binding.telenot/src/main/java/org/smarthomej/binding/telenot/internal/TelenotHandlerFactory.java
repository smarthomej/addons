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
package org.smarthomej.binding.telenot.internal;

import static org.smarthomej.binding.telenot.internal.TelenotBindingConstants.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.telenot.internal.handler.EMAStateHandler;
import org.smarthomej.binding.telenot.internal.handler.IPBridgeHandler;
import org.smarthomej.binding.telenot.internal.handler.InputHandler;
import org.smarthomej.binding.telenot.internal.handler.MBHandler;
import org.smarthomej.binding.telenot.internal.handler.MPHandler;
import org.smarthomej.binding.telenot.internal.handler.OutputHandler;
import org.smarthomej.binding.telenot.internal.handler.SBHandler;
import org.smarthomej.binding.telenot.internal.handler.TelenotBridgeHandler;

/**
 * The {@link TelenotHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.telenot", service = ThingHandlerFactory.class)
public class TelenotHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_IPBRIDGE, THING_TYPE_SB, THING_TYPE_MP, THING_TYPE_MB,
                    THING_TYPE_EMA_STATE, THING_TYPE_INPUT, THING_TYPE_OUTPUT).collect(Collectors.toSet()));

    private final Logger logger = LoggerFactory.getLogger(TelenotHandlerFactory.class);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    private final Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegMap = new HashMap<>();
    // Marked as Nullable only to fix incorrect redundant null check complaints from null annotations

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_IPBRIDGE.equals(thingTypeUID)) {
            IPBridgeHandler bridgeHandler = new IPBridgeHandler((Bridge) thing);
            registerDiscoveryService(bridgeHandler);
            return bridgeHandler;
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

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof TelenotBridgeHandler) {
            ServiceRegistration<?> serviceReg = discoveryServiceRegMap.remove(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                logger.debug("Unregistering discovery service.");
                serviceReg.unregister();
            }
        }
    }

    /**
     * Register a discovery service for a bridge handler.
     *
     * @param bridgeHandler bridge handler for which to register the discovery service
     */
    private synchronized void registerDiscoveryService(TelenotBridgeHandler bridgeHandler) {
        logger.debug("Registering discovery service.");
        TelenotDiscoveryService discoveryService = new TelenotDiscoveryService(bridgeHandler);
        bridgeHandler.setDiscoveryService(discoveryService);
        discoveryServiceRegMap.put(bridgeHandler.getThing().getUID(),
                bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, null));
    }
}
