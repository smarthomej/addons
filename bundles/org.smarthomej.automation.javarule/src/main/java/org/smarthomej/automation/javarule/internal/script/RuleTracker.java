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

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.automation.javarule.JavaRule;

/**
 * The {@link RuleTracker} tracks rule UIDs and ensures schedulers are cancelled when the rule file is unloaded
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class RuleTracker {
    private static final Map<JavaRule, Set<String>> RULE_MAP = new ConcurrentHashMap<>();

    private RuleTracker() {
        // prevent instantiation
    }

    /**
     * add a rule to the tracker
     *
     * @param uid the UID of the rule as provided by the automationManager
     * @param rule the {@link JavaRule} instance that provides this rule
     */
    public static void addRule(String uid, JavaRule rule) {
        Objects.requireNonNull(RULE_MAP.computeIfAbsent(rule, k -> new HashSet<>())).add(uid);
    }

    /**
     * remove a rule from the tracker (last tracked UID of a {@link JavaRule} also cancels all schedulers)
     *
     * @param uid the UID of the rule
     */
    public static void removeRule(String uid) {
        Entry<JavaRule, Set<String>> entry = RULE_MAP.entrySet().stream().filter(e -> e.getValue().contains(uid))
                .findFirst().orElse(null);
        if (entry != null) {
            entry.getValue().remove(uid);
            if (entry.getValue().isEmpty()) {
                RULE_MAP.remove(entry.getKey());
                entry.getKey().futures.forEach(f -> f.cancel(true));
            }
        }
    }
}
