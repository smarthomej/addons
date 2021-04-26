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
package org.smarthomej.binding.notificationsforfiretv.internal;

/**
 * The {@link NotificationsForFireTVConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Tom Blum - Initial contribution
 */
public class NotificationsForFireTVConfiguration {

    public String ip = "";
    public int port = 7676;
    public int type = 0;
    public String title = "OpenHAB";
    public int duration = 10;
    public int fontsize = 0;
    public int position = 0;
    public String bkgcolor = "#607d8b";
    public int transparency = 2;
    public int offset = 0;
    public int offsety = 0;
    public String app = title;
    public Boolean force = true;
    public Boolean interrupt = true;
}
