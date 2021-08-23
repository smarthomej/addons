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

import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.JAVARULE_DEPENDENCY_JAR;

import java.util.List;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptException;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptDependencyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.JavaRule;
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
public class JavaRuleScriptEngine extends JavaScriptEngine implements Invocable {
    private final Logger logger = LoggerFactory.getLogger(JavaRuleScriptEngine.class);

    private final CompilerService compilerService;
    private @Nullable JavaRuleCompiledScript compiledScript;

    public JavaRuleScriptEngine(CompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @Override
    public @Nullable Object eval(@Nullable String script, @Nullable Bindings bindings) throws ScriptException {
        if (bindings != null) {
            ScriptDependencyListener depListener = (ScriptDependencyListener) bindings.get("oh.dependency-listener");
            depListener.accept(JAVARULE_DEPENDENCY_JAR.toString());
        }

        JavaRuleCompiledScript compiledScript = (JavaRuleCompiledScript) this.compile(script);
        compiledScript.eval(bindings);
        this.compiledScript = compiledScript;
        return compiledScript;
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

    @Override
    public @Nullable Object invokeMethod(@Nullable Object o, @Nullable String name, Object @Nullable... args)
            throws NoSuchMethodException {
        throw new NoSuchMethodException("not implemented");
    }

    @Override
    public @Nullable Object invokeFunction(@Nullable String name, Object @Nullable... args) throws ScriptException {
        JavaRuleCompiledScript compiledScript = this.compiledScript;
        if (compiledScript == null || name == null) {
            return null;
        }
        JavaRule instance = (JavaRule) compiledScript.getCompiledInstance();
        switch (name) {
            case "scriptLoaded":
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String engineIdentifier = (String) args[0];
                    instance.scriptLoaded(engineIdentifier);
                } else {
                    logger.debug("{} incompatible with required parameter String", args);
                }
                break;
            case "scriptUnloaded":
                instance.scriptUnloaded();
                break;
            default:
                throw new ScriptException(name + " is not an allowed method in JavaRule");
        }

        return null;
    }

    @Override
    @SuppressWarnings("null")
    public <T> T getInterface(@Nullable Class<T> clazz) {
        return null;
    }

    @Override
    @SuppressWarnings("null")
    public <T> T getInterface(@Nullable Object o, @Nullable Class<T> clazz) {
        return null;
    }
}
