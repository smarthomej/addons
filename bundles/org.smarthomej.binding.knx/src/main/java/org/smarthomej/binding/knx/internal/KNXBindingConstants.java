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
package org.smarthomej.binding.knx.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link KNXBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class KNXBindingConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger(KNXBindingConstants.class);

    public static final String BINDING_ID = "knx";

    // Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_IP_BRIDGE = new ThingTypeUID(BINDING_ID, "ip");
    public static final ThingTypeUID THING_TYPE_SERIAL_BRIDGE = new ThingTypeUID(BINDING_ID, "serial");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");

    // Property IDs
    public static final String FIRMWARE_TYPE = "firmwaretype";
    public static final String FIRMWARE_VERSION = "firmwareversion";
    public static final String FIRMWARE_SUBVERSION = "firmwaresubversion";
    public static final String MANUFACTURER_NAME = "manfacturername";
    public static final String MANUFACTURER_SERIAL_NO = "manfacturerserialnumber";
    public static final String MANUFACTURER_HARDWARE_TYPE = "manfacturerhardwaretype";
    public static final String MANUFACTURER_FIRMWARE_REVISION = "manfacturerfirmwarerevision";

    // Thing Configuration parameters
    public static final String IP_ADDRESS = "ipAddress";
    public static final String IP_CONNECTION_TYPE = "type";
    public static final String LOCAL_IP = "localIp";
    public static final String LOCAL_SOURCE_ADDRESS = "localSourceAddr";
    public static final String PORT_NUMBER = "portNumber";
    public static final String SERIAL_PORT = "serialPort";

    // The default multicast ip address (see <a
    // href="http://www.iana.org/assignments/multicast-addresses/multicast-addresses.xml">iana</a> EIBnet/IP
    public static final String DEFAULT_MULTICAST_IP = "224.0.23.12";

    // Channel Type IDs
    public static final String CHANNEL_COLOR = "color";
    public static final String CHANNEL_COLOR_CONTROL = "color-control";
    public static final String CHANNEL_CONTACT = "contact";
    public static final String CHANNEL_CONTACT_CONTROL = "contact-control";
    public static final String CHANNEL_DATETIME = "datetime";
    public static final String CHANNEL_DATETIME_CONTROL = "datetime-control";
    public static final String CHANNEL_DIMMER = "dimmer";
    public static final String CHANNEL_DIMMER_CONTROL = "dimmer-control";
    public static final String CHANNEL_NUMBER = "number";
    public static final String CHANNEL_NUMBER_CONTROL = "number-control";
    public static final String CHANNEL_ROLLERSHUTTER = "rollershutter";
    public static final String CHANNEL_ROLLERSHUTTER_CONTROL = "rollershutter-control";
    public static final String CHANNEL_STRING = "string";
    public static final String CHANNEL_STRING_CONTROL = "string-control";
    public static final String CHANNEL_SWITCH = "switch";
    public static final String CHANNEL_SWITCH_CONTROL = "switch-control";

    public static final Set<String> CONTROL_CHANNEL_TYPES = Set.of( //
            CHANNEL_COLOR_CONTROL, //
            CHANNEL_CONTACT_CONTROL, //
            CHANNEL_DATETIME_CONTROL, //
            CHANNEL_DIMMER_CONTROL, //
            CHANNEL_NUMBER_CONTROL, //
            CHANNEL_ROLLERSHUTTER_CONTROL, //
            CHANNEL_STRING_CONTROL, //
            CHANNEL_SWITCH_CONTROL //
    );

    public static final String CHANNEL_RESET = "reset";

    // Channel Configuration parameters
    public static final String GA = "ga";
    public static final String HSB_GA = "hsb";
    public static final String INCREASE_DECREASE_GA = "increaseDecrease";
    public static final String POSITION_GA = "position";
    public static final String REPEAT_FREQUENCY = "frequency";
    public static final String STOP_MOVE_GA = "stopMove";
    public static final String SWITCH_GA = "switch";
    public static final String UP_DOWN_GA = "upDown";

    public static final Map<Integer, String> MANUFACTURER_MAP = readPropertiesFile("manufacturer.properties").entrySet()
            .stream().collect(Collectors.toMap(e -> Integer.parseInt(e.getKey()), Map.Entry::getValue));
    public static final Map<Integer, String> FIRMWARE_MAP = readPropertiesFile("firmware.properties").entrySet()
            .stream().collect(Collectors.toMap(e -> Integer.parseInt(e.getKey()), Map.Entry::getValue));

    public static Map<String, String> readPropertiesFile(String filename) {
        InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
        if (resource == null) {
            LOGGER.warn("Could not read resource file '{}', binding will probably fail: resource is null", filename);
            return Map.of();
        }

        try {
            Properties properties = new Properties();
            properties.load(resource);
            return properties.entrySet().stream()
                    .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
        } catch (IOException e) {
            LOGGER.warn("Could not read resource file '{}', binding will probably fail: {}", filename, e.getMessage());
            return Map.of();
        }
    }
}
