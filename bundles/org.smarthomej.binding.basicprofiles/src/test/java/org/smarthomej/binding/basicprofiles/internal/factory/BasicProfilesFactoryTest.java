/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.basicprofiles.internal.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.i18n.ProfileTypeI18nLocalizationService;
import org.openhab.core.util.BundleResolver;

/**
 * Basic unit tests for {@link BasicProfilesFactory}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class BasicProfilesFactoryTest {

    private static final int NUMBER_OF_PROFILES = 10;

    private static final Map<String, Object> PROPERTIES = Map.of("offset", "30", "threshold", 15, "scale", 2, "events",
            "1002,1003", "command", OnOffType.ON.toString(), "min", 0, "max", 100);
    private static final Configuration CONFIG = new Configuration(PROPERTIES);

    private BasicProfilesFactory profileFactory;
    private @Mock ProfileTypeI18nLocalizationService mockLocalizationService;
    private @Mock BundleResolver mockBundleResolver;
    // private @Mock ProfileCallback mockCallback;
    private @Mock ProfileContext mockContext;

    @BeforeEach
    public void setup() {
        profileFactory = new BasicProfilesFactory(mockLocalizationService, mockBundleResolver);

        when(mockContext.getConfiguration()).thenReturn(CONFIG);
    }

    @Test
    public void systemProfileTypesAndUidsShouldBeAvailable() {
        Collection<ProfileTypeUID> supportedProfileTypeUIDs = profileFactory.getSupportedProfileTypeUIDs();
        assertThat(supportedProfileTypeUIDs.size(), is(NUMBER_OF_PROFILES));

        Collection<ProfileType> supportedProfileTypes = profileFactory.getProfileTypes(null);
        assertThat(supportedProfileTypeUIDs.size(), is(supportedProfileTypes.size()));

        for (ProfileType profileType : supportedProfileTypes) {
            assertThat(supportedProfileTypeUIDs.contains(profileType.getUID()), is(true));
        }
    }

    // @Test
    // public void testFactoryCreatesAvailableProfiles() {
    // for (ProfileTypeUID profileTypeUID : profileFactory.getSupportedProfileTypeUIDs()) {
    // assertThat(profileFactory.createProfile(profileTypeUID, mockCallback, mockContext), is(notNullValue()));
    // }
    // }
}
