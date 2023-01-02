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
package org.smarthomej.automation.javarule.annotation;

import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.ANNOTATION_DEFAULT;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ItemStateUpdateTrigger}
 *
 * @author Jan N. Klug - Initial contribution
 */
@Repeatable(ItemStateUpdateTriggers.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@NonNullByDefault
public @interface ItemStateUpdateTrigger {
    String itemName();

    String state() default ANNOTATION_DEFAULT;
}
