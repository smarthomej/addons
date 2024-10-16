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
package org.smarthomej.binding.tuya.internal.local.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link IrCode} represents the IR code decoded messages sent by Tuya devices
 *
 * @author Dmitry Pyatykh - Initial contribution
 */
@NonNullByDefault
public class IrCode {
    public String type = "";
    public String hex = "";
    public Integer address = 0;
    public Integer data = 0;
}
