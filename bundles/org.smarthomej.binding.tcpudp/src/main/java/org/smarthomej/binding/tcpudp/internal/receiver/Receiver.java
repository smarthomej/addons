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
package org.smarthomej.binding.tcpudp.internal.receiver;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.commons.itemvalueconverter.ItemValueConverter;

/**
 * The {@link Receiver} is an interface for TCP and UDP Receivers
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface Receiver extends Runnable {
    /**
     * stops this receiver by closing the socket and preventing re-connection
     */
    void stop();

    class ContentListener {
        public final ItemValueConverter itemValueConverter;
        public final Pattern addressFilter;

        public ContentListener(ItemValueConverter itemValueConverter, String addressFilter) {
            this.itemValueConverter = itemValueConverter;
            // convert input pattern to regex, using only * as wildcard
            this.addressFilter = Pattern.compile(Pattern.quote(addressFilter).replace("*", "\\E.*?\\Q"));
        }
    }
}
