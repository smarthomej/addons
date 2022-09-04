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
package org.smarthomej.binding.viessmann.internal.handler;

import static org.smarthomej.binding.viessmann.internal.ViessmannBindingConstants.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.viessmann.internal.config.ThingsConfig;
import org.smarthomej.binding.viessmann.internal.dto.HeatingCircuit;
import org.smarthomej.binding.viessmann.internal.dto.ThingMessageDTO;
import org.smarthomej.binding.viessmann.internal.dto.ViessmannMessage;
import org.smarthomej.binding.viessmann.internal.dto.features.FeatureCommands;
import org.smarthomej.binding.viessmann.internal.dto.features.FeatureDataDTO;
import org.smarthomej.binding.viessmann.internal.dto.features.FeatureProperties;
import org.smarthomej.binding.viessmann.internal.dto.schedule.DaySchedule;
import org.smarthomej.binding.viessmann.internal.dto.schedule.ScheduleDTO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link DeviceHandler} is responsible for handling DeviceHandler
 *
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class DeviceHandler extends ViessmannThingHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceHandler.class);

    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    private ThingsConfig config = new ThingsConfig();

    private final Map<String, HeatingCircuit> heatingCircuits = new HashMap<>();

    public DeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        ThingsConfig config = getConfigAs(ThingsConfig.class);
        this.config = config;
        if (config.deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid device id setting");
            return;
        }
        updateProperty(PROPERTY_ID, config.deviceId); // set representation property used by discovery

        setPollingDevice();

        initDeviceState();
        logger.trace("Device handler finished initializing");
    }

    @Override
    public void dispose() {
        unsetPollingDevice();
    }

    @Override
    public void initChannelState() {
        Bridge bridge = getBridge();
        ViessmannBridgeHandler bridgeHandler = bridge == null ? null : (ViessmannBridgeHandler) bridge.getHandler();
        if (bridgeHandler != null) {
            bridgeHandler.getAllFeaturesByDeviceId(config.deviceId);
        }
    }

    private void setPollingDevice() {
        Bridge bridge = getBridge();
        ViessmannBridgeHandler bridgeHandler = bridge == null ? null : (ViessmannBridgeHandler) bridge.getHandler();
        if (bridgeHandler != null) {
            bridgeHandler.setPollingDevice(config.deviceId);
        }
    }

    private void unsetPollingDevice() {
        Bridge bridge = getBridge();
        ViessmannBridgeHandler bridgeHandler = bridge == null ? null : (ViessmannBridgeHandler) bridge.getHandler();
        if (bridgeHandler != null) {
            bridgeHandler.unsetPollingDevice(config.deviceId);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            Channel ch = thing.getChannel(channelUID.getId());
            if (ch != null) {
                logger.trace("ChannelUID: {} | Properties: {}", ch.getUID(), ch.getProperties());
                Boolean initState = false;
                Map<String, String> prop = ch.getProperties();
                String commands = prop.get("command");
                if (commands != null) {
                    String uri = null;
                    String param = null;
                    String com[] = commands.split(",");
                    if (OnOffType.ON.equals(command)) {
                        uri = prop.get("activateUri");
                        param = "{}";
                    } else if (OnOffType.OFF.equals(command)) {
                        uri = prop.get("deactivateUri");
                        param = "{}";
                    } else if (command instanceof DecimalType) {
                        logger.trace("Received DecimalType Command for Channel {}",
                                thing.getChannel(channelUID.getId()));
                        for (String str : com) {
                            if (str.contains("setCurve")) {
                                uri = prop.get(str + "Uri");
                                String circuitId = prop.get("circuitId");
                                HeatingCircuit heatingCircuit = heatingCircuits.get(circuitId);
                                if (heatingCircuit != null) {
                                    String slope = heatingCircuit.getSlope();
                                    String shift = heatingCircuit.getShift();
                                    String value = command.toString();
                                    if (ch.getUID().toString().contains("shift")) {
                                        param = "{\"slope\":" + slope + ", \"shift\":" + value + "}";
                                        heatingCircuit.setShift(value);
                                    } else if (ch.getUID().toString().contains("slope")) {
                                        param = "{\"slope\":" + value + ", \"shift\":" + shift + "}";
                                        heatingCircuit.setSlope(value);
                                    }
                                    if (circuitId != null) {
                                        heatingCircuits.put(circuitId, heatingCircuit);
                                    }
                                }
                            }
                        }
                    } else if (command instanceof QuantityType<?>) {
                        QuantityType<?> value = (QuantityType<?>) command;
                        double f = value.doubleValue();
                        for (String str : com) {
                            if (str.contains("Temperature") || str.contains("setHysteresis") || str.contains("setMin")
                                    || str.contains("setMax") || str.contains("temperature")) {
                                uri = prop.get(str + "Uri");
                                param = "{\"" + prop.get(str + "Params") + "\":" + f + "}";
                                break;
                            }
                        }
                        logger.trace("Received QuantityType Command for Channel {} Command: {}",
                                thing.getChannel(channelUID.getId()), value.floatValue());
                    } else if (command instanceof StringType) {
                        for (String str : com) {
                            String s = command.toString();
                            uri = prop.get(str + "Uri");
                            if ("{".equals(s.substring(0, 1))) {
                                param = "{\"" + prop.get(str + "Params") + "\":" + s + "}";
                            } else {
                                param = "{\"" + prop.get(str + "Params") + "\":\"" + s + "\"}";
                            }
                            break;
                        }
                        logger.trace("Received StringType Command for Channel {}",
                                thing.getChannel(channelUID.getId()));
                    }
                    if (uri != null && param != null) {
                        Bridge bridge = getBridge();
                        ViessmannBridgeHandler bridgeHandler = bridge == null ? null
                                : (ViessmannBridgeHandler) bridge.getHandler();
                        if (bridgeHandler != null) {
                            if (!bridgeHandler.setData(uri, param) || initState) {
                                initChannelState();
                            }
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            logger.warn("handleCommand fails", e);
        }
    }

    @Override
    public void handleUpdateChannel(ViessmannMessage msg) {
        logger.trace("handleUpdateChannel: {}", msg);
    }

    @Override
    public void handleUpdate(FeatureDataDTO featureDataDTO) {
        logger.trace("Device handler received update: {}", featureDataDTO);
        ThingMessageDTO msg = new ThingMessageDTO();
        if (featureDataDTO.properties != null) {
            msg.setDeviceId(featureDataDTO.deviceId);
            msg.setFeatureClear(featureDataDTO.feature);
            msg.setFeatureDescription(getFeatureDescription(featureDataDTO.feature));
            FeatureCommands commands = featureDataDTO.commands;
            if (commands != null) {
                msg.setCommands(commands);
            }
            FeatureProperties prop = featureDataDTO.properties;
            List<String> entr = prop.getUsedEntries();
            if (!entr.isEmpty()) {
                for (String entry : entr) {
                    String valueEntry = "";
                    String typeEntry = "";
                    Boolean bool = false;
                    String viUnit = "";
                    String unit = null;
                    HeatingCircuit heatingCircuit = new HeatingCircuit();
                    msg.setFeatureName(getFeatureName(featureDataDTO.feature));
                    msg.setSuffix(entry);
                    switch (entry) {
                        case "value":
                            viUnit = prop.value.unit;
                            if ("celsius".equals(viUnit)) {
                                typeEntry = "temperature";
                            } else if ("percent".equals(viUnit) || "minute".equals(viUnit) || "kelvin".equals(viUnit)
                                    || "liter".equals(viUnit)) {
                                typeEntry = viUnit;
                            } else {
                                typeEntry = prop.value.type;
                            }
                            valueEntry = prop.value.value;
                            break;
                        case "status":
                            typeEntry = prop.status.type;
                            valueEntry = prop.status.value;
                            if ("off".equals(valueEntry)) {
                                typeEntry = "boolean";
                                bool = false;
                            } else if ("on".equals(valueEntry)) {
                                typeEntry = "boolean";
                                bool = true;
                            }
                            viUnit = "";
                            break;
                        case "active":
                            typeEntry = prop.active.type;
                            valueEntry = prop.active.value ? "true" : "false";
                            bool = prop.active.value;
                            break;
                        case "name":
                            typeEntry = prop.name.type;
                            valueEntry = prop.name.value;
                            break;
                        case "shift":
                            typeEntry = prop.shift.type;
                            valueEntry = prop.shift.value.toString();
                            heatingCircuit.setSlope(prop.slope.value.toString());
                            heatingCircuit.setShift(prop.shift.value.toString());
                            heatingCircuits.put(msg.getCircuitId(), heatingCircuit);
                            break;
                        case "slope":
                            typeEntry = "decimal";
                            valueEntry = prop.slope.value.toString();
                            heatingCircuit.setSlope(prop.slope.value.toString());
                            heatingCircuit.setShift(prop.shift.value.toString());
                            heatingCircuits.put(msg.getCircuitId(), heatingCircuit);
                            break;
                        case "entries":
                            msg.setSuffix("schedule");
                            typeEntry = prop.entries.type.toString();
                            valueEntry = new Gson().toJson(prop.entries.value);
                            break;
                        case "overlapAllowed":
                            typeEntry = prop.overlapAllowed.type;
                            valueEntry = prop.overlapAllowed.value ? "true" : "false";
                            bool = prop.overlapAllowed.value;
                            break;
                        case "temperature":
                            typeEntry = prop.temperature.type;
                            valueEntry = prop.temperature.value.toString();
                            typeEntry = "temperature";
                            viUnit = prop.temperature.unit;
                            break;
                        case "start":
                            typeEntry = prop.start.type;
                            valueEntry = prop.start.value;
                            break;
                        case "end":
                            typeEntry = prop.end.type;
                            valueEntry = prop.end.value;
                            break;
                        case "top":
                            typeEntry = prop.top.type;
                            valueEntry = prop.top.value.toString();
                            break;
                        case "middle":
                            typeEntry = prop.middle.type;
                            valueEntry = prop.middle.value.toString();
                            break;
                        case "bottom":
                            typeEntry = prop.bottom.type;
                            valueEntry = prop.bottom.value.toString();
                            break;
                        case "day":
                            // returns array as string
                            typeEntry = prop.day.type;
                            valueEntry = prop.day.value.toString();
                            viUnit = prop.day.unit;
                            break;
                        case "week":
                            // returns array as string
                            typeEntry = prop.week.type;
                            valueEntry = prop.week.value.toString();
                            viUnit = prop.week.unit;
                            break;
                        case "month":
                            // returns array as string
                            typeEntry = prop.month.type;
                            valueEntry = prop.month.value.toString();
                            viUnit = prop.month.unit;
                            break;
                        case "year":
                            // returns array as string
                            typeEntry = prop.year.type;
                            valueEntry = prop.year.value.toString();
                            viUnit = prop.year.unit;
                            break;
                        case "unit":
                            typeEntry = prop.unit.type;
                            valueEntry = prop.unit.value;
                            break;
                        case "starts":
                            typeEntry = prop.starts.type;
                            valueEntry = prop.starts.value.toString();
                            viUnit = prop.starts.unit;
                            break;
                        case "hours":
                            typeEntry = entry;
                            valueEntry = prop.hours.value.toString();
                            viUnit = "hour";
                            break;
                        case "hoursLoadClassOne":
                            typeEntry = "hours";
                            valueEntry = prop.hoursLoadClassOne.value.toString();
                            viUnit = "hour";
                            break;
                        case "hoursLoadClassTwo":
                            typeEntry = "hours";
                            valueEntry = prop.hoursLoadClassTwo.value.toString();
                            viUnit = "hour";
                            break;
                        case "hoursLoadClassThree":
                            typeEntry = "hours";
                            valueEntry = prop.hoursLoadClassThree.value.toString();
                            viUnit = "hour";
                            break;
                        case "hoursLoadClassFour":
                            typeEntry = "hours";
                            valueEntry = prop.hoursLoadClassFour.value.toString();
                            viUnit = "hour";
                            break;
                        case "hoursLoadClassFive":
                            typeEntry = "hours";
                            valueEntry = prop.hoursLoadClassFive.value.toString();
                            viUnit = "hour";
                            break;
                        case "min":
                            typeEntry = prop.min.type;
                            valueEntry = prop.min.value.toString();
                            viUnit = prop.min.unit;
                            break;
                        case "max":
                            typeEntry = prop.max.type;
                            valueEntry = prop.max.value.toString();
                            viUnit = prop.max.unit;
                            break;
                        default:
                            break;
                    }
                    msg.setType(typeEntry);
                    msg.setValue(valueEntry);
                    msg.setChannelType("type-" + typeEntry);

                    if (msg.getDeviceId().contains(config.deviceId) && !"unit".equals(entry)) {
                        if (!"[]".equals(valueEntry)) {
                            logger.trace("Feature: {} Type:{} Entry: {}={}", featureDataDTO.feature, typeEntry, entry,
                                    valueEntry);

                            if (viUnit != null) {
                                if (!viUnit.isEmpty()) {
                                    msg.setUnit(viUnit);
                                    unit = UNIT_MAP.get(viUnit);
                                    if (unit == null) {
                                        logger.warn(
                                                "Unknown unit. Could not parse unit: {} of Feature: {} - Please open an issue on GitHub.",
                                                viUnit, featureDataDTO.feature);
                                    }
                                }
                            }

                            if (thing.getChannel(msg.getChannelId()) == null) {
                                createChannel(msg);
                            }

                            ThingMessageDTO subMsg = new ThingMessageDTO();
                            subMsg.setDeviceId(featureDataDTO.deviceId);
                            subMsg.setFeatureClear(featureDataDTO.feature);
                            subMsg.setFeatureDescription(getFeatureDescription(featureDataDTO.feature));
                            subMsg.setFeatureName(getFeatureName(featureDataDTO.feature));
                            subMsg.setType(typeEntry);
                            subMsg.setValue(valueEntry);
                            switch (entry) {
                                case "entries":
                                    subMsg.setSuffix("produced");
                                    subMsg.setChannelType("type-boolean");
                                    createSubChannel(subMsg);
                                    break;
                                case "day":
                                    subMsg.setSuffix("today");
                                    subMsg.setChannelType("type-energy");
                                    createSubChannel(subMsg);
                                    subMsg.setSuffix("yesterday");
                                    createSubChannel(subMsg);
                                    break;
                                case "week":
                                    subMsg.setSuffix("thisWeek");
                                    subMsg.setChannelType("type-energy");
                                    createSubChannel(subMsg);
                                    subMsg.setSuffix("lastWeek");
                                    createSubChannel(subMsg);
                                    break;
                                case "month":
                                    subMsg.setSuffix("thisMonth");
                                    subMsg.setChannelType("type-energy");
                                    createSubChannel(subMsg);
                                    subMsg.setSuffix("lastMonth");
                                    createSubChannel(subMsg);
                                    break;
                                case "year":
                                    subMsg.setSuffix("thisYear");
                                    subMsg.setChannelType("type-energy");
                                    createSubChannel(subMsg);
                                    subMsg.setSuffix("lastYear");
                                    createSubChannel(subMsg);
                                    break;
                                default:
                                    break;
                            }

                            switch (typeEntry) {
                                case "decimal":
                                case "number":
                                case "temperature":
                                case "percent":
                                case "minute":
                                case "hours":
                                case "kelvin":
                                case "liter":
                                    updateChannelState(msg.getChannelId(), msg.getValue(), unit);
                                    break;
                                case "boolean":
                                    OnOffType state = bool ? OnOffType.ON : OnOffType.OFF;
                                    updateState(msg.getChannelId(), state);
                                    break;
                                case "string":
                                case "array":
                                    updateState(msg.getChannelId(), StringType.valueOf(msg.getValue()));

                                    String parts[] = msg.getValue().replace("[", "").replace("]", "").replace(" ", "")
                                            .split(",");
                                    if (parts.length > 1) {
                                        switch (entry) {
                                            case "day":
                                                subMsg.setSuffix("today");
                                                subMsg.setChannelType("type-energy");
                                                updateChannelState(subMsg.getChannelId(), parts[0], unit);
                                                subMsg.setSuffix("yesterday");
                                                updateChannelState(subMsg.getChannelId(), parts[1], unit);
                                                break;
                                            case "week":
                                                subMsg.setSuffix("thisWeek");
                                                subMsg.setChannelType("type-energy");
                                                updateChannelState(subMsg.getChannelId(), parts[0], unit);
                                                subMsg.setSuffix("lastWeek");
                                                updateChannelState(subMsg.getChannelId(), parts[1], unit);
                                                break;
                                            case "month":
                                                subMsg.setSuffix("thisMonth");
                                                subMsg.setChannelType("type-number");
                                                updateChannelState(subMsg.getChannelId(), parts[0], unit);
                                                subMsg.setSuffix("lastMonth");
                                                updateChannelState(subMsg.getChannelId(), parts[1], unit);
                                                break;
                                            case "year":
                                                subMsg.setSuffix("thisYear");
                                                subMsg.setChannelType("type-number");
                                                updateChannelState(subMsg.getChannelId(), parts[0], unit);
                                                subMsg.setSuffix("lastYear");
                                                updateChannelState(subMsg.getChannelId(), parts[1], unit);
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                    break;
                                case "Schedule":
                                    updateState(msg.getChannelId(), StringType.valueOf(msg.getValue()));
                                    String channelId = msg.getChannelId().replace("#schedule", "#produced");
                                    updateState(channelId, parseSchedule(msg.getValue()));
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates new channels for the thing.
     *
     * @param msg contains everything is needed of the channel to be created.
     */
    private void createChannel(ThingMessageDTO msg) {
        ChannelUID channelUID = new ChannelUID(thing.getUID(), msg.getChannelId());
        ThingHandlerCallback callback = getCallback();
        if (callback == null) {
            logger.warn("Thing '{}'not initialized, could not get callback.", thing.getUID());
            return;
        }

        Map<String, String> prop = buildProperties(msg);
        String channelType = convertChannelType(msg);

        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, channelType);
        if (msg.getFeatureName().contains("active")) {
            logger.trace("Feature: {} ChannelType: {}", msg.getFeatureClear(), channelType);
        }
        Channel channel = callback.createChannelBuilder(channelUID, channelTypeUID).withLabel(msg.getFeatureName())
                .withDescription(msg.getFeatureDescription()).withProperties(prop).build();
        updateThing(editThing().withoutChannel(channelUID).withChannel(channel).build());
    }

    /**
     * Creates new sub channels for the thing.
     *
     * @param msg contains everything is needed of the channel to be created.
     */
    private void createSubChannel(ThingMessageDTO msg) {
        if (thing.getChannel(msg.getChannelId()) == null) {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), msg.getChannelId());
            ThingHandlerCallback callback = getCallback();
            if (callback == null) {
                logger.warn("Thing '{}'not initialized, could not get callback.", thing.getUID());
                return;
            }

            Map<String, String> prop = new HashMap<>();
            prop.put("feature", msg.getFeatureClear());
            String channelType = msg.getChannelType();

            ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, channelType);
            if (msg.getFeatureName().contains("active")) {
                logger.trace("Feature: {} ChannelType: {}", msg.getFeatureClear(), channelType);
            }
            Channel channel = callback.createChannelBuilder(channelUID, channelTypeUID).withLabel(msg.getFeatureName())
                    .withDescription(msg.getFeatureDescription()).withProperties(prop).build();
            updateThing(editThing().withoutChannel(channelUID).withChannel(channel).build());
        }
    }

    private String getFeatureName(String feature) {
        Pattern pattern = Pattern.compile("(\\.[0-3])");
        Matcher matcher = pattern.matcher(feature);
        if (matcher.find()) {
            String circuit = matcher.group(0);
            feature = matcher.replaceAll(".N");
            String name = FEATURES_MAP.getOrDefault(feature, feature) + " (Circuit: " + circuit.replace(".", "") + ")";
            return name;
        }
        return FEATURES_MAP.getOrDefault(feature, feature);
    }

    private @Nullable String getFeatureDescription(String feature) {
        feature.replaceAll("\\.[0-3]", ".N");
        return FEATURE_DESCRIPTION_MAP.get(feature);
    }

    private OnOffType parseSchedule(String scheduleJson) {
        Calendar now = Calendar.getInstance();

        int hour = now.get(Calendar.HOUR_OF_DAY); // Get hour in 24 hour format
        int minute = now.get(Calendar.MINUTE);

        Date currTime = parseTime(hour + ":" + minute);

        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

        ScheduleDTO schedule = GSON.fromJson(scheduleJson, ScheduleDTO.class);
        if (schedule != null) {
            List<DaySchedule> day = schedule.getMon();
            switch (dayOfWeek) {
                case 2:
                    day = schedule.getMon();
                    break;
                case 3:
                    day = schedule.getTue();
                    break;
                case 4:
                    day = schedule.getWed();
                    break;
                case 5:
                    day = schedule.getThu();
                    break;
                case 6:
                    day = schedule.getFri();
                    break;
                case 7:
                    day = schedule.getSat();
                    break;
                case 1:
                    day = schedule.getSun();
                    break;
                default:
                    break;
            }
            for (DaySchedule daySchedule : day) {
                Date startTime = parseTime(daySchedule.getStart());
                Date endTime = parseTime(daySchedule.getEnd());

                if (startTime.before(currTime) && endTime.after(currTime)) {
                    return OnOffType.ON;
                }
            }
        }
        return OnOffType.OFF;
    }

    private Date parseTime(String time) {
        final String inputFormat = "HH:mm";
        SimpleDateFormat inputParser = new SimpleDateFormat(inputFormat);
        try {
            return inputParser.parse(time);
        } catch (java.text.ParseException e) {
            return new Date(0);
        }
    }

    private void updateChannelState(String channelId, String stateAsString, @Nullable String unit) {
        if (unit != null) {
            updateState(channelId, new QuantityType<>(stateAsString + " " + unit));
        } else {
            DecimalType s = DecimalType.valueOf(stateAsString);
            updateState(channelId, s);
        }
    }

    private Map<String, String> buildProperties(ThingMessageDTO msg) {
        Map<String, String> prop = new HashMap<>();
        prop.put("feature", msg.getFeatureClear());
        prop.put("channelType", convertChannelType(msg));
        FeatureCommands commands = msg.getCommands();
        if (commands != null) {
            List<String> com = commands.getUsedCommands();
            if (!com.isEmpty()) {
                for (String command : com) {
                    prop.put("command", addPropertiesCommands(prop, command));
                    switch (command) {
                        case "setName":
                            prop.put("setNameUri", commands.setName.uri);
                            prop.put("setNameParams", "name");
                            break;
                        case "setCurve":
                            prop.put("circuitId", msg.getCircuitId());
                            prop.put("setCurveUri", commands.setCurve.uri);
                            prop.put("setCurveParams", "slope,shift");
                            break;
                        case "setSchedule":
                            prop.put("setScheduleUri", commands.setSchedule.uri);
                            prop.put("setScheduleParams", "newSchedule");
                            break;
                        case "setMode":
                            prop.put("setModeUri", commands.setMode.uri);
                            prop.put("setModeParams", "mode");
                            break;
                        case "setTemperature":
                            prop.put("setTemperatureUri", commands.setTemperature.uri);
                            prop.put("setTemperatureParams", "targetTemperature");
                            break;
                        case "setTargetTemperature":
                            prop.put("temperatureUri", commands.setTargetTemperature.uri);
                            prop.put("command", "temperature");
                            prop.put("temperatureParams", "temperature");
                            break;
                        case "activate":
                            prop.put("activateUri", commands.activate.uri);
                            prop.put("activateParams", "{}");
                            prop.put("deactivateParams", "{}");
                            break;
                        case "deactivate":
                            prop.put("deactivateUri", commands.deactivate.uri);
                            prop.put("activateParams", "{}");
                            prop.put("deactivateParams", "{}");
                            break;
                        case "changeEndDate":
                            prop.put("changeEndDateUri", commands.changeEndDate.uri);
                            prop.put("command", "changeEndDate,schedule,unschedule");
                            prop.put("changeEndDatepParams", "end");
                            prop.put("scheduleParams", "start,end");
                            prop.put("unscheduleParams", "{}");
                            break;
                        case "schedule":
                            prop.put("scheduleUri", commands.schedule.uri);
                            prop.put("scheduleParams", "start,end");
                            break;
                        case "unschedule":
                            prop.put("unscheduleUri", commands.unschedule.uri);
                            prop.put("unscheduleParams", "{}");
                            break;
                        case "setMin":
                            if (msg.getSuffix().contains("min")) {
                                prop.put("setMinUri", commands.setMin.uri);
                                prop.put("command", "setMin");
                                prop.put("setMinParams", "temperature");
                            }
                            break;
                        case "setMax":
                            if (msg.getSuffix().contains("max")) {
                                prop.put("setMaxUri", commands.setMax.uri);
                                prop.put("command", "setMax");
                                prop.put("setMaxParams", "temperature");
                            }
                            break;
                        case "setHysteresis":
                            prop.put("setHysteresisUri", commands.setHysteresis.uri);
                            prop.put("command", "setHysteresis");
                            prop.put("setHysteresisParams", "hysteresis");
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return prop;
    }

    private String addPropertiesCommands(Map<String, String> properties, String command) {
        String commands = properties.get("command");
        if (commands != null) {
            commands = commands + "," + command;
            return commands;
        }
        return command;
    }

    private String convertChannelType(ThingMessageDTO msg) {
        String channelType = msg.getChannelType();
        FeatureCommands commands = msg.getCommands();
        if (commands != null) {
            List<String> com = commands.getUsedCommands();
            if (!com.isEmpty()) {
                for (String command : com) {
                    switch (command) {
                        case "setTemperature":
                            if (!"type-boolean".equals(channelType)) {
                                channelType = "type-settemperature";
                            }
                            break;
                        case "setTargetTemperature":
                            if (!"type-boolean".equals(channelType)) {
                                channelType = "type-settemperature";
                            }
                            break;
                        case "setMin":
                            if (msg.getSuffix().contains("min")) {
                                channelType = "type-setMin";
                            }
                            break;
                        case "setMax":
                            if (msg.getSuffix().contains("max")) {
                                channelType = "type-setMax";
                            }
                            break;
                        case "setHysteresis":
                            channelType = "type-setTargetHysteresis";
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return channelType;
    }
}
