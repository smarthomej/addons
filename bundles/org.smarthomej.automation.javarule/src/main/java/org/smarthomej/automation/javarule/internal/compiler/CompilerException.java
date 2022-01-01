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
package org.smarthomej.automation.javarule.internal.compiler;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link CompilerException} is thrown by the compiler when an error occurs
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CompilerException extends Exception {
    public static final long serialVersionUID = 1L;

    public CompilerException(Throwable t) {
        super(Objects.requireNonNullElse(t.getMessage(), "null"));
    }
}
