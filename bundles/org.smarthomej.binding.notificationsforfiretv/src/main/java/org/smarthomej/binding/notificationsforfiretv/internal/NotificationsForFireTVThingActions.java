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
package org.smarthomej.binding.notificationsforfiretv.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some automation actions to be used with a {@link NotificationsForFireTVThingActions}
 *
 * @author Tom Blum - Initial contribution
 */
@Component(scope = ServiceScope.PROTOTYPE, service = NotificationsForFireTVThingActions.class)
@ThingActionsScope(name = "notificationsforfiretv")
@NonNullByDefault
public class NotificationsForFireTVThingActions implements ThingActions {

    private final Logger logger = LoggerFactory.getLogger(NotificationsForFireTVThingActions.class);

    private @Nullable NotificationsForFireTVHandler handler;

    @RuleAction(label = "Notifications For Fire TV sendNotification", description = "Action that sends notification to Fire TV")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendNotification(
            @ActionInput(name = "message") @Nullable String msg,
            @ActionInput(name = "icon") @Nullable String filename) {
        final NotificationsForFireTVHandler handler = this.handler;
        if (handler == null) {
            logger.warn("Handler is null, cannot send notification.");
            return false;
        } else {
            return handler.sendNotification(msg, filename, null);
        }
    }

    @RuleAction(label = "Notifications For Fire TV sendNotificationWithImage", description = "Action that sends notification to Fire TV")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendNotificationWithImage(
            @ActionInput(name = "message") @Nullable String msg, @ActionInput(name = "icon") @Nullable String filename,
            @ActionInput(name = "image") @Nullable String filename2) {
        final NotificationsForFireTVHandler handler = this.handler;
        if (handler == null) {
            logger.warn("Handler is null, cannot send notification with image.");
            return false;
        } else {
            return handler.sendNotification(msg, filename, filename2);
        }
    }

    public static boolean sendNotification(ThingActions actions, @Nullable String msg, @Nullable String filename) {
        return ((NotificationsForFireTVThingActions) actions).sendNotification(msg, filename);
    }

    public static boolean sendNotificationWithImage(ThingActions actions, @Nullable String msg,
            @Nullable String filename, @Nullable String filename2) {
        return ((NotificationsForFireTVThingActions) actions).sendNotificationWithImage(msg, filename, filename2);
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof NotificationsForFireTVHandler) {
            this.handler = (NotificationsForFireTVHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }
}
