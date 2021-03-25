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
package org.smarthomej.binding.tcpudp.internal;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tcpudp.internal.config.TcpUdpChannelConfig;
import org.smarthomej.binding.tcpudp.internal.config.ThingConfiguration;
import org.smarthomej.commons.SimpleDynamicStateDescriptionProvider;
import org.smarthomej.commons.itemvalueconverter.ChannelMode;
import org.smarthomej.commons.itemvalueconverter.ContentWrapper;
import org.smarthomej.commons.itemvalueconverter.ItemValueConverter;
import org.smarthomej.commons.itemvalueconverter.converter.*;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link TcpUdpBaseThingHandler} is a base class for thing handlers and responsible for handling commands, which
 * are
 * sent to one of the channels.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class TcpUdpBaseThingHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(TcpUdpBaseThingHandler.class);

    private final ValueTransformationProvider valueTransformationProvider;
    private final SimpleDynamicStateDescriptionProvider dynamicStateDescriptionProvider;
    private final Map<ChannelUID, ItemValueConverter> channels = new HashMap<>();
    private final Map<ChannelUID, String> readCommands = new HashMap<>();

    private @Nullable ScheduledFuture<?> refreshJob = null;

    protected ThingConfiguration config = new ThingConfiguration();

    public TcpUdpBaseThingHandler(Thing thing, ValueTransformationProvider valueTransformationProvider,
            SimpleDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        super(thing);
        this.valueTransformationProvider = valueTransformationProvider;
        this.dynamicStateDescriptionProvider = dynamicStateDescriptionProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        ItemValueConverter itemValueConverter = channels.get(channelUID);
        if (itemValueConverter == null) {
            logger.warn("Cannot find channel implementation for channel {}.", channelUID);
            return;
        }

        if (command instanceof RefreshType) {
            String stateContent = readCommands.get(channelUID);
            if (stateContent != null) {
                // return fast in handleCommand
                scheduler.execute(() -> refreshChannel(channelUID, stateContent));
            } else {
                logger.warn("Could not find stateContent for channel, '{}', REFRESH command failed.", channelUID);
            }
        } else {
            try {
                itemValueConverter.send(command);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to convert command '{}' to channel '{}' for sending", command, channelUID);
            } catch (IllegalStateException e) {
                logger.debug("Writing to read-only channel {} not permitted", channelUID);
            }
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(ThingConfiguration.class);

        if (config.host.isEmpty() || config.port == 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Parameter host must not be empty and port must not be 0!");
            return;
        }

        thing.getChannels().forEach(this::createChannel);

        if (channels.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No channels defined.");
            return;
        }

        stopRefresh();
        refreshJob = scheduler.scheduleWithFixedDelay(() -> readCommands.forEach(this::refreshChannel), 0,
                config.refresh, TimeUnit.SECONDS);
    }

    private void stopRefresh() {
        ScheduledFuture<?> refreshJob = this.refreshJob;
        if (refreshJob != null) {
            refreshJob.cancel(true);
            this.refreshJob = null;
        }
    }

    public void dispose() {
        stopRefresh();

        channels.clear();
        readCommands.clear();

        // remove state descriptions
        dynamicStateDescriptionProvider.removeDescriptionsForThing(thing.getUID());

        super.dispose();
    }

    private void refreshChannel(ChannelUID channelUID, String stateContent) {
        logger.trace("Refreshing '{}' with command '{}'", channelUID, stateContent);
        ItemValueConverter itemValueConverter = channels.get(channelUID);

        if (itemValueConverter == null) {
            logger.warn("Failed to refresh '{}': itemValueConverter not found.", channelUID);
            return;
        }

        doSyncRequest(stateContent).ifPresent(itemValueConverter::process);
    }

    private void createChannel(Channel channel) {
        ChannelUID channelUID = channel.getUID();
        TcpUdpChannelConfig channelConfig = channel.getConfiguration().as(TcpUdpChannelConfig.class);

        String acceptedItemType = channel.getAcceptedItemType();
        if (acceptedItemType == null) {
            logger.warn("Cannot determine item-type for channel '{}'", channelUID);
            return;
        }

        ItemValueConverter itemValueConverter;
        switch (acceptedItemType) {
            case "Color":
                itemValueConverter = createItemConverter(ColorItemConverter::new, channelUID, channelConfig);
                break;
            case "DateTime":
                itemValueConverter = createGenericItemConverter(channelUID, channelConfig, DateTimeType::new);
                break;
            case "Dimmer":
                itemValueConverter = createItemConverter(DimmerItemConverter::new, channelUID, channelConfig);
                break;
            case "Contact":
            case "Switch":
                itemValueConverter = createItemConverter(FixedValueMappingItemConverter::new, channelUID,
                        channelConfig);
                break;
            case "Image":
                itemValueConverter = new ImageItemConverter(state -> updateState(channelUID, state));
                break;
            case "Location":
                itemValueConverter = createGenericItemConverter(channelUID, channelConfig, PointType::new);
                break;
            case "Number":
                itemValueConverter = createItemConverter(NumberItemConverter::new, channelUID, channelConfig);
                break;
            case "Player":
                itemValueConverter = createItemConverter(PlayerItemConverter::new, channelUID, channelConfig);
                break;
            case "Rollershutter":
                itemValueConverter = createItemConverter(RollershutterItemConverter::new, channelUID, channelConfig);
                break;
            case "String":
                itemValueConverter = createGenericItemConverter(channelUID, channelConfig, StringType::new);
                break;
            default:
                logger.warn("Unsupported item-type '{}'", channel.getAcceptedItemType());
                return;
        }

        if (channelConfig.mode == ChannelMode.READONLY || channelConfig.mode == ChannelMode.READWRITE) {
            if (channelConfig.stateContent.isEmpty()) {
                logger.warn(
                        "Empty stateContent configured for channel '{}' with capability 'read'. State updates are disabled.",
                        channelUID);
            } else {
                if (!readCommands.containsValue(channelConfig.stateContent)) {
                    readCommands.put(channelUID, channelConfig.stateContent);
                } else {
                    logger.warn(
                            "'{}' is configured as 'stateContent' for more than one channel. Ignoring for channel '{}'.",
                            channelConfig.stateContent, channelUID);
                    return;
                }
            }
        }

        channels.put(channelUID, itemValueConverter);

        StateDescription stateDescription = StateDescriptionFragmentBuilder.create()
                .withReadOnly(channelConfig.mode == ChannelMode.READONLY).build().toStateDescription();
        if (stateDescription != null) {
            // if the state description is not available, we don't need to add it
            dynamicStateDescriptionProvider.setDescription(channelUID, stateDescription);
        }
    }

    private ItemValueConverter createItemConverter(AbstractTransformingItemConverter.Factory factory,
            ChannelUID channelUID, TcpUdpChannelConfig channelConfig) {
        return factory.create(state -> updateState(channelUID, state), command -> postCommand(channelUID, command),
                this::doAsyncSend,
                valueTransformationProvider.getValueTransformation(channelConfig.stateTransformation),
                valueTransformationProvider.getValueTransformation(channelConfig.commandTransformation), channelConfig);
    }

    private ItemValueConverter createGenericItemConverter(ChannelUID channelUID, TcpUdpChannelConfig channelConfig,
            Function<String, State> toState) {
        AbstractTransformingItemConverter.Factory factory = (state, command, value, stateTrans, commandTrans,
                config) -> new GenericItemConverter(toState, state, command, value, stateTrans, commandTrans, config);
        return createItemConverter(factory, channelUID, channelConfig);
    }

    protected abstract Optional<ContentWrapper> doSyncRequest(String s);

    protected abstract void doAsyncSend(String command);
}
