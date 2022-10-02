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
package org.smarthomej.binding.viessmann.internal;

import static org.smarthomej.binding.viessmann.internal.ViessmannBindingConstants.*;

import java.util.Dictionary;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.net.HttpServiceUtil;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.viessmann.internal.handler.DeviceHandler;
import org.smarthomej.binding.viessmann.internal.handler.ViessmannBridgeHandler;

/**
 * The {@link ViessmannHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.viessmann", service = ThingHandlerFactory.class)
public class ViessmannHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(ViessmannHandlerFactory.class);

    private final HttpClient httpClient;
    private final BindingServlet bindingServlet;
    private final ViessmannDynamicCommandDescriptionProvider commandDescriptionProvider;
    private final ViessmannDynamicStateDescriptionProvider stateDescriptionProvider;

    private @Nullable String callbackUrl;

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_BRIDGE, THING_TYPE_DEVICE);

    @Activate
    public ViessmannHandlerFactory(@Reference HttpService httpService, @Reference HttpClientFactory httpClientFactory,
            final @Reference ViessmannDynamicCommandDescriptionProvider commandDescriptionProvider,
            final @Reference ViessmannDynamicStateDescriptionProvider stateDescriptionProvider) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.bindingServlet = new BindingServlet(httpService);
        this.commandDescriptionProvider = commandDescriptionProvider;
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        Dictionary<String, Object> properties = componentContext.getProperties();
        callbackUrl = (String) properties.get("callbackUrl");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        bindingServlet.dispose();
        super.deactivate(componentContext);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            bindingServlet.addAccountThing(thing);
            return new ViessmannBridgeHandler((Bridge) thing, httpClient, createCallbackUrl());
        } else if (THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return new DeviceHandler(thing, commandDescriptionProvider, stateDescriptionProvider);
        }
        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        BindingServlet bindingServlet = this.bindingServlet;
        bindingServlet.removeAccountThing(thingHandler.getThing());
    }

    private @Nullable String createCallbackUrl() {
        if (callbackUrl != null) {
            return callbackUrl;
        } else {
            // we do not use SSL as it can cause certificate validation issues.
            final int port = HttpServiceUtil.getHttpServicePort(bundleContext);
            if (port == -1) {
                logger.warn("Cannot find port of the http service.");
                return null;
            }
            return "http://localhost:" + port;
        }
    }
}
