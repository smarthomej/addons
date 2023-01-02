/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tcpudp.internal.config.TcpUdpChannelConfig;
import org.smarthomej.commons.itemvalueconverter.ItemValueConverter;
import org.smarthomej.commons.itemvalueconverter.converter.AbstractTransformingItemConverter;
import org.smarthomej.commons.itemvalueconverter.converter.ColorItemConverter;
import org.smarthomej.commons.itemvalueconverter.converter.DimmerItemConverter;
import org.smarthomej.commons.itemvalueconverter.converter.FixedValueMappingItemConverter;
import org.smarthomej.commons.itemvalueconverter.converter.GenericItemConverter;
import org.smarthomej.commons.itemvalueconverter.converter.ImageItemConverter;
import org.smarthomej.commons.itemvalueconverter.converter.NumberItemConverter;
import org.smarthomej.commons.itemvalueconverter.converter.PlayerItemConverter;
import org.smarthomej.commons.itemvalueconverter.converter.RollershutterItemConverter;
import org.smarthomej.commons.transform.ValueTransformationProvider;

/**
 * The {@link ItemValueConverterFactory} is a helper class for creating {@link ItemValueConverter}s
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ItemValueConverterFactory {
    private final Logger logger = LoggerFactory.getLogger(ItemValueConverterFactory.class);

    private final ValueTransformationProvider valueTransformationProvider;

    private @Nullable Consumer<String> sendValue;
    private BiConsumer<ChannelUID, State> updateState;
    private BiConsumer<ChannelUID, Command> postCommand;

    public ItemValueConverterFactory(ValueTransformationProvider valueTransformationProvider,
            BiConsumer<ChannelUID, State> updateState, BiConsumer<ChannelUID, Command> postCommand,
            @Nullable Consumer<String> sendValue) {
        this.valueTransformationProvider = valueTransformationProvider;
        this.updateState = updateState;
        this.postCommand = postCommand;
        this.sendValue = sendValue;
    }

    public void setSendValue(Consumer<String> sendValue) {
        this.sendValue = sendValue;
    }

    public void setUpdateState(BiConsumer<ChannelUID, State> updateState) {
        this.updateState = updateState;
    }

    public void setPostCommand(BiConsumer<ChannelUID, Command> postCommand) {
        this.postCommand = postCommand;
    }

    public Optional<ItemValueConverter> create(ChannelUID channelUID, @Nullable String acceptedItemType,
            TcpUdpChannelConfig channelConfig) {
        if (acceptedItemType == null) {
            logger.warn("Cannot determine item-type for channel '{}'", channelUID);
            return Optional.empty();
        }

        ItemValueConverter itemValueConverter = null;
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
                itemValueConverter = new ImageItemConverter(state -> updateState.accept(channelUID, state));
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
                logger.warn("Unsupported item-type '{}'", acceptedItemType);
        }

        return Optional.ofNullable(itemValueConverter);
    }

    private ItemValueConverter createItemConverter(AbstractTransformingItemConverter.Factory factory,
            ChannelUID channelUID, TcpUdpChannelConfig channelConfig) {
        return factory.create(state -> updateState.accept(channelUID, state),
                command -> postCommand.accept(channelUID, command), sendValue,
                valueTransformationProvider.getValueTransformation(channelConfig.stateTransformation),
                valueTransformationProvider.getValueTransformation(channelConfig.commandTransformation), channelConfig);
    }

    private ItemValueConverter createGenericItemConverter(ChannelUID channelUID, TcpUdpChannelConfig channelConfig,
            Function<String, State> toState) {
        AbstractTransformingItemConverter.Factory factory = (state, command, value, stateTrans, commandTrans,
                config) -> new GenericItemConverter(toState, state, command, value, stateTrans, commandTrans, config);
        return createItemConverter(factory, channelUID, channelConfig);
    }
}
