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
package org.smarthomej.transform.chain.internal.test;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.profiles.ProfileContext;

/**
 * The {@link TestProfileContext} is a configuration wrapper for test uses
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TestProfileContext implements ProfileContext {
    private final Configuration configuration;

    public TestProfileContext(Map<String, Object> configuration) {
        this.configuration = new Configuration(configuration);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public ScheduledExecutorService getExecutorService() {
        throw new IllegalStateException();
    }
}
