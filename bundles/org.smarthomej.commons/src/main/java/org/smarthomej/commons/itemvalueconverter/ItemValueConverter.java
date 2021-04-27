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
package org.smarthomej.commons.itemvalueconverter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;

/**
 * The {@link ItemValueConverter} defines the interface for converting received content to item state and converting
 * comannds to sending value
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ItemValueConverter {

    /**
     * called to process a given content for this channel
     *
     * @param content content of the HTTP request (null results in UNDEF)
     */
    void process(@Nullable ContentWrapper content);

    /**
     * called to send a command to this channel
     *
     * @param command
     */
    void send(Command command);
}
