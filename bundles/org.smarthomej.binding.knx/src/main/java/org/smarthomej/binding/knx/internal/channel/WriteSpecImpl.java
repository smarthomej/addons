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
package org.smarthomej.binding.knx.internal.channel;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Type;
import org.smarthomej.binding.knx.internal.client.OutboundSpec;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXFormatException;

/**
 * Command meta-data
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
public class WriteSpecImpl extends AbstractSpec implements OutboundSpec {

    private final Type value;
    private final @Nullable GroupAddress groupAddress;

    public WriteSpecImpl(@Nullable ChannelConfiguration channelConfiguration, String defaultDPT, Type value)
            throws KNXFormatException {
        super(channelConfiguration, defaultDPT);
        if (channelConfiguration != null) {
            this.groupAddress = new GroupAddress(channelConfiguration.getMainGA().getGA());
        } else {
            this.groupAddress = null;
        }
        this.value = value;
    }

    @Override
    public Type getValue() {
        return value;
    }

    @Override
    public @Nullable GroupAddress getGroupAddress() {
        return groupAddress;
    }

    @Override
    public boolean matchesDestination(GroupAddress groupAddress) {
        return groupAddress.equals(this.groupAddress);
    }
}
