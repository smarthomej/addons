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
package org.smarthomej.binding.deconz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.smarthomej.binding.deconz.internal.DeconzHandlerFactory;

/**
 * This class provides tests for automatic thing updates.
 * It should be moved to SAT sometime instead of implementing it in every binding
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UpdateFileTest {
    private static final Pattern VALID_LINES = Pattern
            .compile("^(\\d+;(ADD_CHANNEL|UPDATE_CHANNEL|REMOVE_CHANNEL);.*|#.*)$");

    @Test
    public void doCheck() {
        checkUpdateFiles(DeconzHandlerFactory.class);
    }

    private void checkUpdateFiles(Class<?> clazz) {
        // list all files
        String path = clazz.getProtectionDomain().getCodeSource().getLocation().getFile() + "update";
        File[] allUpdateFiles = new File(path).listFiles(pathname -> {
            if (pathname == null) {
                return false;
            }
            return pathname.getName().endsWith(".update");
        });
        if (allUpdateFiles == null) {
            // no updates
            return;
        }

        // check each file
        Arrays.stream(allUpdateFiles).forEach(file -> {
            try {
                // check each line if it matches the pattern
                Files.lines(file.toPath()).forEach(line -> assertEquals(true, VALID_LINES.matcher(line).matches(),
                        "Checking " + file.getName() + " failed: " + line));
            } catch (IOException e) {
                fail("Failed to read" + file.getName());
            }
        });
    }
}
