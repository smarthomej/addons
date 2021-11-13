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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.types.CommandOption;

/**
 * Dynamic channel command description provider.
 * Overrides the command description for the controls, which receive its configuration in the runtime.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface SimpleDynamicCommandDescriptionProvider {
    /**
     * remove all descriptions for a given thing
     *
     * @param thingUID the thing's UID
     */
    void removeCommandDescriptionForThing(ThingUID thingUID);

    /**
     * For a given {@link ChannelUID}, set a {@link List} of {@link CommandOption}s that should be used for the channel,
     * instead of the one defined statically in the {@link ChannelType}.
     *
     * @param channelUID the {@link ChannelUID} of the channel
     * @param options a {@link List} of {@link CommandOption}s
     */
    void setCommandOptions(ChannelUID channelUID, List<CommandOption> options);
}
