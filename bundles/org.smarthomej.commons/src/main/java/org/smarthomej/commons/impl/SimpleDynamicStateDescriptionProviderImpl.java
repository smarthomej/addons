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

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.DynamicStateDescriptionProvider;
import org.openhab.core.types.StateDescription;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.commons.SimpleDynamicStateDescriptionProvider;

/**
 * Dynamic channel state description provider.
 * Overrides the state description for the controls, which receive its configuration in the runtime.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = { DynamicStateDescriptionProvider.class,
        SimpleDynamicStateDescriptionProvider.class }, immediate = true)
public class SimpleDynamicStateDescriptionProviderImpl implements SimpleDynamicStateDescriptionProvider {

    private final Map<ChannelUID, StateDescription> descriptions = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(SimpleDynamicStateDescriptionProviderImpl.class);

    @Override
    public void setDescription(ChannelUID channelUID, StateDescription description) {
        logger.trace("adding state description for channel {}", channelUID);
        descriptions.put(channelUID, description);
    }

    @Override
    public void removeDescriptionsForThing(ThingUID thingUID) {
        logger.trace("removing state description for thing {}", thingUID);
        descriptions.entrySet().removeIf(entry -> entry.getKey().getThingUID().equals(thingUID));
    }

    @Override
    public @Nullable StateDescription getStateDescription(Channel channel,
            @Nullable StateDescription originalStateDescription, @Nullable Locale locale) {
        if (descriptions.containsKey(channel.getUID())) {
            logger.trace("returning new stateDescription for {}", channel.getUID());
            return descriptions.get(channel.getUID());
        } else {
            return null;
        }
    }
}
