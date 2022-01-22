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
package org.smarthomej.binding.amazonechocontrol.internal;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link AmazonEchoControlBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class AmazonEchoControlBindingConstants {
    public static final String BINDING_ID = "amazonechocontrol";
    public static final String BINDING_NAME = "Amazon Echo Control";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");
    public static final ThingTypeUID THING_TYPE_ECHO = new ThingTypeUID(BINDING_ID, "echo");
    public static final ThingTypeUID THING_TYPE_ECHO_SPOT = new ThingTypeUID(BINDING_ID, "echospot");
    public static final ThingTypeUID THING_TYPE_ECHO_SHOW = new ThingTypeUID(BINDING_ID, "echoshow");
    public static final ThingTypeUID THING_TYPE_ECHO_WHA = new ThingTypeUID(BINDING_ID, "wha");

    public static final ThingTypeUID THING_TYPE_FLASH_BRIEFING_PROFILE = new ThingTypeUID(BINDING_ID,
            "flashbriefingprofile");

    public static final ThingTypeUID THING_TYPE_SMART_HOME_DEVICE = new ThingTypeUID(BINDING_ID, "smartHomeDevice");
    public static final ThingTypeUID THING_TYPE_SMART_HOME_DEVICE_GROUP = new ThingTypeUID(BINDING_ID,
            "smartHomeDeviceGroup");

    public static final Set<ThingTypeUID> SUPPORTED_ECHO_THING_TYPES_UIDS = Set.of(THING_TYPE_ACCOUNT, THING_TYPE_ECHO,
            THING_TYPE_ECHO_SPOT, THING_TYPE_ECHO_SHOW, THING_TYPE_ECHO_WHA, THING_TYPE_FLASH_BRIEFING_PROFILE);

    public static final Set<ThingTypeUID> SUPPORTED_SMART_HOME_THING_TYPES_UIDS = Set.of(THING_TYPE_SMART_HOME_DEVICE,
            THING_TYPE_SMART_HOME_DEVICE_GROUP);

    // List of all Channel ids
    public static final String CHANNEL_PLAYER = "player";
    public static final String CHANNEL_VOLUME = "volume";
    public static final String CHANNEL_EQUALIZER_TREBLE = "equalizerTreble";
    public static final String CHANNEL_EQUALIZER_MIDRANGE = "equalizerMidrange";
    public static final String CHANNEL_EQUALIZER_BASS = "equalizerBass";
    public static final String CHANNEL_ERROR = "error";
    public static final String CHANNEL_SHUFFLE = "shuffle";
    public static final String CHANNEL_LOOP = "loop";
    public static final String CHANNEL_IMAGE_URL = "imageUrl";
    public static final String CHANNEL_TITLE = "title";
    public static final String CHANNEL_SUBTITLE1 = "subtitle1";
    public static final String CHANNEL_SUBTITLE2 = "subtitle2";
    public static final String CHANNEL_PROVIDER_DISPLAY_NAME = "providerDisplayName";
    public static final String CHANNEL_BLUETOOTH_MAC = "bluetoothMAC";
    public static final String CHANNEL_BLUETOOTH = "bluetooth";
    public static final String CHANNEL_BLUETOOTH_DEVICE_NAME = "bluetoothDeviceName";
    public static final String CHANNEL_RADIO_STATION_ID = "radioStationId";
    public static final String CHANNEL_RADIO = "radio";
    public static final String CHANNEL_AMAZON_MUSIC_TRACK_ID = "amazonMusicTrackId";
    public static final String CHANNEL_AMAZON_MUSIC = "amazonMusic";
    public static final String CHANNEL_AMAZON_MUSIC_PLAY_LIST_ID = "amazonMusicPlayListId";
    public static final String CHANNEL_TEXT_TO_SPEECH = "textToSpeech";
    public static final String CHANNEL_TEXT_TO_SPEECH_VOLUME = "textToSpeechVolume";
    public static final String CHANNEL_TEXT_COMMAND = "textCommand";
    public static final String CHANNEL_REMIND = "remind";
    public static final String CHANNEL_PLAY_ALARM_SOUND = "playAlarmSound";
    public static final String CHANNEL_START_ROUTINE = "startRoutine";
    public static final String CHANNEL_MUSIC_PROVIDER_ID = "musicProviderId";
    public static final String CHANNEL_PLAY_MUSIC_VOICE_COMMAND = "playMusicVoiceCommand";
    public static final String CHANNEL_START_COMMAND = "startCommand";
    public static final String CHANNEL_LAST_VOICE_COMMAND = "lastVoiceCommand";
    public static final String CHANNEL_LAST_SPOKEN_TEXT = "lastSpokenText";
    public static final String CHANNEL_MEDIA_PROGRESS = "mediaProgress";
    public static final String CHANNEL_MEDIA_LENGTH = "mediaLength";
    public static final String CHANNEL_MEDIA_PROGRESS_TIME = "mediaProgressTime";
    public static final String CHANNEL_ASCENDING_ALARM = "ascendingAlarm";
    public static final String CHANNEL_NOTIFICATION_VOLUME = "notificationVolume";
    public static final String CHANNEL_NEXT_REMINDER = "nextReminder";
    public static final String CHANNEL_NEXT_ALARM = "nextAlarm";
    public static final String CHANNEL_NEXT_MUSIC_ALARM = "nextMusicAlarm";
    public static final String CHANNEL_NEXT_TIMER = "nextTimer";

    public static final String CHANNEL_SAVE = "save";
    public static final String CHANNEL_ACTIVE = "active";
    public static final String CHANNEL_PLAY_ON_DEVICE = "playOnDevice";

    // List of all Properties
    public static final String DEVICE_PROPERTY_SERIAL_NUMBER = "serialNumber";
    public static final String DEVICE_PROPERTY_FAMILY = "deviceFamily";
    public static final String DEVICE_PROPERTY_DEVICE_TYPE_ID = "deviceTypeId";
    public static final String DEVICE_PROPERTY_MANUFACTURER_NAME = "manufacturerName";
    public static final String DEVICE_PROPERTY_DEVICE_IDENTIFIER_LIST = "deviceIdentifierList";
    public static final String DEVICE_PROPERTY_FLASH_BRIEFING_PROFILE = "configurationJson";
    public static final String DEVICE_PROPERTY_ID = "id";

    // Other
    public static final String FLASH_BRIEFING_COMMAND_PREFIX = "FlashBriefing.";

    // DeviceTypeIds to human readable description
    // originally found here: https://github.com/Apollon77/ioBroker.alexa2/blob/master/main.js
    public static final Map<String, String> DEVICE_TYPES = Map.<String, String> ofEntries( //
            Map.entry("A10A33FOX2NUBK", "Echo Spot"), //
            Map.entry("A10L5JEZTKKCZ8", "Vobot-Clock"), //
            Map.entry("A12GXV8XMS007S", "FireTV"), //
            Map.entry("A15ERDAKK5HQQG", "Sonos"), //
            Map.entry("A17LGWINFBUTZZ", "Anker Roav Viva Alexa"), //
            Map.entry("A18O6U1UQFJ0XK", "Echo Plus 2nd Gen"), //
            Map.entry("A1C66CX2XD756O", "Fire HD 8"), //
            Map.entry("A1DL2DVDQVK3Q", "Apps"), //
            Map.entry("A1ETW4IXK2PYBP", "Echo Auto"), //
            Map.entry("A1H0CMF1XM0ZP4", "Echo Dot/Bose"), //
            Map.entry("A1J16TEDOYCZTN", "Fire Tab"), //
            Map.entry("A1JJ0KFC4ZPNJ3", "Echo Input"), //
            Map.entry("A1NL4BVLQ4L3N3", "Echo Show"), //
            Map.entry("A1P31Q3MOWSHOD", "Anker Zalo Halo Speaker"), //
            Map.entry("A1Q7QCGNMXAKYW", "Fire Tab 7"), //
            Map.entry("A1QKZ9D0IJY332", "Samsung QLED"), //
            Map.entry("A1RABVCI4QCIKC", "Echo Dot 3rd Gen"), //
            Map.entry("A1RTAM01W29CUP", "Windows App"), //
            Map.entry("A1X7HJX9QL16M5", "Bespoken.io"), //
            Map.entry("A1Z88NGR2BK6A2", "Echo Show 8"), //
            Map.entry("A1ZB65LA390I4K", "Fire HD 10"), //
            Map.entry("A21Z3CGI8UIP0F", "Apps"), //
            Map.entry("A265XOI9586NML", "FireTV Stick v3"), //
            Map.entry("A2825NDLA7WDZV", "Apps"), //
            Map.entry("A2E0SNTXJVT7WK", "FireTV V1"), //
            Map.entry("A2GFL5ZMWNE0PX", "FireTV"), //
            Map.entry("A2H4LV5GIZ1JFT", "Echo 4 Clock"), //
            Map.entry("A2IVLV5VM2W81", "Apps"), //
            Map.entry("A2J0R2SD7G9LPA", "Tablet"), //
            Map.entry("A2JKHJ0PX4J3L3", "FireTV Cube"), //
            Map.entry("A2L8KG0CT86ADW", "RaspPi"), //
            Map.entry("A2LWARUGJLBYEW", "FireTV Stick V2"), //
            Map.entry("A2M35JJZWCQOMZ", "Echo Plus"), //
            Map.entry("A2M4YX06LWP8WI", "Fire Tab"), //
            Map.entry("A2OSP3UA4VC85F", "Sonos"), //
            Map.entry("A2T0P32DY3F7VB", "echosim.io"), //
            Map.entry("A2TF17PFR55MTB", "Apps"), //
            Map.entry("A2U21SRK4QGSE1", "Echo Dot 4th Gen"), //
            Map.entry("A2Z8O30CD35N8F", "Sonos Arc"), //
            Map.entry("A303PJF6ISQ7IC", "Echo Auto"), //
            Map.entry("A30YDR2MK8HMRV", "Echo Dot 3rd Gen Clock"), //
            Map.entry("A31DTMEEVDDOIV", "FireTV Stick Lite 2020"), //
            Map.entry("A32DOYMUN6DTXA", "Echo Dot 3rd Gen"), //
            Map.entry("A378ND93PD0NC4", "VR Radio"), //
            Map.entry("A37SHHQ3NUL7B5", "Bose Homespeaker"), //
            Map.entry("A38BPK7OW001EX", "Raspberry Alexa"), //
            Map.entry("A38EHHIB10L47V", "Echo Dot"), //
            Map.entry("A39Y3UG1XLEJLZ", "Fitbit Sense"), //
            Map.entry("A3C9PE6TNYLTCH", "Multiroom"), //
            Map.entry("A3FX4UWTP28V1P", "Echo 3"), //
            Map.entry("A3GZUE7F9MEB4U", "FireTV Cube"), //
            Map.entry("A3H674413M2EKB", "echosim.io"), //
            Map.entry("A3HF4YRA2L7XGC", "FireTV Cube"), //
            Map.entry("A3NPD82ABCPIDP", "Sonos Beam"), //
            Map.entry("A3R8XIAIU4HJAX", "Echo Show"), //
            Map.entry("A3R9S4ZZECZ6YL", "Fire Tab HD 10"), //
            Map.entry("A3RBAYBE7VM004", "Echo Studio"), //
            Map.entry("A3RMGO6LYLH7YN", "Echo 4 Bridge"), //
            Map.entry("A3S5BH2HU6VAYF", "Echo Dot 2nd Gen"), //
            Map.entry("A3SSG6GR8UU7SN", "Echo Sub"), //
            Map.entry("A3TCJ8RTT3NVI7", "Listens for Alexa"), //
            Map.entry("A3V3VA38K169FO", "Fire Tab"), //
            Map.entry("A3VRME03NAXFUB", "Echo Flex"), //
            Map.entry("A4ZP7ZC4PI6TO", "Echo Show 5th Gen"), //
            Map.entry("A7WXQPH584YP", "Echo 2nd Gen"), //
            Map.entry("A8DM4FYR6D3HT", "LG WebOS TV"), //
            Map.entry("AB72C64C86AW2", "Echo"), //
            Map.entry("ADVBD696BHNV5", "FireTV Stick V1"), //
            Map.entry("AILBSA2LNTOYL", "reverb App"), //
            Map.entry("AINRG27IL8AS0", "Megablast Speaker"), //
            Map.entry("AKOAGQTKAS9YB", "Echo Connect"), //
            Map.entry("AKPGW064GI9HE", "FireTV Stick 4K"), //
            Map.entry("AP1F6KUH00XPV", "Stereo/Subwoofer Pair"), //
            Map.entry("AVD3HM0HOJAAL", "Sonos One 2nd Gen"), //
            Map.entry("AVE5HX13UR5NO", "Logitech Zero Touch"), //
            Map.entry("AVU7CPPF2ZRAS", "Fire HD 8"), //
            Map.entry("AWZZ5CVHX2CD", "Echo Show 2nd Gen"));
}
