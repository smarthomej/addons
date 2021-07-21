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
package org.smarthomej.automation.javarule.internal;

import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.EXT_LIB_DIR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.HELPER_PACKAGE;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.LIB_DIR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemAddedEvent;
import org.openhab.core.items.events.ItemRemovedEvent;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.events.ThingAddedEvent;
import org.openhab.core.thing.events.ThingRemovedEvent;
import org.openhab.core.thing.events.ThingStatusInfoChangedEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.Util;

/**
 * The {@link HelperCompiler} is a service for providing development dependencies
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = { EventSubscriber.class,
        HelperCompiler.class }, configurationPid = "automation.javarule", immediate = true)
@NonNullByDefault
@SuppressWarnings("unused")
public class HelperCompiler implements EventSubscriber {
    private static final Set<ThingStatus> INITIALIZED = Set.of(ThingStatus.ONLINE, ThingStatus.OFFLINE,
            ThingStatus.UNKNOWN);
    private static final Set<String> EVENTS = Set.of(ItemAddedEvent.TYPE, ItemRemovedEvent.TYPE, ThingAddedEvent.TYPE,
            ThingRemovedEvent.TYPE, ThingStatusInfoChangedEvent.TYPE);

    private final Logger logger = LoggerFactory.getLogger(HelperCompiler.class);

    // configuration
    private final ItemRegistry itemRegistry;
    private final BaseCompilerService compiler;

    @Activate
    public HelperCompiler(@Reference BaseCompilerService compiler, @Reference ItemRegistry itemRegistry) {
        this.compiler = compiler;
        this.itemRegistry = itemRegistry;

        generateHelpers();
        logger.debug("Java helper compiler initialized!");
    }

    private void generateItems(Path helperDir) throws IOException {
        Path itemJavaFile = helperDir.resolve("Items" + JavaRuleConstants.JAVA_FILE_TYPE);
        Path itemClassFile = helperDir.resolve("Items" + JavaRuleConstants.CLASS_FILE_TYPE);

        String allItems = itemRegistry.getItems().stream()
                .map(item -> "    public static final String " + item.getName() + " = \"" + item.getName() + "\";\n")
                .collect(Collectors.joining());

        String generatedClass = "package " + HELPER_PACKAGE + ";\n\n" //
                + "public class Items {\n" //
                + allItems //
                + "}\n";

        replaceIfNotEqual(itemJavaFile, itemClassFile, generatedClass);
    }

    private void generateThingActions(Path helperDir) throws IOException {
        List<ThingActions> thingActions = Util.findServices(ThingActions.class);

        Map<String, String> scopeToClasses = new HashMap<>();
        Map<String, Set<String>> scopeToImports = new HashMap<>();

        for (ThingActions thingAction : thingActions) {
            Class<? extends ThingActions> clazz = thingAction.getClass();
            ThingActionsScope scope = clazz.getDeclaredAnnotation(ThingActionsScope.class);
            if (scope == null) {
                continue;
            }
            String scopeName = scope.name();
            Set<String> importScope = Objects
                    .requireNonNull(scopeToImports.computeIfAbsent(scopeName, k -> new HashSet<>()));

            List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(method -> method.getDeclaredAnnotation(RuleAction.class) != null)
                    .collect(Collectors.toList());

            String className = clazz.getSimpleName();

            StringBuilder generatedJava = new StringBuilder();

            generatedJava.append("  public static class ").append(className).append(" {\n");
            generatedJava.append("    private ThingActions instance;\n\n");
            generatedJava.append("    private ").append(className)
                    .append("(ThingActions instance) { this.instance = instance; } \n\n");

            for (Method method : methods) {
                String name = method.getName();
                String returnValue = method.getGenericReturnType().getTypeName();

                Arrays.stream(method.getParameterTypes()).map(Class::getName).forEach(importScope::add);
                importScope.add(method.getReturnType().getName());

                List<String> parameters = Arrays.stream(method.getGenericParameterTypes()).map(Type::getTypeName)
                        .collect(Collectors.toList());
                List<String> exceptions = Arrays.stream(method.getGenericExceptionTypes()).map(Type::getTypeName)
                        .collect(Collectors.toList());

                logger.trace("Found method '{}' with parameters '{}' and return value '{}' and exceptions '{}'", name,
                        parameters, returnValue, exceptions);

                String methodJava = String.format("    public %s %s(%s)", returnValue, name,
                        IntStream.range(0, parameters.size()).mapToObj(i -> parameters.get(i) + " p" + i)
                                .collect(Collectors.joining(",")));
                if (!exceptions.isEmpty()) {
                    methodJava += " throws " + String.join(",", exceptions);
                }
                methodJava += " {\n";
                methodJava += "      try {\n";
                methodJava += "        " + (!"void".equals(returnValue) ? "return (" + returnValue + ") " : "")
                        + "instance.getClass().getMethod(\"" + name + "\"";
                if (!parameters.isEmpty()) {
                    methodJava += "," + parameters.stream().map(p -> p + ".class").collect(Collectors.joining(","));
                }
                methodJava += ").invoke(instance";
                if (!parameters.isEmpty()) {
                    methodJava += "," + IntStream.range(0, parameters.size()).mapToObj(i -> " p" + i)
                            .collect(Collectors.joining(","));
                }
                methodJava += ");\n";
                methodJava += "      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {\n";
                methodJava += "        throw new IllegalStateException(e);\n";
                methodJava += "      }\n";
                methodJava += "    }\n\n";

                generatedJava.append(methodJava);
            }

            generatedJava.append("    public static ").append(className).append(" get(String thingUid) {\n");
            generatedJava.append("      ThingActions action = Util.getAction(\"").append(scopeName)
                    .append("\", thingUid).orElse(null);\n");
            generatedJava.append("      return action == null ? null : new ").append(className).append("(action);\n");
            generatedJava.append("    }\n");
            generatedJava.append("  }\n\n");

            scopeToClasses.compute(scopeName, (k, v) -> v == null ? generatedJava.toString() : v + generatedJava);
        }

        for (Map.Entry<String, String> scope : scopeToClasses.entrySet()) {
            Path scopeJavaFile = helperDir.resolve(scope.getKey().toUpperCase() + JavaRuleConstants.JAVA_FILE_TYPE);
            Path scopeClassPath = helperDir.resolve(scope.getKey().toUpperCase() + JavaRuleConstants.CLASS_FILE_TYPE);

            StringBuilder generatedJava = new StringBuilder().append("package ").append(HELPER_PACKAGE).append(";\n\n");

            generatedJava.append("import org.openhab.core.thing.binding.ThingActions;\n");
            generatedJava.append("import java.lang.reflect.InvocationTargetException;\n");
            generatedJava.append("import org.smarthomej.automation.javarule.Util;\n");
            scopeToImports.getOrDefault(scope.getKey(), Set.of()).stream().filter(clazz -> !"void".equals(clazz))
                    .filter(clazz -> !clazz.startsWith("java.util")).map(clazz -> "import " + clazz + ";\n")
                    .forEach(generatedJava::append);
            generatedJava.append("\n");

            generatedJava.append("public class ").append(scope.getKey().toUpperCase()).append(" {\n\n");
            generatedJava.append("  private ").append(scope.getKey().toUpperCase()).append("() {}\n\n");
            generatedJava.append(scope.getValue());

            generatedJava.append("}\n");

            replaceIfNotEqual(scopeJavaFile, scopeClassPath, generatedJava.toString());
        }
    }

    private void replaceIfNotEqual(Path sourcePath, Path targetPath, String content) throws IOException {
        // check if the java already exists and skip generation if content is equal and class exists
        try {
            String existingClass = Files.readString(sourcePath);
            if (content.equals(existingClass) && Files.exists(targetPath)) {
                logger.debug("{} is still the same, ignoring", sourcePath);
                return;
            }
        } catch (IOException e) {
            // ignore, it's ok if the file is not existing or corrupted, we'll create or replace it
        }

        // delete old class file, if it exists and we fail, we won't be able to overwrite it later, either
        Files.deleteIfExists(targetPath);

        try (FileOutputStream outFile = new FileOutputStream(sourcePath.toFile())) {
            outFile.write(content.getBytes(StandardCharsets.UTF_8));
            logger.debug("Wrote generated class: {}", sourcePath.toAbsolutePath());
        }
    }

    private synchronized void generateHelpers() {
        try {
            Path helperDir = Files.createTempDirectory("javarule");

            generateItems(helperDir);
            generateThingActions(helperDir);

            if (!compiler.compile(helperDir, null) && Files.exists(LIB_DIR.resolve(JavaRuleConstants.HELPER_JAR))) {
                // nothing compiled, keep old jar
                return;
            }

            try (FileOutputStream outFile = new FileOutputStream(
                    EXT_LIB_DIR.resolve(JavaRuleConstants.HELPER_JAR).toFile())) {
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                JarOutputStream target = new JarOutputStream(outFile, manifest);

                try (Stream<Path> helperPath = Files.list(helperDir)) {
                    helperPath.filter(JavaRuleConstants.CLASS_FILE_Filter).forEach(f -> addClassToJar(f, target));
                }
                target.close();
            }

            logger.debug("Finished generating helper libraries.");
        } catch (IOException e) {
            logger.warn("Failed to generate class files, helper libraries not available: {}", e.getMessage());
        }
    }

    private void addClassToJar(Path path, JarOutputStream jar) {
        String name = HELPER_PACKAGE.replaceAll("\\.", "/") + "/" + path.getFileName().toString();
        try {
            logger.trace("Adding {}", path);
            File file = path.toFile();
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buffer = in.readAllBytes();

                JarEntry entry = new JarEntry(name);
                entry.setTime(file.lastModified());
                jar.putNextEntry(entry);
                jar.write(buffer, 0, buffer.length);
                jar.closeEntry();
            }
        } catch (IOException e) {
            logger.warn("Failed to add {} to jar: {}", path, e.getMessage());
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return EVENTS;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        String eventType = event.getType();
        if (ThingStatusInfoChangedEvent.TYPE.equals(eventType)) {
            ThingStatusInfoChangedEvent event1 = (ThingStatusInfoChangedEvent) event;
            if ((ThingStatus.INITIALIZING.equals(event1.getOldStatusInfo().getStatus())
                    && INITIALIZED.contains(event1.getStatusInfo().getStatus()))
                    || (ThingStatus.UNINITIALIZED.equals(event1.getStatusInfo().getStatus())
                            && INITIALIZED.contains(event1.getOldStatusInfo().getStatus()))) {
                // only regenerate jar if things are changing to or from an initialized
                generateHelpers();
            }
        } else if (EVENTS.contains(eventType)) {
            logger.debug("Added/updated: {}", event);
            generateHelpers();
        }
    }
}
