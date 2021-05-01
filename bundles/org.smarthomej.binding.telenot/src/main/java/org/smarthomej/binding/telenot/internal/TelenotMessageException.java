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
package org.smarthomej.binding.telenot.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TelenotMessage} Telenot Message Exception.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class TelenotMessageException extends Exception {
    private static final long serialVersionUID = 2021042422231712345L;

    public TelenotMessageException(String message) {
        super(message);
    }
}
