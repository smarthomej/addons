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
package org.smarthomej.binding.viessmann.internal;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link ViessmannBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class ViessmannBindingConstants {

    public static final String BINDING_ID = "viessmann";
    public static final String BINDING_NAME = "Viessmann API";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");

    public static final Set<ThingTypeUID> DISCOVERABLE_DEVICE_TYPE_UIDS = Set.of(THING_TYPE_DEVICE);

    public static final String COUNT_API_CALLS = "countApiCalls";

    // References for needed API identifiers
    public static final String VIESSMANN_HOST = "api.viessmann.com";
    public static final String VIESSMANN_BASE_URL = "https://api.viessmann.com/";
    public static final String IAM_BASE_URL = "https://iam.viessmann.com/";
    public static final String VIESSMANN_AUTHORIZE_URL = IAM_BASE_URL + "idp/v2/authorize";
    public static final String VIESSMANN_TOKEN_URL = IAM_BASE_URL + "idp/v2/token";
    public static final String VIESSMANN_SCOPE = "IoT%20User%20offline_access";

    public static final String PROPERTY_ID = "deviceId";
}
