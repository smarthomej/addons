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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.smarthomej.commons.impl.ThingUpdater;

/**
 * The {@link UpdatingBaseBridgeHandler} is an extension to the {@link BaseBridgeHandler} which allows updating thing
 * channels on bundle upgrade
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class UpdatingBaseBridgeHandler extends BaseBridgeHandler {
    public static final String PROPERTY_THING_TYPE_VERSION = "thingTypeVersion";

    private final ThingUpdater thingUpdater;

    /**
     * Creates a new instance of this class for the {@link Thing}.
     *
     * @param bridge the bridge that should be handled, not null
     */
    public UpdatingBaseBridgeHandler(Bridge bridge) {
        super(bridge);

        thingUpdater = new ThingUpdater(bridge, this.getClass());
    }

    @Override
    public void setCallback(@Nullable ThingHandlerCallback thingHandlerCallback) {
        super.setCallback(thingHandlerCallback);
        if (thingHandlerCallback != null && thingUpdater.thingNeedsUpdate()) {
            thingUpdater.update(editThing(), thingHandlerCallback, this::isInitialized, this::updateThing);
        }
    }
}
