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
package org.smarthomej.automation.javarule.internal;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.AbstractScriptEngineFactory;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smarthomej.automation.javarule.internal.compiler.CompilerService;
import org.smarthomej.automation.javarule.internal.script.JavaRuleJavaScriptEngineFactory;

/**
 * The {@link JavaRuleScriptEngineFactory} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = { ScriptEngineFactory.class }, immediate = true)
@NonNullByDefault
@SuppressWarnings("unused")
public class JavaRuleScriptEngineFactory extends AbstractScriptEngineFactory {

    private final JavaRuleJavaScriptEngineFactory factory;
    private final List<String> scriptTypes;

    @Activate
    public JavaRuleScriptEngineFactory(@Reference CompilerService compilerService) {
        this.factory = new JavaRuleJavaScriptEngineFactory(compilerService);
        this.scriptTypes = Stream.of(factory.getExtensions(), factory.getMimeTypes()).flatMap(List::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<String> getScriptTypes() {
        return scriptTypes;
    }

    @Override
    public @Nullable ScriptEngine createScriptEngine(String scriptType) {
        return scriptTypes.contains(scriptType) ? factory.getScriptEngine() : null;
    }
}
