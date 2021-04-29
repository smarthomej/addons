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
package org.smarthomej.binding.notificationsforfiretv.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link NotificationsForFireTVConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Tom Blum - Initial contribution
 */
@NonNullByDefault
public class NotificationsForFireTVConfiguration {

    public String hostname = "";
    public int port = 7676;
    public int type = 0;
    public String title = "Smarthome";
    public int duration = 10;
    public int fontsize = 0;
    public int position = 0;
    public String bkgcolor = "#607d8b";
    public int transparency = 2;
    public int offsetX = 0;
    public int offsetY = 0;
    public String app = title;
    public boolean force = true;
    public int interrupt = 0;
}
