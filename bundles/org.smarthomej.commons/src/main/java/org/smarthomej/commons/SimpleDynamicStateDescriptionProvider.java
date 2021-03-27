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
package org.smarthomej.commons;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.DynamicStateDescriptionProvider;
import org.openhab.core.types.StateDescription;

/**
 * Dynamic channel state description provider.
 * Overrides the state description for the channels, which receive their configuration at runtime.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface SimpleDynamicStateDescriptionProvider extends DynamicStateDescriptionProvider {

    /**
     * Set a state description for a channel. This description will be used when preparing the channel state by
     * the framework for presentation. A previous description, if existed, will be replaced.
     *
     * @param channelUID channel UID
     * @param description state description for the channel
     */
    void setDescription(ChannelUID channelUID, StateDescription description);

    /**
     * remove all descriptions for a given thing
     *
     * @param thingUID the thing's UID
     */
    void removeDescriptionsForThing(ThingUID thingUID);
}
