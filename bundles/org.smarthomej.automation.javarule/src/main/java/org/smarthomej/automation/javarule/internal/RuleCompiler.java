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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.ANNOTATION_DEFAULT;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.CONDITION_FROM_ANNOTATION;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.DEPENDENCY_JAR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.EXT_LIB_DIR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.HELPER_JAR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.JAVA_CLASS_PATH_PROPERTY;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.LIB_DIR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.RULES_DIR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.TRIGGER_FROM_ANNOTATION;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.WORKING_DIR;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.openhab.core.automation.module.script.rulesupport.shared.ScriptedAutomationManager;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.service.AbstractWatchService;
import org.openhab.core.thing.ThingRegistry;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.Util;
import org.smarthomej.automation.javarule.rules.JavaRule;
import org.smarthomej.automation.javarule.rules.annotation.Rule;

/**
 * The {@link RuleCompiler} is responsible for compiling and managing rules
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = { RuleCompiler.class }, configurationPid = "automation.javarule", immediate = true)
@NonNullByDefault
public class RuleCompiler extends AbstractWatchService {
    private static final String INIT_DELAY_PROPERTY = "initDelay";
    private static final int DEFAULT_INIT_DELAY = 5;

    private final Logger logger = LoggerFactory.getLogger(RuleCompiler.class);

    // rule management
    private String rulesClassPath = "";
    private final Map<String, RuleContext> ruleContexts = new HashMap<>();

    // injected services
    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;
    private final EventPublisher eventPublisher;
    private final BaseCompilerService compiler;

    private @Nullable ScheduledFuture<?> initFuture;

    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);
    private List<URI> rulesClassPathUris = List.of();

    @Activate
    public RuleCompiler(@Reference BaseCompilerService compiler, @Reference ItemRegistry itemRegistry,
            @Reference ThingRegistry thingRegistry, @Reference EventPublisher eventPublisher,
            Map<String, Object> properties) {
        super(WORKING_DIR.toString());

        this.compiler = compiler;
        this.itemRegistry = itemRegistry;
        this.thingRegistry = thingRegistry;
        this.eventPublisher = eventPublisher;

        if (!(Util.checkFolder(EXT_LIB_DIR) && Util.checkFolder(RULES_DIR))) {
            throw new IllegalStateException("Failed to initialize folders");
        }

        int initDelay = (Integer) properties.getOrDefault(INIT_DELAY_PROPERTY, DEFAULT_INIT_DELAY);
        initFuture = scheduler.schedule(() -> {
            logger.info("Initializing Java Rules Engine");
            initialize();
            this.initFuture = null;
        }, initDelay, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unused")
    @Deactivate
    public void deactivate() {
        ScheduledFuture<?> initFuture = this.initFuture;
        if (initFuture != null) {
            initFuture.cancel(true);
            this.initFuture = null;
        }

        ruleContexts.values().forEach(RuleContext::dispose);
        ruleContexts.clear();

        logger.debug("Java rule compiler deactivated.");
    }

    private void initialize() {
        // create classpath && classloader
        List<String> classPathFragments = new ArrayList<>();
        List<URI> uriList = new ArrayList<>();

        classPathFragments.add(Objects.requireNonNull(System.getProperty(JAVA_CLASS_PATH_PROPERTY)));
        classPathFragments.add(EXT_LIB_DIR.resolve(HELPER_JAR).toString());
        classPathFragments.add(LIB_DIR.resolve(DEPENDENCY_JAR).toString());

        uriList.add(EXT_LIB_DIR.resolve(HELPER_JAR).toUri());
        uriList.add(WORKING_DIR.resolve(JavaRuleConstants.RULES_DIR_START).toUri());
        //
        try (Stream<Path> extLibStream = Files.list(EXT_LIB_DIR)) {
            extLibStream.forEach(path -> {
                classPathFragments.add(path.toString());
                uriList.add(path.toUri());
            });
        } catch (IOException e) {
            logger.warn("Failed to add external libraries: {}", e.getMessage());
        }

        rulesClassPath = String.join(File.pathSeparator, classPathFragments);
        rulesClassPathUris = uriList;

        reloadRules();

        logger.debug("Java rule compiler initialized!");
    }

    private void initializeRule(Path classfile) {
        logger.debug("Loading instance for class: {}", classfile.getFileName());
        String classFileName = classfile.getFileName().toString();
        String scriptIdentifier = Util.removeExtension(classfile.toString());

        RuleContext ruleContext = getRuleContext(scriptIdentifier);
        if (ruleContext == null) {
            logger.warn("Could not create RuleContext for '{}'", scriptIdentifier);
            return;
        }
        ruleContexts.put(scriptIdentifier, ruleContext);

        try {
            Class<?> loadedClass = ruleContext.classLoader
                    .loadClass(JavaRuleConstants.RULES_PACKAGE + Util.removeExtension(classFileName));
            logger.debug("Loaded class '{}'", classFileName);

            if (Modifier.isAbstract(loadedClass.getModifiers())) {
                logger.debug("Not creating an instance of abstract class: {}", classFileName);
                return;
            }
            try {
                final Object obj = loadedClass.getDeclaredConstructor().newInstance();
                if (obj instanceof JavaRule) {
                    JavaRule rule = (JavaRule) obj;
                    rule.setExecutionContext(eventPublisher, itemRegistry, thingRegistry, scheduler);
                    getSimpleRules(rule).forEach(ruleContext.automationManager::addRule);
                }
                logger.debug("Created instance: {} obj: {}", classFileName, obj);
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                    | InvocationTargetException e) {
                logger.warn("Could not create create instance using default constructor: {}", classFileName);
            }
        } catch (ClassNotFoundException | IllegalArgumentException | SecurityException e) {
            logger.warn("Could not load class '{}': {}", classfile, e.getMessage());
        }
    }

    private void reloadRules() {
        logger.debug("Reloading all rules");
        ruleContexts.values().forEach(RuleContext::dispose);
        ruleContexts.clear();

        compiler.compile(RULES_DIR, rulesClassPath);

        try (Stream<Path> ruleFileStream = Files.list(RULES_DIR)) {
            List<Path> classFiles = ruleFileStream.filter(JavaRuleConstants.CLASS_FILE_Filter)
                    .collect(Collectors.toList());
            logger.info("Number of Java Rules classes to load from '{}' to memory: {}", RULES_DIR, classFiles.size());

            classFiles.forEach(this::initializeRule);
        } catch (IOException e) {
            logger.warn("Could not load rules classes: {}", e.getMessage());
        }
    }

    private void reloadRule(Path path) {
        logger.debug("Reloading rule {}", path);
        File javaFile = path.toFile();

        // reset automation manager if it exists
        String scriptIdentifier = Util.removeExtension(javaFile.getAbsolutePath());
        ruleContexts.computeIfPresent(scriptIdentifier, (k, v) -> {
            v.dispose();
            return null;
        });

        compiler.compile(path, rulesClassPath);
        initializeRule(Path.of(scriptIdentifier + JavaRuleConstants.CLASS_FILE_TYPE));
    }

    @Override
    public boolean watchSubDirectories() {
        return true;
    }

    @Override
    public WatchEvent.Kind<?>[] getWatchEventKinds(@Nullable Path directory) {
        return new WatchEvent.Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    public void processWatchEvent(@Nullable WatchEvent<?> event, WatchEvent.@Nullable Kind<?> kind,
            @Nullable Path path) {
        if (path == null || kind == null || (kind != ENTRY_CREATE && kind != ENTRY_MODIFY && kind != ENTRY_DELETE)) {
            logger.trace("Received '{}' for path '{}' - ignoring (null or wrong kind)", kind, path);
            return;
        }
        if (!path.getFileName().toString().endsWith(JavaRuleConstants.JAVA_FILE_TYPE)) {
            logger.trace("Received '{}' for path '{}' - ignoring (wrong extension)", kind, path);
            return;
        }
        logger.debug("Received '{}' for path '{}' - notifying listeners", kind, path);

        if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind) || StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {
            reloadRule(path);
            logger.debug("(Re-)loaded rule file '{}'", path);
        } else {
            // must be delete!
            try {
                logger.debug("Removing class file and rules for '{}'", path);
                String scriptIdentifier = Util.removeExtension(path.toAbsolutePath().toString());
                ruleContexts.computeIfPresent(scriptIdentifier, (k, v) -> {
                    v.dispose();
                    return null;
                });
                Files.deleteIfExists(Path.of(scriptIdentifier + JavaRuleConstants.CLASS_FILE_TYPE));
            } catch (IOException e) {
                logger.warn("Failed to remove class file for deleted java file '{}'", path);
            }
        }
    }

    private @Nullable RuleContext getRuleContext(String scriptIdentifier) {
        Optional<ScriptedAutomationManager> automationManager = Util.findServices(ScriptExtensionProvider.class)
                .stream()
                .map(provider -> provider.importPreset(scriptIdentifier, "RuleSupport").get("automationManager"))
                .filter(Objects::nonNull).map(ScriptedAutomationManager.class::cast).findAny();
        if (automationManager.isEmpty()) {
            return null;
        }

        ClassLoader classLoader = FrameworkUtil.getBundle(JavaRule.class).adapt(BundleWiring.class).getClassLoader();

        // we need a new classloader each time we re-load a JavaRule file, because it is not possible to unload a single
        // class
        return new RuleContext(automationManager.get(), new URIClassLoader(rulesClassPathUris, classLoader));
    }

    /**
     * Get the rules in a {@link JavaRule} class
     **
     * @return a list of {@link JavaSimpleRule}s that correspond to the rules contained in the argument class
     */
    private List<JavaSimpleRule> getSimpleRules(JavaRule javaRule) {
        List<JavaSimpleRule> rules = new ArrayList<>();

        for (Method method : javaRule.getClass().getDeclaredMethods()) {
            Rule ruleAnnotation = method.getDeclaredAnnotation(Rule.class);
            if (ruleAnnotation == null || ruleAnnotation.name().isBlank()) {
                logger.debug("Rule method ignored since RuleName annotation is missing: {}", method.getName());
                continue;
            }
            String ruleName = ruleAnnotation.name();

            if (ruleAnnotation.disabled()) {
                logger.info("Ignoring rule '{}', disabled", ruleAnnotation.name());
            }

            String ruleUID = (this.getClass().getName() + "." + method.getName()).replaceAll("\\.", "_");

            List<Trigger> triggers = TRIGGER_FROM_ANNOTATION.entrySet().stream()
                    .map(annotation -> getModuleForAnnotation(method, annotation.getKey(), annotation.getValue(),
                            ruleUID, ModuleBuilder::createTrigger))
                    .flatMap(Collection::stream).collect(Collectors.toList());
            List<Condition> conditions = CONDITION_FROM_ANNOTATION.entrySet().stream()
                    .map(annotationClazz -> getModuleForAnnotation(method, annotationClazz.getKey(),
                            annotationClazz.getValue(), ruleUID, ModuleBuilder::createCondition))
                    .flatMap(Collection::stream).collect(Collectors.toList());
            logger.debug("Added {} trigger(s) and {} condition(s) for rule '{}'", triggers.size(), conditions.size(),
                    ruleUID);

            JavaSimpleRule javaSimpleRule = new JavaSimpleRule(javaRule, method);
            javaSimpleRule.setName(ruleName);
            javaSimpleRule.setDescription(ruleName);
            javaSimpleRule.setTriggers(triggers);
            javaSimpleRule.setConditions(conditions);

            rules.add(javaSimpleRule);
        }

        return rules;
    }

    private <T extends Annotation, R extends Module> List<R> getModuleForAnnotation(Method method, Class<T> clazz,
            String typeUid, String ruleUid, Supplier<ModuleBuilder<?, R>> builder) {
        T[] annotations = method.getDeclaredAnnotationsByType(clazz);
        return IntStream.range(0, annotations.length)
                .mapToObj(i -> builder.get()
                        .withId(ruleUid + "_" + annotations[i].annotationType().getSimpleName() + "_" + i)
                        .withTypeUID(typeUid).withConfiguration(getAnnotationConfiguration(annotations[i])).build())
                .collect(Collectors.toList());
    }

    private Configuration getAnnotationConfiguration(Annotation annotation) {
        Map<String, Object> configuration = new HashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            try {
                if (method.getParameterCount() == 0) {
                    Object parameterValue = method.invoke(annotation);
                    if (parameterValue == null || ANNOTATION_DEFAULT.equals(parameterValue)) {
                        continue;
                    }
                    if (parameterValue instanceof String[]) {
                        configuration.put(method.getName(), Arrays.asList((String[]) parameterValue));
                    } else {
                        configuration.put(method.getName(), parameterValue);
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                // ignore private fields
            }
        }
        return new Configuration(configuration);
    }

    private static class RuleContext {
        public final ScriptedAutomationManager automationManager;
        public final ClassLoader classLoader;

        public RuleContext(ScriptedAutomationManager automationManager, ClassLoader classLoader) {
            this.automationManager = automationManager;
            this.classLoader = classLoader;
        }

        public void dispose() {
            automationManager.removeAll();
        }
    }
}
