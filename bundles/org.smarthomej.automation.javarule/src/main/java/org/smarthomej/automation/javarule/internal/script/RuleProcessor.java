/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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

import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.ANNOTATION_DEFAULT;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.CONDITION_FROM_ANNOTATION;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.TRIGGER_FROM_ANNOTATION;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.TriggerBuilder;
import org.openhab.core.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.JavaRule;
import org.smarthomej.automation.javarule.annotation.GenericAutomationTrigger;
import org.smarthomej.automation.javarule.annotation.Rule;
import org.smarthomej.automation.javarule.annotation.ScriptLoadedTrigger;

/**
 * The {@link RuleProcessor} is responsible for processing scripts
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class RuleProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleProcessor.class);

    private RuleProcessor() {
        // prevent instantiation
    }

    /**
     * Get the rules in a {@link JavaRule} script
     **
     * @return a list of {@link SimpleRule}s that correspond to the rules contained in the argument class
     */
    public static List<SimpleRule> getSimpleRules(String scriptIdentifier, JavaRule script,
            Consumer<Method> scriptLoadedMethods) {
        List<SimpleRule> rules = new ArrayList<>();

        for (Method method : script.getClass().getDeclaredMethods()) {
            Rule ruleAnnotation = method.getDeclaredAnnotation(Rule.class);
            if (ruleAnnotation == null) {
                LOGGER.debug("Method '{}' ignored since @Rule annotation is missing.", method.getName());
                continue;
            }

            String ruleName = method.getName();
            String ruleDescription = ruleAnnotation.name();
            Set<String> ruleTags = Set.of(ruleAnnotation.tags());

            if (ruleDescription.isBlank() || ANNOTATION_DEFAULT.equals(ruleDescription)) {
                ruleDescription = script.getClass().getSimpleName() + "/" + method.getName();
            }

            if (ruleAnnotation.disabled()) {
                LOGGER.info("Ignoring rule '{}', disabled", ruleAnnotation.name());
                continue;
            }

            String ruleUID = scriptIdentifier + "-" + script.getClass().getSimpleName() + "-" + method.getName();

            List<Trigger> triggers = new ArrayList<>();
            TRIGGER_FROM_ANNOTATION
                    .entrySet().stream().map(annotation -> getModuleForAnnotation(method, annotation.getKey(),
                            annotation.getValue(), ModuleBuilder::createTrigger))
                    .flatMap(Collection::stream).forEach(triggers::add);
            Arrays.stream(method.getDeclaredAnnotationsByType(GenericAutomationTrigger.class))
                    .map(annotation -> getGenericAutomationTrigger(annotation, ruleUID)).forEach(triggers::add);

            List<Condition> conditions = CONDITION_FROM_ANNOTATION.entrySet().stream()
                    .map(annotationClazz -> getModuleForAnnotation(method, annotationClazz.getKey(),
                            annotationClazz.getValue(), ModuleBuilder::createCondition))
                    .flatMap(Collection::stream).collect(Collectors.toList());

            LOGGER.debug("Added {} trigger(s) and {} condition(s) for rule '{}'", triggers.size(), conditions.size(),
                    ruleUID);

            SimpleRule simpleRule = new SimpleRule() {
                @Override
                public Object execute(Action module, Map<String, ?> input) {
                    try {
                        Object returnValue = method.getParameterCount() == 1 ? method.invoke(script, input)
                                : method.invoke(script);
                        return Objects.requireNonNullElse(returnValue, Map.of());
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
            simpleRule.setName(ruleName);
            simpleRule.setDescription(ruleDescription);
            simpleRule.setTriggers(triggers);
            simpleRule.setConditions(conditions);
            simpleRule.setTags(ruleTags);

            rules.add(simpleRule);

            // check if ScriptLoadedTrigger is present
            if (method.getDeclaredAnnotation(ScriptLoadedTrigger.class) != null) {
                scriptLoadedMethods.accept(method);
            }
        }

        return rules;
    }

    private static Trigger getGenericAutomationTrigger(GenericAutomationTrigger annotation, String ruleUID) {
        String typeUid = annotation.typeUid();
        Configuration configuration = new Configuration();
        for (String param : annotation.params()) {
            String[] parts = param.split("=");
            if (parts.length != 2) {
                LOGGER.warn("Ignoring '{}' in trigger for '{}', can not determine key and value", param, ruleUID);
                continue;
            }
            configuration.put(parts[0], parts[1]);
        }
        return TriggerBuilder.create().withTypeUID(typeUid).withId("").withConfiguration(configuration).build();
    }

    private static <T extends Annotation, R extends Module> List<R> getModuleForAnnotation(Method method,
            Class<T> clazz, String typeUid, Supplier<ModuleBuilder<?, R>> builder) {
        T[] annotations = method.getDeclaredAnnotationsByType(clazz);
        return Arrays.stream(annotations)
                .map(annotation -> builder.get().withId("").withTypeUID(typeUid)
                        .withConfiguration(getAnnotationConfiguration(annotation)).build())
                .collect(Collectors.toList());
    }

    private static Configuration getAnnotationConfiguration(Annotation annotation) {
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
                    } else if (parameterValue instanceof Integer) {
                        configuration.put(method.getName(), BigDecimal.valueOf((Integer) parameterValue));
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
}
