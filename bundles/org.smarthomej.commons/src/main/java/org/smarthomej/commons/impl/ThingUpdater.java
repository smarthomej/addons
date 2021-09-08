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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
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
@NonNullByDefault
public class ThingUpdater {
    private static final Pattern UPDATE_INSTRUCTION = Pattern
            .compile("(?<version>\\d+);(?<action>ADD_CHANNEL|UPDATE_CHANNEL|REMOVE_CHANNEL);(?<parameters>.*)");

    private final Logger logger = LoggerFactory.getLogger(ThingUpdater.class);
    private final TreeMap<Integer, List<UpdateInstruction>> updateInstructions = new TreeMap<>();
    private final ThingUID thingUid;
    private int currentThingTypeVersion;
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);

    public ThingUpdater(Thing thing, Class<?> clazz) {
        currentThingTypeVersion = Integer
                .parseInt(thing.getProperties().getOrDefault(PROPERTY_THING_TYPE_VERSION, "0"));
        thingUid = thing.getUID();
        String thingType = thing.getThingTypeUID().getId();

        // we need the classloader of the bundle that our handler belongs to
        ClassLoader classLoader = clazz.getClassLoader();
        if (classLoader == null) {
            logger.warn("Could not get classloader for class {}", clazz);
            return;
        }

        String fileName = "update/" + thingType + ".update";
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
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

    public void update(Supplier<ThingBuilder> thingBuilderSupplier, ThingHandlerCallback callback,
            Supplier<Boolean> isInitialized, Consumer<Thing> update) {
        if (!isInitialized.get()) {
            logger.trace("Thing {} is not yet initialized, deferring update for 500ms.", thingUid);
            scheduler.schedule(() -> update(thingBuilderSupplier, callback, isInitialized, update), 500,
                    TimeUnit.MILLISECONDS);
            return;
        }
        ThingBuilder thingBuilder = thingBuilderSupplier.get();
        updateInstructions.forEach((targetThingTypeVersion, updateInstruction) -> {
            logger.info("Updating {} from version {} to {}", thingUid, currentThingTypeVersion, targetThingTypeVersion);
            updateInstruction.forEach(instruction -> processUpdateInstruction(instruction, thingBuilder, callback));
            currentThingTypeVersion = targetThingTypeVersion;
        });
        thingBuilder.withProperties(Map.of(PROPERTY_THING_TYPE_VERSION, String.valueOf(updateInstructions.lastKey())));
        update.accept(thingBuilder.build());
    }

    private void processUpdateInstruction(UpdateInstruction instruction, ThingBuilder thingBuilder,
            ThingHandlerCallback callback) {
        ChannelUID affectedChannelUid = new ChannelUID(thingUid, instruction.channelId);
        switch (instruction.updateCommand) {
            case UPDATE_CHANNEL:
                thingBuilder.withoutChannel(affectedChannelUid);
                // fall-through to add channel
            case ADD_CHANNEL:
                ChannelBuilder channelBuilder = callback.createChannelBuilder(affectedChannelUid,
                        new ChannelTypeUID(instruction.parameters.get(1)));
                if (instruction.parameters.size() >= 3) {
                    // label is optional (could be inherited from thing-type)
                    channelBuilder.withLabel(instruction.parameters.get(2));
                }
                if (instruction.parameters.size() == 4) {
                    // label is optional (could be inherited from thing-type)
                    channelBuilder.withDescription(instruction.parameters.get(3));
                }
                thingBuilder.withChannel(channelBuilder.build());
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

            if (!this.updateCommand.checkParameterCount(this.parameters.size())) {
                throw new IllegalArgumentException("Wrong number of parameters: " + this.parameters.size());
            }
        }
    }

    private enum UpdateCommand {
        ADD_CHANNEL(2, 4),
        REMOVE_CHANNEL(0, 0),
        UPDATE_CHANNEL(2, 4);

        private final int minParameterCount;
        private final int maxParameterCount;

        UpdateCommand(int minParameterCount, int maxParameterCount) {
            this.minParameterCount = minParameterCount;
            this.maxParameterCount = maxParameterCount;
        }

        public boolean checkParameterCount(int parameterCount) {
            return parameterCount >= minParameterCount && parameterCount <= maxParameterCount;
        }
    }
}
