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
package org.smarthomej.automation.javarule;

import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.JAVARULE_THREADPOOL_NAME;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.rulesupport.shared.ScriptedAutomationManager;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.internal.script.RuleProcessor;

/**
 * The {@link JavaRule} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JavaRule implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(JavaRule.class);

    private @Nullable String engineIdentifier;
    private final List<Method> scriptLoadedMethods = new ArrayList<>();

    // direct injected fields
    public @NonNullByDefault({}) ItemRegistry itemRegistry;
    public @NonNullByDefault({}) ItemRegistry ir;
    public @NonNullByDefault({}) ThingRegistry things;
    public @NonNullByDefault({}) RuleRegistry rules;
    public @NonNullByDefault({}) Map<String, State> items;
    public @Nullable HashMap<String, String> ctx;

    // injected in setup
    public @NonNullByDefault({}) JavaRuleBusEvent events;
    public @NonNullByDefault({}) Object scriptExtension;
    public @NonNullByDefault({}) ThingActionsWrapper actions;
    public @NonNullByDefault({}) ScheduledExecutorService scheduler;

    private Map<String, Object> ruleSupport = Map.of();
    private String hashedScriptIdentifier = "";

    public final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    /**
     * Override this method in standalone scripts (e.g. UI defined rules)
     */
    public void runScript() {
        ScriptedAutomationManager automationManager = (ScriptedAutomationManager) ruleSupport.get("automationManager");
        if (automationManager == null) {
            logger.warn("Could not get automation manager, no rules added");
            return;
        }

        RuleProcessor.getSimpleRules(hashedScriptIdentifier, this, scriptLoadedMethods::add)
                .forEach(automationManager::addRule);
    }

    public void scriptLoaded(String engineIdentifier) {
        this.engineIdentifier = engineIdentifier;
        logger.trace("Script '{}' loaded, executing ScriptLoadedTriggers", engineIdentifier);

        // the ScriptLoadedTrigger is a "virtual trigger" that only exists in the script itself and bypasses the event
        // system
        for (Method method : scriptLoadedMethods) {
            try {
                switch (method.getParameterCount()) {
                    case 0:
                        method.invoke(this);
                        break;
                    case 1:
                        method.invoke(this, (Object) null);
                        break;
                    default:
                        logger.warn("Method has too many parameters: {}", method.getName());
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                logger.warn("Could not invoke method '{}' on scriptLoaded: {}", method.getName(), e.getMessage());
            }
        }
    }

    public void scriptUnloaded() {
        futures.values().forEach(f -> f.cancel(true));
        logger.trace("Script '{}' unloaded", this.engineIdentifier);
    }

    /**
     * Initialize this script, provide extensions
     */
    @SuppressWarnings("unchecked")
    public final void run() {
        try {
            // get a unique script identifier
            Field containerField = scriptExtension.getClass().getDeclaredField("container");
            containerField.setAccessible(true);
            ScriptEngineContainer container = (ScriptEngineContainer) containerField.get(scriptExtension);
            if (container == null) {
                logger.warn("Could not get scriptIdentifier");
                return;
            }
            hashedScriptIdentifier = Integer.toHexString(container.getIdentifier().hashCode());

            // get the automationManager and initialize events
            Method importPreset = scriptExtension.getClass().getDeclaredMethod("importPreset", String.class);
            ruleSupport = Objects.requireNonNullElse(
                    (Map<String, Object>) importPreset.invoke(scriptExtension, "RuleSupport"), Map.of());
            Map<String, Object> ruleDefault = Objects.requireNonNullElse(
                    (Map<String, Object>) importPreset.invoke(scriptExtension, "default"), Map.of());

            Object events = ruleDefault.get("events");
            if (events == null) {
                logger.warn("Could not initialize 'events', failing here.");
                return;
            }
            this.events = new JavaRuleBusEvent(events);

            Object actions = ruleDefault.get("actions");
            if (actions == null) {
                logger.warn("Could not initialize 'actions', failing here.");
                return;
            }
            this.actions = new ThingActionsWrapper(actions, this);

            scheduler = ThreadPoolManager.getScheduledPool(JAVARULE_THREADPOOL_NAME);
            runScript();
        } catch (NoSuchFieldException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            logger.warn("Could not load script extensions, no rules added", e);
        }
    }
}
