/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.commons.itemvalueconverter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ContentWrapper} defines the pre-processed response
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ContentWrapper {
    private final byte[] rawContent;
    private final Charset encoding;
    private final @Nullable String mediaType;

    public ContentWrapper(byte[] rawContent, String encoding, @Nullable String mediaType) {
        this.rawContent = rawContent;
        this.mediaType = mediaType;

        Charset finalEncoding = StandardCharsets.UTF_8;
        try {
            finalEncoding = Charset.forName(encoding);
        } catch (IllegalArgumentException e) {
        }
        this.encoding = finalEncoding;
    }

    public byte[] getRawContent() {
        return rawContent;
    }

    public String getAsString() {
        return new String(rawContent, encoding);
    }

    public @Nullable String getMediaType() {
        return mediaType;
    }
}
