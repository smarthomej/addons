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
package org.smarthomej.commons.impl;

import static org.smarthomej.commons.UpdatingBaseThingHandler.PROPERTY_THING_TYPE_VERSION;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ThingUpdater} is responsible for processing thing updates
 *
 * @author Jan N. Klug - Initial contribution
 */
public class ThingUpdater {
    private static final Pattern UPDATE_INSTRUCTION = Pattern
            .compile("(?<version>\\d+);(?<action>ADD_CHANNEL|UPDATE_CHANNEL|REMOVE_CHANNEL);(?<parameters>.*)");

    private final Logger logger = LoggerFactory.getLogger(ThingUpdater.class);
    private final TreeMap<Integer, List<UpdateInstruction>> updateInstructions = new TreeMap<>();
    private final ThingUID thingUid;
    private int currentThingTypeVersion;

    public ThingUpdater(Thing thing) {
        currentThingTypeVersion = Integer
                .parseInt(thing.getProperties().getOrDefault(PROPERTY_THING_TYPE_VERSION, "0"));
        thingUid = thing.getUID();
        String thingType = thing.getThingTypeUID().getId();

        InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("update" + File.separator + thingType + ".update");
        if (inputStream == null) {
            logger.trace("No update instructions found for thing type '{}'", thingType);
            return;
        }

        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            scanner.useDelimiter(Pattern.compile("\\r|\\n|\\r\\n"));
            while (scanner.hasNext()) {
                String line = scanner.next().trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                Matcher matcher = UPDATE_INSTRUCTION.matcher(line);
                if (!matcher.matches()) {
                    logger.warn("Line '{}' did not match format for instruction. Ignoring.", line);
                    continue;
                }

                // create update instruction: version;command;parameter(s)
                int targetThingTypeVersion = Integer.parseInt(matcher.group("version"));
                if (targetThingTypeVersion > currentThingTypeVersion) {
                    try {
                        updateInstructions.compute(targetThingTypeVersion, (k, v) -> {
                            List<UpdateInstruction> list = Objects.requireNonNullElse(v, new ArrayList<>());
                            list.add(new UpdateInstruction(matcher.group("action"), matcher.group("parameters")));
                            return list;
                        });
                    } catch (IllegalArgumentException e) {
                        logger.warn("Illegal thing update instruction '{}' found: {}", line, e.getMessage());
                    }
                }
            }
        }
    }

    public boolean thingNeedsUpdate() {
        return !updateInstructions.isEmpty();
    }

    public ThingBuilder update(ThingBuilder thingBuilder) {
        updateInstructions.forEach((targetThingTypeVersion, updateInstruction) -> {
            logger.info("Updating {} from version {} to {}", thingUid, currentThingTypeVersion, targetThingTypeVersion);
            updateInstruction.forEach(instruction -> processUpdateInstruction(instruction, thingBuilder));
        });
        thingBuilder.withProperties(Map.of(PROPERTY_THING_TYPE_VERSION, String.valueOf(updateInstructions.lastKey())));
        currentThingTypeVersion = updateInstructions.lastKey();
        return thingBuilder;
    }

    private void processUpdateInstruction(UpdateInstruction instruction, ThingBuilder thingBuilder) {
        ChannelUID affectedChannelUid = new ChannelUID(thingUid, instruction.channelId);
        switch (instruction.updateCommand) {
            case UPDATE_CHANNEL:
                thingBuilder.withoutChannel(affectedChannelUid);
                // fall-through to add channel
            case ADD_CHANNEL:
                Channel channel = ChannelBuilder.create(affectedChannelUid, instruction.parameters.get(0))
                        .withType(new ChannelTypeUID(instruction.parameters.get(1)))
                        .withLabel(instruction.parameters.get(2)).build();
                thingBuilder.withChannel(channel);
                break;
            case REMOVE_CHANNEL:
                thingBuilder.withoutChannel(affectedChannelUid);
                break;
        }
    }

    private static class UpdateInstruction {
        public final UpdateCommand updateCommand;
        public final String channelId;
        public final List<String> parameters;

        public UpdateInstruction(String updateCommand, String parameters) {
            this.updateCommand = UpdateCommand.valueOf(updateCommand);
            this.parameters = new ArrayList<>(List.of(parameters.split(",")));
            // first is always channelId
            this.channelId = this.parameters.remove(0);

            if (this.parameters.size() != this.updateCommand.getParameterCount()) {
                throw new IllegalArgumentException("Wrong number of parameters: " + this.parameters.size());
            }
        }
    }

    private enum UpdateCommand {
        ADD_CHANNEL(3),
        REMOVE_CHANNEL(0),
        UPDATE_CHANNEL(3);

        private final int parameterCount;

        UpdateCommand(int parameterCount) {
            this.parameterCount = parameterCount;
        }

        public int getParameterCount() {
            return parameterCount;
        }
    }
}
