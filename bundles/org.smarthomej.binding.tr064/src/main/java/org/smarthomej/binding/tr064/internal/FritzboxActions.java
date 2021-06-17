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
package org.smarthomej.binding.tr064.internal;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.xml.soap.SOAPMessage;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.api.ContentResponse;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.tr064.internal.dto.scpd.root.SCPDServiceType;
import org.smarthomej.binding.tr064.internal.soap.SOAPRequest;
import org.smarthomej.binding.tr064.internal.util.SCPDUtil;
import org.smarthomej.binding.tr064.internal.util.Util;

/**
 * The {@link FritzboxActions} provides actions for managing scenes in groups
 *
 * @author Jan N. Klug - Initial contribution
 */
@ThingActionsScope(name = "tr065")
@NonNullByDefault
public class FritzboxActions implements ThingActions {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy_HHmm");

    private final Logger logger = LoggerFactory.getLogger(FritzboxActions.class);

    private @Nullable Tr064RootHandler handler;

    @RuleAction(label = "create configuration backup", description = "Creates a configuration backup")
    public void getConfigurationBackup() {
        Tr064RootHandler handler = this.handler;

        if (handler == null) {
            logger.warn("TR064 action service ThingHandler is null!");
            return;
        }

        SCPDUtil scpdUtil = handler.getSCPDUtil();
        if (scpdUtil == null) {
            logger.warn("Could not get SCPDUtil, handler seems to be uninitialized.");
            return;
        }

        Optional<SCPDServiceType> scpdService = scpdUtil.getDevice("")
                .flatMap(deviceType -> deviceType.getServiceList().stream().filter(
                        service -> service.getServiceId().equals("urn:DeviceConfig-com:serviceId:DeviceConfig1"))
                        .findFirst());
        if (!scpdService.isPresent()) {
            logger.warn("Could not get service.");
        }

        BackupConfiguration configuration = handler.getBackupConfiguration();
        try {
            SOAPRequest soapRequest = new SOAPRequest(scpdService.get(), "X_AVM-DE_GetConfigFile",
                    Map.of("NewX_AVM-DE_Password", configuration.password));
            SOAPMessage soapMessage = handler.getSOAPConnector().doSOAPRequestUncached(soapRequest);
            String configBackupURL = Util.getSOAPElement(soapMessage, "NewX_AVM-DE_ConfigFileUrl")
                    .orElseThrow(() -> new Tr064CommunicationException("Empty URL"));

            ContentResponse content = handler.getUrl(configBackupURL);

            String fileName = String.format("%s %s.export", handler.getFriendlyName(),
                    dateTimeFormatter.format(LocalDateTime.now()));
            Path filePath = FileSystems.getDefault().getPath(configuration.directory, fileName);
            Path folder = filePath.getParent();
            if (folder != null) {
                Files.createDirectories(folder);
            }
            Files.write(filePath, content.getContent());
        } catch (Tr064CommunicationException e) {
            logger.warn("Failed to get configuration backup URL: {}", e.getMessage());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warn("Failed to get remote backup file: {}", e.getMessage());
        } catch (IOException e) {
            logger.warn("Failed to create backup file: {}", e.getMessage());
        }
    }

    public static void getConfigurationBackup(ThingActions actions) {
        if (actions instanceof FritzboxActions) {
            ((FritzboxActions) actions).getConfigurationBackup();
        }
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof Tr064RootHandler) {
            this.handler = (Tr064RootHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }

    public static class BackupConfiguration {
        public final String directory;
        public final String password;

        public BackupConfiguration(String directory, String password) {
            this.directory = directory;
            this.password = password;
        }
    }
}
