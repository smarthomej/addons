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
package org.smarthomej.binding.viessmann.internal.handler;

import static org.smarthomej.binding.viessmann.internal.ViessmannBindingConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import org.smarthomej.binding.viessmann.internal.dto.ThingMessageDTO;
import org.smarthomej.binding.viessmann.internal.dto.ViessmannMessage;
import org.smarthomej.binding.viessmann.internal.dto.features.FeatureCommands;
import org.smarthomej.binding.viessmann.internal.dto.features.FeatureDataDTO;
import org.smarthomej.binding.viessmann.internal.dto.features.FeatureProperties;
import org.smarthomej.binding.viessmann.internal.dto.features.ViessmannFeatureMap;

import com.google.gson.Gson;

/**
 * The {@link DeviceHandler} is responsible for handling DeviceHandler
 *
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class DeviceHandler extends ViessmannThingHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceHandler.class);

    private ThingsConfig config = new ThingsConfig();
    private String devId = "";
    private @Nullable Bridge br;

    public DeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(ThingsConfig.class);
        int l = config.deviceId.length();
        if (l == 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid device id setting");
            return;
        }
        updateProperty(PROPERTY_ID, String.valueOf(config.deviceId)); // set representation property used by discovery
        devId = String.valueOf(config.deviceId);

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
            bridgeHandler.getAllFeaturesByDeviceId(devId);
        }
    }

    private void setPollingDevice() {
        Bridge bridge = getBridge();
        br = bridge;
        ViessmannBridgeHandler bridgeHandler = bridge == null ? null : (ViessmannBridgeHandler) bridge.getHandler();
        if (bridgeHandler != null) {
            bridgeHandler.setPollingDevice(devId);
        }
    }

    private void unsetPollingDevice() {
        Bridge bridge = br;
        ViessmannBridgeHandler bridgeHandler = bridge == null ? null : (ViessmannBridgeHandler) bridge.getHandler();
        if (bridgeHandler != null) {
            bridgeHandler.unsetPollingDevice(devId);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            Channel ch = thing.getChannel(channelUID.getId());
            if (ch != null) {
                logger.trace("ChannelUID: {}", ch.getProperties());
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
                    } else if (command instanceof QuantityType<?>) {
                        QuantityType<?> value = (QuantityType<?>) command;
                        Integer f = value.intValue();
                        String s = f.toString();
                        for (String str : com) {
                            if (str.indexOf("Temperature") != -1) {
                                uri = prop.get(str + "Uri");
                                param = "{\"" + prop.get(str + "Params") + "\":" + s + "}";
                                break;
                            }
                        }
                        logger.trace("Received QuantityType Command for Channel {} Comamnd: {}",
                                thing.getChannel(channelUID.getId()), value.floatValue());
                    } else if (command instanceof StringType) {
                        for (String str : com) {
                            String s = command.toString();
                            uri = prop.get(str + "Uri");
                            param = "{\"" + prop.get(str + "Params") + "\":" + s + "}";
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
                            if (!bridgeHandler.setData(uri, param)) {
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
    public void channelLinked(ChannelUID channelUID) {
        // can be overridden by subclasses
        // standard behavior is to refresh the linked channel,
        // so the newly linked items will receive a state update.
        // handleCommand(channelUID, RefreshType.REFRESH);
    }

    @Override
    public void handleUpdateChannel(ViessmannMessage msg) {
        logger.trace("handleUpdateChannel: {}", msg);
    }

    @Override
    public void handleUpdate(FeatureDataDTO featureDataDTO) {
        logger.trace("Device handler received update: {}", featureDataDTO);
        ThingMessageDTO msg = new ThingMessageDTO();
        ViessmannFeatureMap map = new ViessmannFeatureMap();
        if (featureDataDTO.properties != null) {
            devId = featureDataDTO.deviceId;
            msg.setDeviceId(featureDataDTO.deviceId);
            msg.setFeatureClear(featureDataDTO.feature);
            msg.setFeatureDescription(map.getDescription(featureDataDTO.feature));
            FeatureCommands commands = featureDataDTO.commands;
            if (commands != null) {
                msg.setCommands(commands);
            }
            FeatureProperties prop = featureDataDTO.properties;
            ArrayList<String> entr = prop.getUsedEntries();
            if (!entr.isEmpty()) {
                for (String entry : entr) {
                    String valueEntry = "";
                    String typeEntry = "";
                    Boolean bool = false;
                    String featureName = map.getName(featureDataDTO.feature);
                    switch (entry) {
                        case "value":
                            msg.setFeatureName(featureName);
                            msg.setFeature(featureDataDTO.feature);
                            if (featureDataDTO.feature.indexOf("temperature") != -1) {
                                typeEntry = "temperature";
                            } else {
                                typeEntry = prop.value.type;
                            }
                            valueEntry = prop.value.value;
                            break;
                        case "status":
                            msg.setFeatureName(featureName + " status");
                            msg.setFeature(featureDataDTO.feature + "#status");
                            typeEntry = prop.status.type;
                            valueEntry = prop.status.value;
                            if ("off".equals(valueEntry)) {
                                typeEntry = "boolean";
                                bool = false;
                            } else if ("on".equals(valueEntry)) {
                                typeEntry = "boolean";
                                bool = true;
                            }
                            break;
                        case "active":
                            msg.setFeatureName(featureName + " active");
                            msg.setFeature(featureDataDTO.feature + "#active");
                            typeEntry = prop.active.type;
                            valueEntry = prop.active.value ? "true" : "false";
                            bool = prop.active.value;
                            break;
                        case "name":
                            typeEntry = prop.name.type;
                            valueEntry = prop.name.value;
                            break;
                        case "shift":
                            msg.setFeatureName(featureName + " shift");
                            msg.setFeature(featureDataDTO.feature + "#shift");
                            typeEntry = prop.shift.type;
                            valueEntry = prop.shift.value.toString();
                            break;
                        case "slope":
                            msg.setFeatureName(featureName + " slope");
                            msg.setFeature(featureDataDTO.feature + "#slope");
                            typeEntry = prop.slope.type;
                            valueEntry = prop.slope.value.toString();
                            break;
                        case "entries":
                            msg.setFeatureName(featureName);
                            msg.setFeature(featureDataDTO.feature + "#schedule");
                            typeEntry = prop.entries.type.toString();
                            valueEntry = new Gson().toJson(prop.entries.value);
                            break;
                        case "overlapAllowed":
                            msg.setFeatureName(featureName);
                            msg.setFeature(featureDataDTO.feature + "#overlapAllowed");
                            typeEntry = prop.overlapAllowed.type;
                            valueEntry = prop.overlapAllowed.value ? "true" : "false";
                            bool = prop.overlapAllowed.value;
                            break;
                        case "temperature":
                            msg.setFeatureName(featureName + " temperature");
                            msg.setFeature(featureDataDTO.feature + "#temperature");
                            typeEntry = prop.temperature.type;
                            valueEntry = prop.temperature.value.toString();
                            typeEntry = "temperature";
                            break;
                        case "start":
                            msg.setFeatureName(featureName + " start");
                            msg.setFeature(featureDataDTO.feature + "#start");
                            typeEntry = prop.start.type;
                            valueEntry = prop.start.value;
                            break;
                        case "end":
                            msg.setFeatureName(featureName + " end");
                            msg.setFeature(featureDataDTO.feature + "#end");
                            typeEntry = prop.end.type;
                            valueEntry = prop.end.value;
                            break;
                        case "top":
                            msg.setFeatureName(featureName + " top");
                            msg.setFeature(featureDataDTO.feature + "#top");
                            typeEntry = prop.top.type;
                            valueEntry = prop.top.value.toString();
                            break;
                        case "middle":
                            msg.setFeatureName(featureName + " middle");
                            msg.setFeature(featureDataDTO.feature + "#middle");
                            typeEntry = prop.middle.type;
                            valueEntry = prop.middle.value.toString();
                            break;
                        case "bottom":
                            msg.setFeatureName(featureName + " bottom");
                            msg.setFeature(featureDataDTO.feature + "#bottom");
                            typeEntry = prop.bottom.type;
                            valueEntry = prop.bottom.value.toString();
                            break;
                        case "day":
                            msg.setFeatureName(featureName + " Day");
                            msg.setFeature(featureDataDTO.feature + "#day");
                            // returns array as string
                            typeEntry = prop.day.type;
                            valueEntry = prop.day.value.toString();
                            break;
                        case "week":
                            msg.setFeatureName(featureName + " Week");
                            msg.setFeature(featureDataDTO.feature + "#week");
                            // returns array as string
                            typeEntry = prop.week.type;
                            valueEntry = prop.week.value.toString();
                            break;
                        case "month":
                            msg.setFeatureName(featureName + " Month");
                            msg.setFeature(featureDataDTO.feature + "#month");
                            // returns array as string
                            typeEntry = prop.month.type;
                            valueEntry = prop.month.value.toString();
                            break;
                        case "year":
                            msg.setFeatureName(featureName + " Year");
                            msg.setFeature(featureDataDTO.feature + "#year");
                            // returns array as string
                            typeEntry = prop.year.type;
                            valueEntry = prop.year.value.toString();
                            break;
                        case "unit":
                            msg.setFeatureName(featureName + " unit");
                            msg.setFeature(featureDataDTO.feature + "#unit");
                            typeEntry = prop.unit.type;
                            valueEntry = prop.unit.value;
                            break;
                        case "starts":
                            msg.setFeatureName(featureName + " Starts");
                            msg.setFeature(featureDataDTO.feature + "#starts");
                            typeEntry = prop.starts.type;
                            valueEntry = prop.starts.value.toString();
                            break;
                        case "hours":
                            msg.setFeatureName(featureName + " Hours");
                            msg.setFeature(featureDataDTO.feature + "#hours");
                            typeEntry = prop.hours.type;
                            valueEntry = prop.hours.value.toString();
                            break;
                        default:
                            break;
                    }
                    msg.setType(typeEntry);
                    msg.setValue(valueEntry);
                    msg.setChannelType("type-" + typeEntry);
                    Boolean active = true;
                    if (msg.getDeviceId().indexOf(config.deviceId) != -1 && active) {
                        logger.trace("Feature: {} Type:{} Entry: {}={}", featureDataDTO.feature, typeEntry, entry,
                                valueEntry);
                        if (thing.getChannel(msg.getChannelId()) == null && !"unit".equals(entry)) {
                            createChannel(msg);
                        }
                        if ("temperature".equals(typeEntry)) {
                            DecimalType state = DecimalType.valueOf(msg.getValue());
                            updateState(msg.getChannelId(), state);
                        } else if ("number".equals(typeEntry)) {
                            DecimalType state = DecimalType.valueOf(msg.getValue());
                            updateState(msg.getChannelId(), state);
                        } else if ("boolean".equals(typeEntry)) {
                            OnOffType state = bool ? OnOffType.ON : OnOffType.OFF;
                            updateState(msg.getChannelId(), state);
                        } else if ("string".equals(typeEntry) || "Schedule".equals(typeEntry)
                                || "array".equals(typeEntry)) {
                            StringType state = StringType.valueOf(msg.getValue());
                            updateState(msg.getChannelId(), state);
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

        Map<String, String> prop = new HashMap<>();
        prop.put("feature", msg.getFeatureClear());
        String channelType = msg.getChannelType();

        FeatureCommands commands = msg.getCommands();
        if (commands != null) {
            ArrayList<String> com = commands.getUsedCommands();
            if (!com.isEmpty()) {
                for (String command : com) {
                    switch (command) {
                        case "setName":
                            channelType = msg.getChannelType();
                            prop.put("setNameUri", commands.setName.uri);
                            prop.put("command", "setName");
                            prop.put("setNameParams", "name");
                            break;
                        case "setCurve":
                            channelType = msg.getChannelType();
                            prop.put("setCurveUri", commands.setCurve.uri);
                            prop.put("command", "setCurve");
                            prop.put("setCurveParams", "slope,shift");
                            break;
                        case "setSchedule":
                            channelType = msg.getChannelType();
                            prop.put("setScheduleUri", commands.setSchedule.uri);
                            prop.put("command", "setSchedule");
                            prop.put("setScheduleParams", "newSchedule");
                            break;
                        case "setMode":
                            channelType = msg.getChannelType();
                            prop.put("setModeUri", commands.setMode.uri);
                            prop.put("command", "setMode");
                            prop.put("setModeParams", "mode");
                            break;
                        case "setTemperature":
                            if (!"type-boolean".equals(channelType)) {
                                channelType = "type-settemperature";
                            }
                            prop.put("setTemperatureUri", commands.setTemperature.uri);
                            prop.put("command", "setTemperature");
                            prop.put("setTemperatureParams", "targetTemperature");
                            break;
                        case "activate":
                            channelType = msg.getChannelType();
                            prop.put("activateUri", commands.activate.uri);
                            prop.put("command", "activate,deactivate");
                            prop.put("activateParams", "{}");
                            prop.put("deactivateParams", "{}");
                            break;
                        case "deactivate":
                            channelType = msg.getChannelType();
                            prop.put("deactivateUri", commands.deactivate.uri);
                            prop.put("command", "activate,deactivate");
                            prop.put("activateParams", "{}");
                            prop.put("deactivateParams", "{}");
                            break;
                        case "changeEndDate":
                            channelType = msg.getChannelType();
                            prop.put("changeEndDateUri", commands.changeEndDate.uri);
                            prop.put("command", "changeEndDate,schedule,unschedule");
                            prop.put("changeEndDatepParams", "end");
                            prop.put("scheduleParams", "start,end");
                            prop.put("unscheduleParams", "{}");
                            break;
                        case "schedule":
                            channelType = msg.getChannelType();
                            prop.put("scheduleUri", commands.schedule.uri);
                            prop.put("scheduleParams", "start,end");
                            break;
                        case "unschedule":
                            channelType = msg.getChannelType();
                            prop.put("unscheduleUri", commands.unschedule.uri);
                            prop.put("unscheduleParams", "{}");
                            break;
                        case "setTargetTemperature":
                            channelType = "type-setTargetTemperature";
                            prop.put("setTargetTemperatureUri", commands.setTargetTemperature.uri);
                            prop.put("command", "setTargetTemperature");
                            prop.put("setTargetTemperatureParams", "temperature");
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, channelType);
        if (msg.getFeatureName().indexOf("active") != -1) {
            logger.trace("Feature: {} ChannelType: {}", msg.getFeatureClear(), channelType);
        }
        Channel channel = callback.createChannelBuilder(channelUID, channelTypeUID).withLabel(msg.getFeatureName())
                .withDescription(msg.getFeatureDescription()).withProperties(prop).build();
        updateThing(editThing().withoutChannel(channelUID).withChannel(channel).build());
    }
}
