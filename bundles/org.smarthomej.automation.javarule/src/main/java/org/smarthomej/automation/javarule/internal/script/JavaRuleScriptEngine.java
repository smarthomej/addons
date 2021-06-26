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

import java.util.List;

import javax.script.ScriptException;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.smarthomej.automation.javarule.internal.compiler.CompilerException;
import org.smarthomej.automation.javarule.internal.compiler.CompilerService;

import ch.obermuhlner.scriptengine.java.JavaCompiledScript;
import ch.obermuhlner.scriptengine.java.JavaRuleCompiledScript;
import ch.obermuhlner.scriptengine.java.JavaScriptEngine;
import ch.obermuhlner.scriptengine.java.MemoryFileManager;
import ch.obermuhlner.scriptengine.java.name.DefaultNameStrategy;
import ch.obermuhlner.scriptengine.java.name.NameStrategy;

/**
 * The {@link JavaRuleScriptEngine} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JavaRuleScriptEngine extends JavaScriptEngine {

    private final CompilerService compilerService;

    public JavaRuleScriptEngine(CompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @Override
    public JavaCompiledScript compile(@Nullable String script) throws ScriptException {
        try {
            MemoryFileManager fileManager = compilerService.getMemoryFileManager();
            String fullClassName = new DefaultNameStrategy().getFullName(script);
            String simpleClassName = NameStrategy.extractSimpleName(fullClassName);
            JavaFileObject scriptSource = fileManager.createSourceFileObject(null, simpleClassName, script);
            compilerService.compile(List.of(scriptSource), fileManager, null);
            Class<?> clazz = fileManager.getClassLoader(StandardLocation.CLASS_OUTPUT).loadClass(fullClassName);
            return new JavaRuleCompiledScript(this, clazz);
        } catch (CompilerException | ClassNotFoundException e) {
            throw new ScriptException(e);
        }
    }
}
