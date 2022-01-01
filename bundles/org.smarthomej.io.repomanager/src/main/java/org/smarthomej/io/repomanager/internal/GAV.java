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
package org.smarthomej.io.repomanager.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link GAV} is responsible for storing artifact coordinates
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class GAV {
    public final String groupId;
    public final String artifactId;
    public final String version;

    public GAV(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
}
