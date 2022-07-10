/**
 * Copyright (c) 2021-2022 Contributors to the SmartHome/J project
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
package org.smarthomej.transform.math.internal.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.transform.TransformationService;
import org.smarthomej.transform.math.internal.BitwiseOrTransformationService;

/**
 * Profile to offer the {@link BitwiseOrTransformationService} on a ItemChannelLink.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Jan N. Klug - Adapted To BoitwiseTransformations
 */
@NonNullByDefault
public class BitwiseOrTransformationProfile extends BitwiseTransformationProfile {

    public static final ProfileTypeUID PROFILE_TYPE_UID = new ProfileTypeUID(
            TransformationService.TRANSFORM_PROFILE_SCOPE, "BITOR");

    public BitwiseOrTransformationProfile(ProfileCallback callback, ProfileContext context,
            TransformationService service) {
        super(callback, context, service, PROFILE_TYPE_UID);
    }
}
