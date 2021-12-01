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
package org.smarthomej.io.marketplace.internal;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link AddonEntryDTO} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
class AddonEntryDTO {
    public String id;
    public String type;
    public String description = "";
    public String title;
    public String link;
    public String version;
    public String author = "";
    public String configDescriptionURI = "";
    public String maturity = "unstable";
    @SerializedName("content_type")
    public String contentType;
    public String url;
}
