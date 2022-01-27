/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
    public static final Map<String, Function<SmartHomeDeviceHandler, InterfaceHandler>> HANDLER_FACTORY = Map.ofEntries(
            Map.entry(HandlerPowerController.INTERFACE, HandlerPowerController::new),
            Map.entry(HandlerBrightnessController.INTERFACE, HandlerBrightnessController::new),
            Map.entry(HandlerColorController.INTERFACE, HandlerColorController::new),
            Map.entry(HandlerColorTemperatureController.INTERFACE, HandlerColorTemperatureController::new),
            Map.entry(HandlerSecurityPanelController.INTERFACE, HandlerSecurityPanelController::new),
            Map.entry(HandlerAcousticEventSensor.INTERFACE, HandlerAcousticEventSensor::new),
            Map.entry(HandlerTemperatureSensor.INTERFACE, HandlerTemperatureSensor::new),
            Map.entry(HandlerThermostatController.INTERFACE, HandlerThermostatController::new),
            Map.entry(HandlerPercentageController.INTERFACE, HandlerPercentageController::new),
            Map.entry(HandlerPowerLevelController.INTERFACE, HandlerPowerLevelController::new),
            Map.entry(HandlerRangeController.INTERFACE, HandlerRangeController::new),
            Map.entry(HandlerMotionSensor.INTERFACE, HandlerMotionSensor::new),
            Map.entry(HandlerContactSensor.INTERFACE, HandlerContactSensor::new));

    public static final Set<String> SUPPORTED_INTERFACES = HANDLER_FACTORY.keySet();

    // channel types
    public static final ChannelTypeUID CHANNEL_TYPE_TEMPERATURE = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "temperature");
    public static final ChannelTypeUID CHANNEL_TYPE_TARGETSETPOINT = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "targetSetpoint");
    public static final ChannelTypeUID CHANNEL_TYPE_LOWERSETPOINT = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "lowerSetpoint");
    public static final ChannelTypeUID CHANNEL_TYPE_UPPERSETPOINT = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "upperSetpoint");
    public static final ChannelTypeUID CHANNEL_TYPE_THERMOSTATMODE = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "thermostatMode");
    public static final ChannelTypeUID CHANNEL_TYPE_AIR_QUALITY_INDOOR_AIR_QUALITY = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "indoorAirQuality");
    public static final ChannelTypeUID CHANNEL_TYPE_AIR_QUALITY_HUMIDITY = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "humidity");
    public static final ChannelTypeUID CHANNEL_TYPE_AIR_QUALITY_PM25 = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "pm25");
    public static final ChannelTypeUID CHANNEL_TYPE_AIR_QUALITY_CARBON_MONOXIDE = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "carbonMonoxide");
    public static final ChannelTypeUID CHANNEL_TYPE_AIR_QUALITY_VOC = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "voc");
    public static final ChannelTypeUID CHANNEL_TYPE_POWER_STATE = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "powerState");
    public static final ChannelTypeUID CHANNEL_TYPE_UID_ACOUSTIC_EVENT_DETECTION = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "acousticEventDetectionState");
    public static final ChannelTypeUID CHANNEL_TYPE_BRIGHTNESS = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "brightness");
    public static final ChannelTypeUID CHANNEL_TYPE_COLOR_NAME = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "colorName");
    public static final ChannelTypeUID CHANNEL_TYPE_COLOR = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "color");
    public static final ChannelTypeUID CHANNEL_TYPE_COLOR_TEMPERATURE_NAME = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "colorTemperatureName");
    public static final ChannelTypeUID CHANNEL_TYPE_COLOR_TEMPERATURE_IN_KELVIN = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "colorTemperatureInKelvin");
    public static final ChannelTypeUID CHANNEL_TYPE_PERCENTAGE = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "percentage");
    public static final ChannelTypeUID CHANNEL_TYPE_POWER_LEVEL = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "powerLevel");
    public static final ChannelTypeUID CHANNEL_TYPE_ARM_STATE = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "armState");
    public static final ChannelTypeUID CHANNEL_TYPE_BURGLARY_ALARM = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "burglaryAlarm");
    public static final ChannelTypeUID CHANNEL_TYPE_CARBON_MONOXIDE_ALARM = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "carbonMonoxideAlarm");
    public static final ChannelTypeUID CHANNEL_TYPE_FIRE_ALARM = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "fireAlarm");
    public static final ChannelTypeUID CHANNEL_TYPE_WATER_ALARM = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "waterAlarm");
    public static final ChannelTypeUID CHANNEL_TYPE_MOTION_DETECTED = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "motionDetected");
    public static final ChannelTypeUID CHANNEL_TYPE_CONTACT_STATUS = new ChannelTypeUID(
            AmazonEchoControlBindingConstants.BINDING_ID, "contact");
}
