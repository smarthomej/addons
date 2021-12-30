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
package org.smarthomej.binding.tuya.internal.local.handlers;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * The {@link UserEventHandler} is a Netty handler for events (used for closing the connection)
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UserEventHandler extends ChannelDuplexHandler {
    private final Logger logger = LoggerFactory.getLogger(UserEventHandler.class);

    @Override
    public void userEventTriggered(@NonNullByDefault({}) ChannelHandlerContext ctx, @NonNullByDefault({}) Object evt) {
        if (evt instanceof DisposeEvent) {
            logger.debug("Received DisposeEvent, closing channel");
            ctx.close();
        }
    }

    public static class DisposeEvent {
    }
}
