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
package org.smarthomej.automation.javarule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Util} contains utilities
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private Util() {
        // prevent instantiation
    }

    /**
     * Get OSGi by defining class/interface
     *
     * @param clazz the requested service definition class/interface
     * @return a list of OSGi-services
     */
    public static <T> List<T> findServices(Class<T> clazz) {
        try {
            BundleContext context = FrameworkUtil.getBundle(Util.class).getBundleContext();
            return context.getServiceReferences(clazz, null).stream().map(context::getService)
                    .collect(Collectors.toList());
        } catch (InvalidSyntaxException e) {
            return List.of();
        }
    }

    /**
     * Get a {@link ThingActions} instance for a given thing and scope
     *
     * @param scope the scope of the action
     * @param thingUid the thingUID as string
     * @return an Optional with the requested action, empty if not found
     */
    @SuppressWarnings("unused")
    public static Optional<ThingActions> getAction(String scope, String thingUid) {
        return findServices(ThingActions.class).stream().filter(action -> {
            ThingActionsScope annotatedScope = action.getClass().getAnnotation(ThingActionsScope.class);
            ThingHandler thingHandler = action.getThingHandler();

            return (annotatedScope != null && scope.equals(annotatedScope.name()) && thingHandler != null
                    && thingUid.equals(thingHandler.getThing().getUID().toString()));
        }).findFirst();
    }

    public static String removeExtension(String name) {
        return name.substring(0, name.lastIndexOf("."));
    }

    /**
     * Check if a folder exists, is readable and writable or create if missing
     *
     * @param folder the folder to check
     * @return true if successful, false if failed
     */
    public static boolean checkFolder(Path folder) {
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            LOGGER.warn("Failed to create directory '{}': {}", folder, e.getMessage());
            return false;
        }
        if (!Files.isWritable(folder) || !Files.isReadable(folder)) {
            LOGGER.warn("Directory '{}' must be available for read and write", folder);
            return false;
        }
        return true;
    }
}
