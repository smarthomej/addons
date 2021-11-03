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
package org.smarthomej.binding.viessmann.internal.api;

import static org.smarthomej.binding.viessmann.internal.ViessmannBindingConstants.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.io.net.http.HttpUtil;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.viessmann.internal.dto.device.DeviceDTO;
import org.smarthomej.binding.viessmann.internal.dto.error.ViErrorDTO;
import org.smarthomej.binding.viessmann.internal.dto.features.FeaturesDTO;
import org.smarthomej.binding.viessmann.internal.dto.installation.Data;
import org.smarthomej.binding.viessmann.internal.dto.installation.Gateway;
import org.smarthomej.binding.viessmann.internal.dto.installation.InstallationDTO;
import org.smarthomej.binding.viessmann.internal.dto.oauth.TokenResponseDTO;
import org.smarthomej.binding.viessmann.internal.handler.ViessmannBridgeHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link ViessmannApi} is responsible for managing all communication with
 * the Viessmann API service.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class ViessmannApi {
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    private static final int TOKEN_MIN_DIFF_MS = (int) TimeUnit.DAYS.toMillis(2);

    public static final Properties HTTP_HEADERS;
    static {
        HTTP_HEADERS = new Properties();
        HTTP_HEADERS.put("User-Agent", "openhab-viessmann-api/2.0");
    }

    public static Gson getGson() {
        return GSON;
    }

    private @Nullable static TokenResponseDTO tokenResponse;

    public static void setTokenResponseDTO(TokenResponseDTO newTokenResponse) {
        tokenResponse = newTokenResponse;
    }

    public static @Nullable TokenResponseDTO getTokenResponseDTO() throws ViessmannAuthException {
        return tokenResponse;
    }

    private static long tokenExpiryDate;

    static void setTokenExpiryDate(long expiresIn) {
        tokenExpiryDate = System.nanoTime() + expiresIn;
    }

    public long getTokenExpiryDate() {
        return tokenExpiryDate;
    }

    private final Logger logger = LoggerFactory.getLogger(ViessmannApi.class);

    private final ViessmannBridgeHandler bridgeHandler;

    private final String apiKey;
    private final String user;
    private final String password;
    private String installationId;
    private String gatewaySerial;

    private int apiTimeout;
    private final HttpClient httpClient;

    private @NonNullByDefault({}) ViessmannAuth viessmannAuth;

    public ViessmannApi(final ViessmannBridgeHandler bridgeHandler, final String apiKey, final int apiTimeout,
            HttpClient httpClient, String user, String password, String installationId, String gatewaySerial) {
        this.bridgeHandler = bridgeHandler;
        this.apiKey = apiKey;
        this.apiTimeout = apiTimeout;
        this.httpClient = httpClient;
        this.user = user;
        this.password = password;
        this.installationId = installationId;
        this.gatewaySerial = gatewaySerial;
        tokenResponse = null;
        createOAuthClientService();
        isAuthorized();
        if (installationId == null || gatewaySerial == null) {
            setInstallationAndGatewayId();
        }
    }

    public void createOAuthClientService() {
        String bridgeUID = bridgeHandler.getThing().getUID().getAsString();
        logger.debug("API: Creating OAuth Client Service for {}", bridgeUID);
        viessmannAuth = new ViessmannAuth(bridgeHandler, apiKey, apiTimeout, httpClient, user, password);
    }

    /**
     * Check to see if the Viessmann authorization process is complete. This will be determined
     * by requesting an AccessTokenResponse from the API. If we get a valid
     * response, then assume that the Viessmann authorization process is complete. Otherwise,
     * start the Viessmann authorization process.
     */
    private boolean isAuthorized() {
        boolean isAuthorized = false;
        try {
            TokenResponseDTO localAccessTokenResponseDTO = getTokenResponseDTO();
            if (localAccessTokenResponseDTO != null) {
                if (localAccessTokenResponseDTO.accessToken != null) {
                    logger.trace("API: Got AccessTokenResponse from OAuth service: {}", localAccessTokenResponseDTO);
                    logger.debug("Checking if new access token is needed...");
                    try {
                        long difference = getTokenExpiryDate() - System.nanoTime();
                        if (difference <= TOKEN_MIN_DIFF_MS) {
                            viessmannAuth.setState(ViessmannAuthState.NEED_REFRESH_TOKEN);
                            viessmannAuth.setRefreshToken(localAccessTokenResponseDTO.refreshToken);
                        } else {
                            viessmannAuth.setState(ViessmannAuthState.COMPLETE);
                        }
                    } catch (RuntimeException r) {
                        logger.debug("Could not check token expiry date for Thing {}: {}",
                                bridgeHandler.getThing().getUID().getAsString(), r.getMessage(), r);
                    }
                    isAuthorized = true;
                } else {
                    logger.debug(
                            "API: Didn't get an AccessTokenResponse from OAuth service - doEcobeeAuthorization!!!");
                    if (viessmannAuth.isComplete()) {
                        viessmannAuth.setState(ViessmannAuthState.NEED_AUTH);
                    }
                }
            }
            viessmannAuth.doAuthorization();
            isAuthorized = true;
        } catch (RuntimeException e) {
            if (logger.isDebugEnabled()) {
                logger.info("API: Got exception trying to get access token from OAuth service", e);
            } else {
                logger.info("API: Got {} trying to get access token from OAuth service: {}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
        } catch (ViessmannAuthException e) {
            if (logger.isDebugEnabled()) {
                logger.info("API: The Viessmann authorization process threw an exception", e);
            } else {
                logger.info("API: The Viessmann authorization process threw an exception: {}", e.getMessage());
            }
            viessmannAuth.setState(ViessmannAuthState.NEED_AUTH);
        }
        return isAuthorized;
    }

    public void checkExpiringToken() {
        logger.debug("Checking if new access token is needed...");
        TokenResponseDTO localAccessTokenResponseDTO;
        try {
            localAccessTokenResponseDTO = getTokenResponseDTO();
            if (localAccessTokenResponseDTO != null) {
                try {
                    long difference = getTokenExpiryDate() - System.nanoTime();
                    if (difference <= TOKEN_MIN_DIFF_MS) {
                        viessmannAuth.setState(ViessmannAuthState.NEED_REFRESH_TOKEN);
                        viessmannAuth.setRefreshToken(localAccessTokenResponseDTO.refreshToken);
                        viessmannAuth.doAuthorization();
                    } else {
                        viessmannAuth.setState(ViessmannAuthState.COMPLETE);
                    }
                } catch (RuntimeException r) {
                    logger.debug("Could not check token expiry date for Thing {}: {}",
                            bridgeHandler.getThing().getUID().getAsString(), r.getMessage(), r);
                }
            }
        } catch (ViessmannAuthException e) {
            if (logger.isDebugEnabled()) {
                logger.info("API: The Viessmann authorization process threw an exception", e);
            } else {
                logger.info("API: The Viessmann authorization process threw an exception: {}", e.getMessage());
            }
            viessmannAuth.setState(ViessmannAuthState.NEED_AUTH);
        }
    }

    public @Nullable DeviceDTO getAllDevices() {
        String response = "";
        response = executeGet(VIESSMANN_BASE_URL + "iot/v1/equipment/installations/" + installationId + "/gateways/"
                + gatewaySerial + "/devices");
        DeviceDTO devices = GSON.fromJson(response, DeviceDTO.class);
        return devices;
    }

    public @Nullable FeaturesDTO getAllFeatures(String deviceId) {
        String response = "";
        response = executeGet(VIESSMANN_BASE_URL + "iot/v1/equipment/installations/" + installationId + "/gateways/"
                + gatewaySerial + "/devices/" + deviceId + "/features/");
        if (response != null) {
            FeaturesDTO features = GSON.fromJson(response, FeaturesDTO.class);
            return features;
        }
        return null;
    }

    private void setInstallationAndGatewayId() {
        String response = executeGet(VIESSMANN_BASE_URL + "iot/v1/equipment/installations?includeGateways=true");
        InstallationDTO installation = GSON.fromJson(response, InstallationDTO.class);
        if (installation != null) {
            List<Data> listData = installation.data;
            Data data = listData.get(0);
            List<Gateway> listGateway = data.gateways;
            Gateway gateway = listGateway.get(0);

            logger.debug("Installation ID: {}", data.id);
            logger.debug("Gateway Serial : {}", gateway.serial);

            this.installationId = data.id.toString();
            this.gatewaySerial = gateway.serial;
            ViessmannBridgeHandler.setInstallationGatewayId(data.id.toString(), gateway.serial);
        }
    }

    public boolean setData(String url, String json) {
        return executePost(url, json);
    }

    private @Nullable String executeGet(String url) {
        String response = null;
        try {
            long startTime = System.currentTimeMillis();
            logger.trace("API: Get Request URL is '{}'", url);
            response = HttpUtil.executeUrl("GET", url, setHeaders(), null, null, apiTimeout);
            logger.trace("API: Response took {} msec: {}", System.currentTimeMillis() - startTime, response);
            if (response.indexOf("viErrorId") >= 0) {
                ViErrorDTO viError = GSON.fromJson(response, ViErrorDTO.class);
                if (viError != null) {
                    if (viError.getStatusCode() == 429) {
                        logger.warn("ViError: {} | Resetting Limit at {}", viError.getMessage(),
                                viError.getExtendedPayload().getLimitRestetDateTime());
                        bridgeHandler.updateBridgeStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                String.format("%s Resetting Limit at %s", viError.getMessage(),
                                        viError.getExtendedPayload().getLimitRestetDateTime()));
                        bridgeHandler.waitForApiCallLimitReset(viError.getExtendedPayload().getLimitReset());
                    } else {
                        logger.warn("ViError: {}", viError.getMessage());
                    }
                    return null;
                }
            }
        } catch (IOException e) {
            logger.info("API: Unable to execute GET: {}", e.getMessage());
        } catch (ViessmannAuthException e) {
            logger.info("API: Unable to execute GET: {}", e.getMessage());
            isAuthorized();
        }
        return response;
    }

    private boolean executePost(String url, String json) {
        try {
            logger.trace("API: Post request json is '{}'", json);
            long startTime = System.currentTimeMillis();
            String response = HttpUtil.executeUrl("POST", url, setHeaders(), new ByteArrayInputStream(json.getBytes()),
                    "application/json", apiTimeout);
            logger.trace("API: Response took {} msec: {}", System.currentTimeMillis() - startTime, response);
            if (response.indexOf("viErrorId") >= 0) {
                ViErrorDTO viError = GSON.fromJson(response, ViErrorDTO.class);
                if (viError != null) {
                    if (viError.getStatusCode() == 429) {
                        logger.warn("ViError: {} | Reseting Limit at {}", viError.getMessage(),
                                viError.getExtendedPayload().getLimitRestetDateTime());
                        bridgeHandler.updateBridgeStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                String.format("API Call limit reached. Reset at {}",
                                        viError.getExtendedPayload().getLimitRestetDateTime()));
                    } else {
                        logger.warn("ViError: {} | Reason: {}", viError.getMessage(),
                                viError.getExtendedPayload().getReason());
                    }
                }
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.info("API: Unable to execute POST: {}", e.getMessage());
        } catch (ViessmannAuthException e) {
            logger.info("API: Unable to execute POST: {}", e.getMessage());
        }
        return false;
    }

    private Properties setHeaders() throws ViessmannAuthException {
        TokenResponseDTO atr = getTokenResponseDTO();

        if (atr == null) {
            throw new ViessmannAuthException("Can not set auth header because access token is null");
        }
        if (atr.accessToken == null) {
            throw new ViessmannAuthException("Can not set auth header because access token is null");
        }
        Properties headers = new Properties();
        headers.putAll(HTTP_HEADERS);
        headers.put("Authorization", "Bearer " + atr.accessToken);
        return headers;
    }
}
