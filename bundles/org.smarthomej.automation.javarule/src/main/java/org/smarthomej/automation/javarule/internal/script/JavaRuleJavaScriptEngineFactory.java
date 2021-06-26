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
package org.smarthomej.automation.javarule.internal.script;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.automation.javarule.internal.compiler.CompilerService;

import ch.obermuhlner.scriptengine.java.JavaScriptEngineFactory;

/**
 * The {@link JavaRuleJavaScriptEngineFactory} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JavaRuleJavaScriptEngineFactory extends JavaScriptEngineFactory {

    private final CompilerService compilerService;

    public JavaRuleJavaScriptEngineFactory(CompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new JavaRuleScriptEngine(compilerService);
    }
}
