/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.notificationsforfiretv.internal;

import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.APP;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.BKCOLOR;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.DURATION;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.FILENAME;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.FILENAME2;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.FONTSIZE;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.FORCE;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.INTERRUPT;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.MSG;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.OFFSET;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.OFFSETY;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.POSITION;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.TITLE;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.TRANSPARENCY;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.TYPE;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NotificationsForFireTVHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tom Blum - Initial contribution
 */
@NonNullByDefault
public class NotificationsForFireTVHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(NotificationsForFireTVHandler.class);

    private @Nullable NotificationsForFireTVConfiguration config;

    public NotificationsForFireTVHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        config = getConfigAs(NotificationsForFireTVConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    public boolean sendNotificationForFireTV(@Nullable String msg, @Nullable String filename,
            @Nullable String filename2) {
        try {
            // CREATE CONNECTION
            NotificationsForFireTVConnection notificationsForFireTVConnection = new NotificationsForFireTVConnection(
                    config.ip);
            // ADD FORM FIELDS
            notificationsForFireTVConnection.addFormField(TYPE, String.valueOf(config.type));
            notificationsForFireTVConnection.addFormField(TITLE, config.title);
            notificationsForFireTVConnection.addFormField(MSG, msg);
            notificationsForFireTVConnection.addFormField(DURATION, String.valueOf(config.duration));
            notificationsForFireTVConnection.addFormField(FONTSIZE, String.valueOf(config.fontsize));
            notificationsForFireTVConnection.addFormField(POSITION, String.valueOf(config.position));
            notificationsForFireTVConnection.addFormField(BKCOLOR, config.bkgcolor);
            notificationsForFireTVConnection.addFormField(TRANSPARENCY, String.valueOf(config.transparency));
            notificationsForFireTVConnection.addFormField(OFFSET, String.valueOf(config.offset));
            notificationsForFireTVConnection.addFormField(OFFSETY, String.valueOf(config.offsety));
            notificationsForFireTVConnection.addFormField(APP, config.app);
            notificationsForFireTVConnection.addFormField(FORCE, String.valueOf(config.force));
            notificationsForFireTVConnection.addFormField(INTERRUPT, String.valueOf(config.interrupt));
            // ADD FILES
            File file = new File(filename);
            if (!file.exists()) {
                throw new IOException("File doesn't exist: " + filename);
            }
            File file2 = new File(filename2);
            if (!file2.exists()) {
                throw new IOException("File doesn't exist: " + filename2);
            }
            notificationsForFireTVConnection.addFilePart(FILENAME, new File(filename));
            notificationsForFireTVConnection.addFilePart(FILENAME2, new File(filename2));
            // POST FORM
            String response = notificationsForFireTVConnection.finish();
            System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singletonList(NotificationsForFireTVThingActions.class);
    }
}
