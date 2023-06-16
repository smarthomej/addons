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
package org.smarthomej.binding.tuya.internal.handler;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.smarthomej.binding.tuya.internal.local.TuyaDeviceManager;
import org.smarthomej.binding.tuya.internal.local.TuyaDeviceManagerFactory;
import org.smarthomej.binding.tuya.internal.util.SchemaDp;
import org.smarthomej.commons.SimpleDynamicCommandDescriptionProvider;

/**
 * The {@link TuyaDeviceHandler} handles commands and state updates
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TuyaDeviceHandler extends BaseTuyaDeviceHandler {

    private final TuyaDeviceManagerFactory tuyaDeviceManagerFactory;

    public TuyaDeviceHandler(Thing thing, @Nullable List<SchemaDp> schemaDps,
            SimpleDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider,
            TuyaDeviceManagerFactory tuyaDeviceManagerFactory) {
        super(thing, schemaDps, dynamicCommandDescriptionProvider);
        this.tuyaDeviceManagerFactory = tuyaDeviceManagerFactory;
    }

    @Override
    public void initialize() {
        super.initialize();
        tuyaDeviceManager = tuyaDeviceManagerFactory.create(getThing(), this, scheduler);
    }

    @Override
    public void dispose() {
        super.dispose();
        TuyaDeviceManager tuyaDeviceManager = this.tuyaDeviceManager;
        if (tuyaDeviceManager != null) {
            tuyaDeviceManager.dispose();
        }
    }
}
