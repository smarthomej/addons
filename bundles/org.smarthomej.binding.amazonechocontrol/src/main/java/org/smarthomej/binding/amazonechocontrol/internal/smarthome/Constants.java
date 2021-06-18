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
package org.smarthomej.binding.amazonechocontrol.internal.smarthome;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.smarthomej.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants;
import org.smarthomej.binding.amazonechocontrol.internal.handler.SmartHomeDeviceHandler;

/**
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class Constants {
    public static final Map<String, Function<SmartHomeDeviceHandler, HandlerBase>> HANDLER_FACTORY = Map.ofEntries(
            Map.entry(HandlerPowerController.INTERFACE, HandlerPowerController::new),
            Map.entry(HandlerBrightnessController.INTERFACE, HandlerBrightnessController::new),
            Map.entry(HandlerColorController.INTERFACE, HandlerColorController::new),
            Map.entry(HandlerColorTemperatureController.INTERFACE, HandlerColorTemperatureController::new),
            Map.entry(HandlerSecurityPanelController.INTERFACE, HandlerSecurityPanelController::new),
            Map.entry(HandlerAcousticEventSensor.INTERFACE, HandlerAcousticEventSensor::new),
            Map.entry(HandlerTemperatureSensor.INTERFACE, HandlerTemperatureSensor::new),
            Map.entry(HandlerThermostatController.INTERFACE, HandlerThermostatController::new),
            Map.entry(HandlerPercentageController.INTERFACE, HandlerPercentageController::new),
            Map.entry(HandlerPowerLevelController.INTERFACE, HandlerPowerLevelController::new));

    public static final Set<String> SUPPORTED_INTERFACES = HANDLER_FACTORY.keySet();

    // channel types
    public static final ChannelTypeUID CHANNEL_TYPE_TEMPERATURE = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "temperature");
    public static final ChannelTypeUID CHANNEL_TYPE_TARGETSETPOINT = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "targetSetpoint");

    // List of Item types
    public static final String ITEM_TYPE_SWITCH = "Switch";
    public static final String ITEM_TYPE_DIMMER = "Dimmer";
    public static final String ITEM_TYPE_STRING = "String";
    public static final String ITEM_TYPE_NUMBER = "Number";
    public static final String ITEM_TYPE_NUMBER_TEMPERATURE = "Number:Temperature";
    public static final String ITEM_TYPE_CONTACT = "Contact";
    public static final String ITEM_TYPE_COLOR = "Color";
}
