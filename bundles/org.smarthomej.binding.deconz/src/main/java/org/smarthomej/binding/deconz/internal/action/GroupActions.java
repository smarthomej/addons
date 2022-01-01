/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.binding.deconz.internal.action;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.deconz.internal.dto.NewSceneResponse;
import org.smarthomej.binding.deconz.internal.handler.GroupThingHandler;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * The {@link GroupActions} provides actions for managing scenes in groups
 *
 * @author Jan N. Klug - Initial contribution
 */
@ThingActionsScope(name = "deconz")
@NonNullByDefault
public class GroupActions implements ThingActions {
    private static final String NEW_SCENE_ID_OUTPUT = "newSceneId";
    private static final Type NEW_SCENE_RESPONSE_TYPE = new TypeToken<List<NewSceneResponse>>() {
    }.getType();

    private final Logger logger = LoggerFactory.getLogger(GroupActions.class);
    private final Gson gson = new Gson();

    private @Nullable GroupThingHandler handler;

    @RuleAction(label = "create a scene", description = "Creates a new scene and returns the new scene's id")
    public @ActionOutput(name = NEW_SCENE_ID_OUTPUT, type = "java.lang.Integer") Map<String, Object> createScene(
            @ActionInput(name = "name") @Nullable String name) {
        GroupThingHandler handler = this.handler;

        if (handler == null) {
            logger.warn("Deconz GroupActions service ThingHandler is null!");
            return Map.of();
        }

        if (name == null) {
            logger.debug("Skipping scene creation due to missing scene name");
            return Map.of();
        }

        CompletableFuture<String> newSceneId = new CompletableFuture<>();
        handler.doNetwork(Map.of("name", name), "scenes", HttpMethod.POST, newSceneId::complete);

        try {
            String returnedJson = newSceneId.get(2000, TimeUnit.MILLISECONDS);
            List<NewSceneResponse> newSceneResponses = gson.fromJson(returnedJson, NEW_SCENE_RESPONSE_TYPE);
            if (newSceneResponses != null && !newSceneResponses.isEmpty()) {
                return Map.of(NEW_SCENE_ID_OUTPUT, newSceneResponses.get(0).success.id);
            }
            throw new IllegalStateException("response is empty");
        } catch (InterruptedException | ExecutionException | TimeoutException | JsonParseException
                | IllegalStateException e) {
            logger.warn("Couldn't get newSceneId", e);
            return Map.of();
        }
    }

    public static Map<String, Object> createScene(ThingActions actions, @Nullable String name) {
        if (actions instanceof GroupActions) {
            return ((GroupActions) actions).createScene(name);
        }
        return Map.of();
    }

    @RuleAction(label = "delete a scene", description = "Deletes a scene")
    public void deleteScene(@ActionInput(name = "sceneId") @Nullable Integer sceneId) {
        GroupThingHandler handler = this.handler;

        if (handler == null) {
            logger.warn("Deconz GroupActions service ThingHandler is null!");
            return;
        }

        if (sceneId == null) {
            logger.debug("Skipping scene deletion due to missing scene id");
            return;
        }

        handler.doNetwork(null, "scenes/" + sceneId, HttpMethod.DELETE, null);
    }

    public static void deleteScene(ThingActions actions, @Nullable Integer sceneId) {
        if (actions instanceof GroupActions) {
            ((GroupActions) actions).deleteScene(sceneId);
        }
    }

    @RuleAction(label = "store as scene", description = "Stores the current light state as scene")
    public void storeScene(@ActionInput(name = "sceneId") @Nullable Integer sceneId) {
        GroupThingHandler handler = this.handler;

        if (handler == null) {
            logger.warn("Deconz GroupActions service ThingHandler is null!");
            return;
        }

        if (sceneId == null) {
            logger.debug("Skipping scene storage due to missing scene id");
            return;
        }

        handler.doNetwork(null, "scenes/" + sceneId + "/store", HttpMethod.PUT, null);
    }

    public static void storeScene(ThingActions actions, @Nullable Integer sceneId) {
        if (actions instanceof GroupActions) {
            ((GroupActions) actions).storeScene(sceneId);
        }
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof GroupThingHandler) {
            this.handler = (GroupThingHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }
}
