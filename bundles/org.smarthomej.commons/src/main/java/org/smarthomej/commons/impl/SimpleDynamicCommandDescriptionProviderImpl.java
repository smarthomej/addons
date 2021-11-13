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
package org.smarthomej.commons.impl;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseDynamicCommandDescriptionProvider;
import org.openhab.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.DynamicCommandDescriptionProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.commons.SimpleDynamicCommandDescriptionProvider;

/**
 * Dynamic channel command description provider.
 * Overrides the command description for the controls, which receive its configuration in the runtime.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = { DynamicCommandDescriptionProvider.class, SimpleDynamicCommandDescriptionProvider.class })
public class SimpleDynamicCommandDescriptionProviderImpl extends BaseDynamicCommandDescriptionProvider
        implements SimpleDynamicCommandDescriptionProvider {

    @Activate
    public SimpleDynamicCommandDescriptionProviderImpl(final @Reference EventPublisher eventPublisher, //
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry, //
            final @Reference ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService) {
        this.eventPublisher = eventPublisher;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
    }

    private final Logger logger = LoggerFactory.getLogger(SimpleDynamicCommandDescriptionProviderImpl.class);

    @Override
    public void removeCommandDescriptionForThing(ThingUID thingUID) {
        logger.trace("removing state description for thing {}", thingUID);
        channelOptionsMap.entrySet().removeIf(entry -> entry.getKey().getThingUID().equals(thingUID));
    }
}
