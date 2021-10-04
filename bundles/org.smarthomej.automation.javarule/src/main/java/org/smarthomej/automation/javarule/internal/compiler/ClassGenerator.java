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
package org.smarthomej.automation.javarule.internal.compiler;

import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.HELPER_PACKAGE;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.internal.JavaRuleConstants;

/**
 * The {@link ClassGenerator} is responsible for generating the additional classes for rule development
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ClassGenerator {
    private final Logger logger = LoggerFactory.getLogger(ClassGenerator.class);

    private final Map<String, String> scopeClasses = new HashMap<>();

    private final Path folder;
    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;
    private final BundleContext bundleContext;

    public ClassGenerator(Path folder, ItemRegistry itemRegistry, ThingRegistry thingRegistry,
            BundleContext bundleContext) {
        this.folder = folder;
        this.itemRegistry = itemRegistry;
        this.thingRegistry = thingRegistry;
        this.bundleContext = bundleContext;
    }

    public boolean generateThingActions() throws IOException {
        List<ThingActions> thingActions;
        try {
            Set<Class<?>> classes = new HashSet<>();
            thingActions = bundleContext.getServiceReferences(ThingActions.class, null).stream()
                    .map(bundleContext::getService).filter(sr -> classes.add(sr.getClass()))
                    .collect(Collectors.toList());
        } catch (InvalidSyntaxException e) {
            logger.warn("Failed to get thing actions: {}", e.getMessage());
            return false;
        }

        Set<String> scopes = new HashSet<>();

        boolean changed = false;

        for (ThingActions thingAction : thingActions) {
            Class<? extends ThingActions> clazz = thingAction.getClass();

            String packageName = clazz.getPackageName();
            String simpleClassName = clazz.getSimpleName();

            ThingActionsScope scope = clazz.getAnnotation(ThingActionsScope.class);
            if (scope == null) {
                logger.warn("Found ThingActions class '{}' but no scope, ignoring", clazz.getName());
                continue;
            }
            scopes.add(scope.name());

            boolean isPresent = true;

            try {
                // check if we can load the class from our context classLoader
                @SuppressWarnings("unused")
                Class<?> checkClazz = Class.forName(clazz.getName());
            } catch (ClassNotFoundException e) {
                isPresent = false;
            }

            if (isPresent) {
                logger.trace("Class '{}' in package '{}' is available fro OSGi classloader, skipping.", simpleClassName,
                        packageName);
                continue;
            }

            logger.trace("Processing class '{}' in package '{}'", simpleClassName, packageName);

            List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(method -> method.getDeclaredAnnotation(RuleAction.class) != null)
                    .collect(Collectors.toList());

            Set<Class<?>> imports = new HashSet<>();
            List<String> methodSignatures = new ArrayList<>();

            for (Method method : methods) {
                String name = method.getName();
                String returnValue = method.getGenericReturnType().getTypeName();

                imports.addAll(Arrays.asList(method.getParameterTypes()));
                imports.add(method.getReturnType());

                List<String> parameters = Arrays.stream(method.getGenericParameterTypes()).map(this::typeToParameter)
                        .collect(Collectors.toList());

                logger.trace("Found method '{}' with parameters '{}' and return value '{}'.", name, parameters,
                        returnValue);

                String methodJava = String.format("    public %s %s(%s);\n", returnValue, name,
                        IntStream.range(0, parameters.size()).mapToObj(i -> parameters.get(i) + " p" + i)
                                .collect(Collectors.joining(",")));

                methodSignatures.add(methodJava);
            }

            StringBuilder generatedInterface = new StringBuilder();
            generatedInterface.append("package ").append(packageName).append(";\n\n");
            generatedInterface.append("import org.openhab.core.thing.binding.ThingActions;\n");
            imports.stream().map(this::classToImport).filter(s -> !s.isEmpty())
                    .map(importClazz -> "import " + importClazz + ";\n").forEach(generatedInterface::append);

            generatedInterface.append("\n");
            generatedInterface.append("public interface ").append(simpleClassName).append(" extends ThingActions {\n");
            methodSignatures.forEach(generatedInterface::append);
            generatedInterface.append("}\n");

            String generatedClass = generatedInterface.toString();
            String fullClassName = clazz.getName();
            Path scopeJavaFile = folder.resolve(fullClassName + JavaRuleConstants.JAVA_FILE_TYPE);

            if (replaceIfNotEqual(scopeJavaFile, fullClassName, generatedClass)) {
                changed = true;
            }
        }

        Path scopeJavaFile = folder.resolve(HELPER_PACKAGE + ".Scopes" + JavaRuleConstants.JAVA_FILE_TYPE);
        String allScopes = scopes.stream()
                .map(scope -> "    public static final String " + scope.toUpperCase() + " = \"" + scope + "\";\n")
                .collect(Collectors.joining());
        String generatedClass = "package " + HELPER_PACKAGE + ";\n\n" //
                + "public class Scopes {\n" //
                + allScopes //
                + "}\n";

        if (replaceIfNotEqual(scopeJavaFile, HELPER_PACKAGE + ".Scopes", generatedClass)) {
            changed = true;
        }

        return changed;
    }

    private boolean replaceIfNotEqual(Path scopeJavaFile, String fullClassName, String generatedClass)
            throws IOException {
        if (!generatedClass.equals(scopeClasses.put(fullClassName, generatedClass))) {
            // the class has changed or is a new one
            try (FileOutputStream outFile = new FileOutputStream(scopeJavaFile.toFile())) {
                outFile.write(generatedClass.getBytes(StandardCharsets.UTF_8));
                logger.debug("Wrote generated class: {}", scopeJavaFile.toAbsolutePath());
            }
            return true;
        } else {
            logger.debug("{} has not changed.", scopeJavaFile.toAbsolutePath());
            return false;
        }
    }

    private String classToImport(Class<?> clazz) {
        if ((clazz.isPrimitive())
                || (clazz.isArray() && Objects.requireNonNull(clazz.getComponentType()).isPrimitive())) {
            return "";
        }
        if (clazz.isArray()) {
            return Objects.requireNonNull(clazz.getComponentType()).getName();
        } else {
            return clazz.getName();
        }
    }

    private String typeToParameter(Type type) {
        return type instanceof GenericArrayType
                ? Objects.requireNonNull(((GenericArrayType) type).getGenericComponentType()).getTypeName() + "[]"
                : type.getTypeName();
    }

    public void generateItems() throws IOException {
        Collection<Item> items = itemRegistry.getItems();

        Path itemJavaFile = folder.resolve(HELPER_PACKAGE + ".Items" + JavaRuleConstants.JAVA_FILE_TYPE);

        String allItems = items.stream()
                .map(item -> "    public static final String " + item.getName() + " = \"" + item.getName() + "\";\n")
                .collect(Collectors.joining());

        String generatedClass = "package " + HELPER_PACKAGE + ";\n\n" //
                + "public class Items {\n" //
                + allItems //
                + "}\n";

        try (FileOutputStream outFile = new FileOutputStream(itemJavaFile.toFile())) {
            outFile.write(generatedClass.getBytes(StandardCharsets.UTF_8));
            logger.debug("Wrote generated class: {}", itemJavaFile.toAbsolutePath());
        }
    }

    public void generateThings() throws IOException {
        Collection<Thing> things = thingRegistry.getAll();

        Path thingJavaFile = folder.resolve(HELPER_PACKAGE + ".Things" + JavaRuleConstants.JAVA_FILE_TYPE);

        String allThings = things.stream()
                .map(thing -> "    public static final String "
                        + thing.getUID().toString().replace(":", "_").replace("-", "_") + " = \"" + thing.getUID()
                        + "\";\n")
                .collect(Collectors.joining());

        String generatedClass = "package " + HELPER_PACKAGE + ";\n\n" //
                + "public class Things {\n" //
                + allThings //
                + "}\n";

        try (FileOutputStream outFile = new FileOutputStream(thingJavaFile.toFile())) {
            outFile.write(generatedClass.getBytes(StandardCharsets.UTF_8));
            logger.debug("Wrote generated class: {}", thingJavaFile.toAbsolutePath());
        }
    }
}
