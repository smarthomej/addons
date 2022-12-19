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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smarthomej.binding.viessmann.internal.handler.DeviceHandler;

/**
 * The {@link ViessmannConsoleCommandExtension} class
 *
 * @author Ronny Grun - Initial contribution
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class ViessmannConsoleCommandExtension extends AbstractConsoleCommandExtension {
    private static final String LIST_DEVICES = "listDevices";
    private static final String RELOAD_CHANNELS = "reloadChannels";

    private final ViessmannHandlerFactory handlerFactory;

    @Activate
    public ViessmannConsoleCommandExtension(@Reference ViessmannHandlerFactory handlerFactory) {
        super("viessmann", "Manage the viessmann binding");

        this.handlerFactory = handlerFactory;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String command = args[0];
            switch (command) {
                case LIST_DEVICES:
                    listDevices(console);
                    break;
                case RELOAD_CHANNELS:
                    if (args.length == 2) {
                        reloadChannels(console, args[1]);
                    } else {
                        console.println("Invalid use of command '" + command + "'");
                        printUsage(console);
                    }
                    break;
                default:
                    console.println("Unknown command '" + command + "'");
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    private void listDevices(Console console) {
        Set<DeviceHandler> deviceHandler = handlerFactory.getDeviceHandlers();

        deviceHandler.forEach(handler -> console
                .println("ThingUID: " + handler.getThing().getUID() + " ('" + handler.getThing().getLabel() + "')"));
    }

    private void reloadChannels(Console console, String thingUid) {
        Optional<DeviceHandler> deviceHandler = handlerFactory.getDeviceHandlers().stream()
                .filter(handler -> handler.getThing().getUID().toString().equals(thingUid)).findAny();

        if (deviceHandler.isPresent()) {
            console.println("Reload channels from '" + thingUid + "'");
            deviceHandler.get().reloadThingChannels();
        } else {
            console.println("Device '" + thingUid + "' not found.");
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(buildCommandUsage(LIST_DEVICES, "list all devices"),
                buildCommandUsage(RELOAD_CHANNELS + " <ThingUID>", "reload all channels from the device "));
    }
}
