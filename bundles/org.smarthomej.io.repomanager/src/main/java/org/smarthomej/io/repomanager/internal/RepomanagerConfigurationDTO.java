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
package org.smarthomej.io.repomanager.internal;

import java.lang.reflect.Type;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.framework.FrameworkUtil;

import com.google.gson.reflect.TypeToken;

/**
 * The {@link RepomanagerConfigurationDTO} is a DTO for the servlet
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class RepomanagerConfigurationDTO {
    public static final Type TYPE_TOKEN = new TypeToken<RepomanagerConfigurationDTO>() {
    }.getType();

    public boolean releasesEnabled = false;
    public boolean snapshotsEnabled = true;

    public List<Release> releases = List.of();
    public String snapshotVersion = "";

    @SuppressWarnings("unused")
    public String bundleVersion = FrameworkUtil.getBundle(getClass()).getVersion().toString();

    @Override
    public String toString() {
        return "RepomanagerConfiguration{releasesEnabled=" + releasesEnabled + ", snapshotsEnabled=" + snapshotsEnabled
                + ", releases=" + releases + ", snapshotVersion='" + snapshotVersion + "', bundleVersion='"
                + bundleVersion + "'}";
    }

    /**
     * The {@link Release} encapsulates a single release version and the corresponding feature repo status
     * 
     */
    public static class Release {
        public final String version;
        public final boolean enabled;

        public Release(String version, boolean enabled) {
            this.version = version;
            this.enabled = enabled;
        }

        @Override
        public String toString() {
            return "Release{" + "version='" + version + "', enabled=" + enabled + '}';
        }
    }
}
