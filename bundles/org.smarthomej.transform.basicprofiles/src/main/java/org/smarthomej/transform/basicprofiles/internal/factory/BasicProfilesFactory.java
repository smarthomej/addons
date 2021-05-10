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

import static org.smarthomej.transform.basicprofiles.internal.BasicProfilesConstants.SCOPE;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.LocalizedKey;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileAdvisor;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeBuilder;
import org.openhab.core.thing.profiles.ProfileTypeProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.i18n.ProfileTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smarthomej.transform.basicprofiles.internal.profiles.BatteryLowStateProfile;
import org.smarthomej.transform.basicprofiles.internal.profiles.InvertStateProfile;
import org.smarthomej.transform.basicprofiles.internal.profiles.RoundStateProfile;
import org.smarthomej.transform.basicprofiles.internal.profiles.ToPercentStateProfile;
import org.smarthomej.transform.basicprofiles.internal.profiles.ToggleSwitchTriggerProfile;

/**
 * The {@link BasicProfilesFactory} is responsible for creating profiles.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@Component(service = { ProfileFactory.class, ProfileTypeProvider.class })
@NonNullByDefault
public class BasicProfilesFactory implements ProfileFactory, ProfileTypeProvider, ProfileAdvisor {

    public static final ProfileTypeUID BATTERY_LOW_UID = new ProfileTypeUID(SCOPE, "battery-low");
    public static final ProfileTypeUID INVERT_UID = new ProfileTypeUID(SCOPE, "invert");
    public static final ProfileTypeUID ROUND_UID = new ProfileTypeUID(SCOPE, "round");
    public static final ProfileTypeUID MAP_TO_ON_TYPE_UID = new ProfileTypeUID(SCOPE, "map-to-on");
    public static final ProfileTypeUID TO_PERCENT_TYPE_UID = new ProfileTypeUID(SCOPE, "to-percent");
    public static final ProfileTypeUID GENERIC_COMMAND_PROFILE_TYPE_UID = new ProfileTypeUID(SCOPE, "generic-command");
    public static final ProfileTypeUID TOGGLE_PLAYER_PROFILE_TYPE_UID = new ProfileTypeUID(SCOPE, "toggle-player");
    public static final ProfileTypeUID TOGGLE_ROLLERSHUTTER_PROFILE_TYPE_UID = new ProfileTypeUID(SCOPE,
            "toggle-rollershutter");
    public static final ProfileTypeUID TOGGLE_SWITCH_PROFILE_TYPE_UID = new ProfileTypeUID(SCOPE, "toggle-switch");
    public static final ProfileTypeUID TIMESTAMP_OFFSET_TYPE_UID = new ProfileTypeUID(SCOPE, "timestamp-offset");

    private static final ProfileType PROFILE_TYPE_BATTERY_LOW = ProfileTypeBuilder
            .newState(BATTERY_LOW_UID, "Battery Low") //
            .withSupportedItemTypesOfChannel(CoreItemFactory.DIMMER, CoreItemFactory.NUMBER) //
            .withSupportedItemTypes(CoreItemFactory.SWITCH) //
            .build();
    private static final ProfileType PROFILE_TYPE_INVERT = ProfileTypeBuilder.newState(INVERT_UID, "Invert")
            .withSupportedItemTypes(CoreItemFactory.CONTACT, CoreItemFactory.DIMMER, CoreItemFactory.NUMBER,
                    CoreItemFactory.PLAYER, CoreItemFactory.ROLLERSHUTTER, CoreItemFactory.SWITCH) //
            .withSupportedItemTypesOfChannel(CoreItemFactory.CONTACT, CoreItemFactory.DIMMER, CoreItemFactory.NUMBER,
                    CoreItemFactory.PLAYER, CoreItemFactory.ROLLERSHUTTER, CoreItemFactory.SWITCH) //
            .build();
    private static final ProfileType PROFILE_TYPE_ROUND = ProfileTypeBuilder.newState(ROUND_UID, "Round")
            .withSupportedItemTypes(CoreItemFactory.NUMBER) //
            .withSupportedItemTypesOfChannel(CoreItemFactory.NUMBER) //
            .build();
    private static final ProfileType PROFILE_TYPE_MAP_TO_ON = ProfileTypeBuilder
            .newState(MAP_TO_ON_TYPE_UID, "Maps OPEN/CLOSED to ON on change") //
            .withSupportedItemTypesOfChannel(CoreItemFactory.CONTACT) //
            .withSupportedItemTypes(CoreItemFactory.SWITCH) //
            .build();
    private static final ProfileType PROFILE_TYPE_TO_PERCENT = ProfileTypeBuilder
            .newState(TO_PERCENT_TYPE_UID, "To Percent") //
            .withSupportedItemTypesOfChannel(CoreItemFactory.DIMMER, CoreItemFactory.NUMBER,
                    CoreItemFactory.ROLLERSHUTTER) //
            .withSupportedItemTypes(CoreItemFactory.COLOR, CoreItemFactory.DIMMER, CoreItemFactory.ROLLERSHUTTER) //
            .build();
    private static final ProfileType GENERIC_COMMAND_PROFILE_TYPE = ProfileTypeBuilder
            .newTrigger(GENERIC_COMMAND_PROFILE_TYPE_UID, "Generic Command Profile") //
            .withSupportedItemTypes(CoreItemFactory.DIMMER, CoreItemFactory.NUMBER, CoreItemFactory.PLAYER,
                    CoreItemFactory.ROLLERSHUTTER, CoreItemFactory.SWITCH) // .withSupportedChannelTypeUIDs(CHANNEL_TYPE_BUTTONEVENT)
            .build();
    private static final ProfileType TOGGLE_PLAYER_TYPE = ProfileTypeBuilder
            .newTrigger(TOGGLE_PLAYER_PROFILE_TYPE_UID, "Toggle Player Profile") //
            .withSupportedItemTypes(CoreItemFactory.PLAYER) // .withSupportedChannelTypeUIDs(CHANNEL_TYPE_BUTTONEVENT)
            .build();
    private static final ProfileType TOGGLE_ROLLERSHUTTER_TYPE = ProfileTypeBuilder
            .newTrigger(TOGGLE_ROLLERSHUTTER_PROFILE_TYPE_UID, "Toggle Rollershutter Profile") //
            .withSupportedItemTypes(CoreItemFactory.ROLLERSHUTTER) // .withSupportedChannelTypeUIDs(CHANNEL_TYPE_BUTTONEVENT)
            .build();
    private static final ProfileType TOGGLE_SWITCH_TYPE = ProfileTypeBuilder
            .newTrigger(TOGGLE_SWITCH_PROFILE_TYPE_UID, "Toggle Switch Profile") //
            .withSupportedItemTypes(CoreItemFactory.SWITCH) // .withSupportedChannelTypeUIDs(CHANNEL_TYPE_BUTTONEVENT)
            .build();
    public static final ProfileType TIMESTAMP_OFFSET_TYPE = ProfileTypeBuilder
            .newState(TIMESTAMP_OFFSET_TYPE_UID, "Timestamp Offset").withSupportedItemTypes(CoreItemFactory.DATETIME)
            .withSupportedItemTypesOfChannel(CoreItemFactory.DATETIME).build();

    private static final Set<ProfileTypeUID> SUPPORTED_PROFILE_TYPE_UIDS = Set.of(BATTERY_LOW_UID, INVERT_UID,
            ROUND_UID, MAP_TO_ON_TYPE_UID, TO_PERCENT_TYPE_UID, GENERIC_COMMAND_PROFILE_TYPE_UID,
            TOGGLE_PLAYER_PROFILE_TYPE_UID, TOGGLE_ROLLERSHUTTER_PROFILE_TYPE_UID, TOGGLE_SWITCH_PROFILE_TYPE_UID,
            TIMESTAMP_OFFSET_TYPE_UID);
    private static final Set<ProfileType> SUPPORTED_PROFILE_TYPES = Set.of(PROFILE_TYPE_BATTERY_LOW,
            PROFILE_TYPE_INVERT, PROFILE_TYPE_ROUND, PROFILE_TYPE_MAP_TO_ON, PROFILE_TYPE_TO_PERCENT,
            GENERIC_COMMAND_PROFILE_TYPE, TOGGLE_PLAYER_TYPE, TOGGLE_ROLLERSHUTTER_TYPE, TOGGLE_SWITCH_TYPE,
            TIMESTAMP_OFFSET_TYPE);

    private final Map<LocalizedKey, ProfileType> localizedProfileTypeCache = new ConcurrentHashMap<>();

    private final ProfileTypeI18nLocalizationService profileTypeI18nLocalizationService;
    private final Bundle bundle;

    @Activate
    public BasicProfilesFactory(final @Reference ProfileTypeI18nLocalizationService profileTypeI18nLocalizationService,
            final @Reference BundleResolver bundleResolver) {
        this.profileTypeI18nLocalizationService = profileTypeI18nLocalizationService;
        this.bundle = bundleResolver.resolveBundle(BasicProfilesFactory.class);
    }

    @Override
    public @Nullable Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback,
            ProfileContext context) {
        if (BATTERY_LOW_UID.equals(profileTypeUID)) {
            return new BatteryLowStateProfile(callback, context);
        } else if (INVERT_UID.equals(profileTypeUID)) {
            return new InvertStateProfile(callback);
        } else if (ROUND_UID.equals(profileTypeUID)) {
            return new RoundStateProfile(callback, context);
            // else if (MAP_TO_ON_TYPE_UID.equals(profileTypeUID)) {
            // return new BasicMapToOnStateProfile(callback);
        } else if (TO_PERCENT_TYPE_UID.equals(profileTypeUID)) {
            return new ToPercentStateProfile(callback, context);
            // } else if (GENERIC_COMMAND_PROFILE_TYPE_UID.equals(profileTypeUID)) {
            // return new GenericCommandTriggerProfile(callback, context);
            // } else if (TOGGLE_PLAYER_PROFILE_TYPE_UID.equals(profileTypeUID)) {
            // return new TogglePlayerTriggerProfile(callback, context);
            // } else if (TOGGLE_ROLLERSHUTTER_PROFILE_TYPE_UID.equals(profileTypeUID)) {
            // return new ToggleRollershutterTriggerProfile(callback, context);
        } else if (TOGGLE_SWITCH_PROFILE_TYPE_UID.equals(profileTypeUID)) {
            return new ToggleSwitchTriggerProfile(callback, context);
        }
        return null;
    }

    @Override
    public Collection<ProfileType> getProfileTypes(@Nullable Locale locale) {
        return SUPPORTED_PROFILE_TYPES.stream().map(p -> createLocalizedProfileType(p, locale))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Collection<ProfileTypeUID> getSupportedProfileTypeUIDs() {
        return SUPPORTED_PROFILE_TYPE_UIDS;
    }

    @Override
    public @Nullable ProfileTypeUID getSuggestedProfileTypeUID(ChannelType channelType, @Nullable String itemType) {
        return getSuggestedProfileTypeUID(channelType.getUID(), itemType);
    }

    @Override
    public @Nullable ProfileTypeUID getSuggestedProfileTypeUID(Channel channel, @Nullable String itemType) {
        return getSuggestedProfileTypeUID(channel.getChannelTypeUID(), itemType);
    }

    private ProfileType createLocalizedProfileType(ProfileType profileType, @Nullable Locale locale) {
        final LocalizedKey localizedKey = new LocalizedKey(profileType.getUID(),
                locale != null ? locale.toLanguageTag() : null);

        final ProfileType cachedlocalizedProfileType = localizedProfileTypeCache.get(localizedKey);
        if (cachedlocalizedProfileType != null) {
            return cachedlocalizedProfileType;
        }

        final ProfileType localizedProfileType = profileTypeI18nLocalizationService.createLocalizedProfileType(bundle,
                profileType, locale);
        if (localizedProfileType != null) {
            localizedProfileTypeCache.put(localizedKey, localizedProfileType);
            return localizedProfileType;
        } else {
            return profileType;
        }
    }

    private @Nullable ProfileTypeUID getSuggestedProfileTypeUID(@Nullable ChannelTypeUID channelTypeUID,
            @Nullable String itemType) {
        if (itemType != null) {
            switch (itemType) {
                case CoreItemFactory.PLAYER:
                    return TOGGLE_PLAYER_PROFILE_TYPE_UID;
                case CoreItemFactory.ROLLERSHUTTER:
                    return TOGGLE_ROLLERSHUTTER_PROFILE_TYPE_UID;
                case CoreItemFactory.SWITCH:
                    return TOGGLE_SWITCH_PROFILE_TYPE_UID;
            }
        }
        return null;
    }
}
