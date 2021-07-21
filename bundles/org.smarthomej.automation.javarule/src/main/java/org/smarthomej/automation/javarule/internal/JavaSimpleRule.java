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

import java.lang.reflect.Method;
import java.util.Map;

import org.openhab.core.automation.Action;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.rules.JavaRule;

/**
 * The {@link JavaSimpleRule} is a wrapper for {@link JavaRule} defined rules
 *
 * @author Jan N. Klug - Initial contribution
 */
public class JavaSimpleRule extends SimpleRule {
    private final Logger logger = LoggerFactory.getLogger(JavaSimpleRule.class);
    private final Method method;
    private final JavaRule rule;

    public JavaSimpleRule(JavaRule rule, Method method) {
        this.method = method;
        this.rule = rule;
    }

    @Override
    public Object execute(Action action, Map<String, ?> input) {
        try {
            if (method.getParameterCount() == 2) {
                return method.invoke(rule, action, input);
            } else if (method.getParameterCount() == 1 && Action.class.equals(method.getParameterTypes()[0])) {
                return method.invoke(rule, action);
            } else if (method.getParameterCount() == 1 && Map.class.equals(method.getParameterTypes()[0])) {
                return method.invoke(rule, input);
            } else if (method.getParameterCount() == 0) {
                return method.invoke(rule);
            } else {
                logger.warn("Parameter type mismatch in rule '{}'", getName());
                return null;
            }
        } catch (Exception e) {
            // catching exception to make sure we getInstance every error in the log
            logger.warn("Execution of rule {} failed: ", getName(), e);
            return null;
        }
    }
}
