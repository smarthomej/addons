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
package org.smarthomej.transform.basicprofiles.internal.factory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.i18n.ProfileTypeI18nLocalizationService;
import org.openhab.core.util.BundleResolver;
import org.smarthomej.transform.basicprofiles.internal.profiles.GenericCommandTriggerProfile;
import org.smarthomej.transform.basicprofiles.internal.profiles.RoundStateProfile;
import org.smarthomej.transform.basicprofiles.internal.profiles.ThresholdStateProfile;

/**
 * Basic unit tests for {@link BasicProfilesFactory}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@NonNullByDefault
public class BasicProfilesFactoryTest {

    private static final int NUMBER_OF_PROFILES = 6;

    private static final Map<String, Object> PROPERTIES = Map.of(ThresholdStateProfile.PARAM_THRESHOLD, 15,
            RoundStateProfile.PARAM_SCALE, 2, GenericCommandTriggerProfile.PARAM_EVENTS, "1002,1003",
            GenericCommandTriggerProfile.PARAM_COMMAND, OnOffType.ON.toString());
    private static final Configuration CONFIG = new Configuration(PROPERTIES);

    private @Mock @NonNullByDefault({}) ProfileTypeI18nLocalizationService mockLocalizationService;
    private @Mock @NonNullByDefault({}) BundleResolver mockBundleResolver;
    private @Mock @NonNullByDefault({}) ProfileCallback mockCallback;
    private @Mock @NonNullByDefault({}) ProfileContext mockContext;

    private @NonNullByDefault({}) BasicProfilesFactory profileFactory;

    @BeforeEach
    public void setup() {
        profileFactory = new BasicProfilesFactory(mockLocalizationService, mockBundleResolver);

        when(mockContext.getConfiguration()).thenReturn(CONFIG);
    }

    @Test
    public void systemProfileTypesAndUidsShouldBeAvailable() {
        Collection<ProfileTypeUID> supportedProfileTypeUIDs = profileFactory.getSupportedProfileTypeUIDs();
        assertThat(supportedProfileTypeUIDs, hasSize(NUMBER_OF_PROFILES));

        Collection<ProfileType> supportedProfileTypes = profileFactory.getProfileTypes(null);
        assertThat(supportedProfileTypeUIDs, hasSize(NUMBER_OF_PROFILES));

        for (ProfileType profileType : supportedProfileTypes) {
            assertTrue(supportedProfileTypeUIDs.contains(profileType.getUID()));
        }
    }

    @Test
    public void testFactoryCreatesAvailableProfiles() {
        for (ProfileTypeUID profileTypeUID : profileFactory.getSupportedProfileTypeUIDs()) {
            assertNotNull(profileFactory.createProfile(profileTypeUID, mockCallback, mockContext));
        }
    }
}
