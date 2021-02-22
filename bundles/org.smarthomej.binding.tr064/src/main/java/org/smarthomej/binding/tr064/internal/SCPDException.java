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
package org.smarthomej.binding.tr064.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * The{@link SCPDException} is a catched Exception that is thrown in case of errors during SCPD processing
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class SCPDException extends Exception {
    private static final long serialVersionUID = 1L;

    public SCPDException(String message) {
        super(message);
    }
}
