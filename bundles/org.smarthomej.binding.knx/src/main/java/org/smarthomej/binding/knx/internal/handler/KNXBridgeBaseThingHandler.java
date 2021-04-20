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
package org.smarthomej.binding.knx.internal.handler;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.knx.internal.client.KNXClient;
import org.smarthomej.binding.knx.internal.client.StatusUpdateCallback;
import org.smarthomej.binding.knx.internal.config.IPBridgeConfiguration;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.mgmt.Destination;
import tuwien.auto.calimero.secure.Keyring;
import tuwien.auto.calimero.secure.KnxSecureException;
import tuwien.auto.calimero.secure.Security;
import tuwien.auto.calimero.xml.KNXMLException;

/**
 * The {@link KNXBridgeBaseThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Simon Kaufmann - Initial contribution and API
 */
@NonNullByDefault
public abstract class KNXBridgeBaseThingHandler extends BaseBridgeHandler implements StatusUpdateCallback {
    private final Logger logger = LoggerFactory.getLogger(KNXBridgeBaseThingHandler.class);
    private final ScheduledExecutorService knxScheduler = ThreadPoolManager.getScheduledPool("knx");
    private final ScheduledExecutorService backgroundScheduler = Executors.newSingleThreadScheduledExecutor();

    @SuppressWarnings("unused")
    protected ConcurrentHashMap<IndividualAddress, Destination> destinations = new ConcurrentHashMap<>();

    public KNXBridgeBaseThingHandler(Bridge bridge) {
        super(bridge);
    }

    protected abstract KNXClient getClient();

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Nothing to do here
    }

    public ScheduledExecutorService getScheduler() {
        return knxScheduler;
    }

    public ScheduledExecutorService getBackgroundScheduler() {
        return backgroundScheduler;
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    @Override
    public void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }

    /**
     * initialize the security if configured
     *
     * @param config bridge configuration file
     * @return true if successful or not configured, false if failed
     */
    protected boolean initializeSecurity(IPBridgeConfiguration config) {
        String keyringFile = config.getKeyringFile();
        String keyringPassword = config.getKeyringPassword();
        if (keyringFile.trim().isEmpty()) {
            // security is optional
            logger.debug("Security not configured.");
            return true;
        }

        try {
            // load keyring file from config dir, folder misc
            String keyringUri;
            keyringUri = OpenHAB.getConfigFolder() + File.separator + "misc" + File.separator + keyringFile.trim();
            Keyring keyring = Keyring.load(keyringUri);

            // loading was successful, check signatures
            char[] keyringPasswordArray = keyringPassword.toCharArray();
            if (!keyring.verifySignature(keyringPasswordArray)) {
                throw new KnxSecureException("signature verification failed, please check keyring file: " + keyringUri);
            }

            // Add to global static key(ring) storage of Calimero library.
            // More than one can be added ONLY IF addresses are different,
            // as Calimero adds all information to this static object.
            // -> to be discussed with owner of Calimero lib.
            Security.defaultInstallation().useKeyring(keyring, keyringPasswordArray);
        } catch (KnxSecureException | KNXMLException e) {
            logger.warn("Security configured, but initialization failed: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Security configuration failed");
            return false;
        }

        logger.debug("Security enabled for {} group addresses, {} devices",
                Security.defaultInstallation().groupKeys().size(),
                Security.defaultInstallation().deviceToolKeys().size());
        return true;
    }
}
