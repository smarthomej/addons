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
import org.openhab.core.thing.binding.generic.ChannelHandler;
import org.openhab.core.thing.binding.generic.ChannelTransformation;
import org.openhab.core.thing.binding.generic.converter.ColorChannelHandler;
import org.openhab.core.thing.binding.generic.converter.DimmerChannelHandler;
import org.openhab.core.thing.binding.generic.converter.FixedValueMappingChannelHandler;
import org.openhab.core.thing.binding.generic.converter.GenericChannelHandler;
import org.openhab.core.thing.binding.generic.converter.ImageChannelHandler;
import org.openhab.core.thing.binding.generic.converter.NumberChannelHandler;
import org.openhab.core.thing.binding.generic.converter.PlayerChannelHandler;
import org.openhab.core.thing.binding.generic.converter.RollershutterChannelHandler;
import org.openhab.core.thing.internal.binding.generic.converter.AbstractTransformingChannelHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tcpudp.internal.config.TcpUdpChannelConfig;

/**
 * The {@link ChannelHandlerFactory} is a helper class for creating {@link ChannelHandler}s
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ChannelHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(ChannelHandlerFactory.class);

    private @Nullable Consumer<String> sendValue;
    private BiConsumer<ChannelUID, State> updateState;
    private BiConsumer<ChannelUID, Command> postCommand;

    public ChannelHandlerFactory(BiConsumer<ChannelUID, State> updateState, BiConsumer<ChannelUID, Command> postCommand,
            @Nullable Consumer<String> sendValue) {
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

    public Optional<ChannelHandler> create(ChannelUID channelUID, @Nullable String acceptedItemType,
            TcpUdpChannelConfig channelConfig) {
        if (acceptedItemType == null) {
            logger.warn("Cannot determine item-type for channel '{}'", channelUID);
            return Optional.empty();
        }

        ChannelHandler channelHandler = null;
        switch (acceptedItemType) {
            case "Color":
                channelHandler = createChannelHandler(ColorChannelHandler::new, channelUID, channelConfig);
                break;
            case "DateTime":
                channelHandler = createGenericChannelHandler(channelUID, channelConfig, DateTimeType::new);
                break;
            case "Dimmer":
                channelHandler = createChannelHandler(DimmerChannelHandler::new, channelUID, channelConfig);
                break;
            case "Contact":
            case "Switch":
                channelHandler = createChannelHandler(FixedValueMappingChannelHandler::new, channelUID, channelConfig);
                break;
            case "Image":
                channelHandler = new ImageChannelHandler(state -> updateState.accept(channelUID, state));
                break;
            case "Location":
                channelHandler = createGenericChannelHandler(channelUID, channelConfig, PointType::new);
                break;
            case "Number":
                channelHandler = createChannelHandler(NumberChannelHandler::new, channelUID, channelConfig);
                break;
            case "Player":
                channelHandler = createChannelHandler(PlayerChannelHandler::new, channelUID, channelConfig);
                break;
            case "Rollershutter":
                channelHandler = createChannelHandler(RollershutterChannelHandler::new, channelUID, channelConfig);
                break;
            case "String":
                channelHandler = createGenericChannelHandler(channelUID, channelConfig, StringType::new);
                break;
            default:
                logger.warn("Unsupported item-type '{}'", acceptedItemType);
        }

        return Optional.ofNullable(channelHandler);
    }

    private ChannelHandler createChannelHandler(AbstractTransformingChannelHandler.Factory factory,
            ChannelUID channelUID, TcpUdpChannelConfig channelConfig) {
        return factory.create(state -> updateState.accept(channelUID, state),
                command -> postCommand.accept(channelUID, command), sendValue,
                new ChannelTransformation(channelConfig.stateTransformation),
                new ChannelTransformation(channelConfig.commandTransformation), channelConfig);
    }

    private ChannelHandler createGenericChannelHandler(ChannelUID channelUID, TcpUdpChannelConfig channelConfig,
            Function<String, State> toState) {
        AbstractTransformingChannelHandler.Factory factory = (state, command, value, stateTrans, commandTrans,
                config) -> new GenericChannelHandler(toState, state, command, value, stateTrans, commandTrans, config);
        return createChannelHandler(factory, channelUID, channelConfig);
    }
}
