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
package org.smarthomej.binding.basicprofiles.internal.config;

import java.net.URI;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.thing.profiles.Profile;
import org.osgi.service.component.annotations.Component;
import org.smarthomej.binding.basicprofiles.internal.profiles.TimestampOffsetProfile;

/**
 * A {@link ConfigOptionProvider} that provides a options for @link {@link Profile}s.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class BasicProfilesConfigOptionsProvider implements ConfigOptionProvider {

    private static final String TIMESTAMP_OFFSET_CONFIG_URI = "profile:basic-profiles:timestamp-offset";

    private static final String NO_OFFSET_FORMAT = "(GMT) %s";
    private static final String NEGATIVE_OFFSET_FORMAT = "(GMT%d:%02d) %s";
    private static final String POSITIVE_OFFSET_FORMAT = "(GMT+%d:%02d) %s";

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        switch (uri.toString()) {
            case TIMESTAMP_OFFSET_CONFIG_URI:
                return TimestampOffsetProfile.TIMEZONE_PARAM.equals(param) ? processTimeZoneParam() : null;
            default:
                return null;
        }
    }

    private Collection<ParameterOption> processTimeZoneParam() {
        final Comparator<TimeZone> byOffset = (t1, t2) -> {
            return t1.getRawOffset() - t2.getRawOffset();
        };
        final Comparator<TimeZone> byID = (t1, t2) -> {
            return t1.getID().compareTo(t2.getID());
        };
        return ZoneId.getAvailableZoneIds().stream().map(TimeZone::getTimeZone).sorted(byOffset.thenComparing(byID))
                .map(tz -> {
                    return new ParameterOption(tz.getID(), getTimeZoneRepresentation(tz));
                }).collect(Collectors.toList());
    }

    private static String getTimeZoneRepresentation(TimeZone tz) {
        long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset()) - TimeUnit.HOURS.toMinutes(hours);
        minutes = Math.abs(minutes);
        final String result;
        if (hours > 0) {
            result = String.format(POSITIVE_OFFSET_FORMAT, hours, minutes, tz.getID());
        } else if (hours < 0) {
            result = String.format(NEGATIVE_OFFSET_FORMAT, hours, minutes, tz.getID());
        } else {
            result = String.format(NO_OFFSET_FORMAT, tz.getID());
        }
        return result;
    }
}
