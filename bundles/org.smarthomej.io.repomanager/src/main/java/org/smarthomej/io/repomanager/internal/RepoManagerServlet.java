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
package org.smarthomej.io.repomanager.internal;

import static org.smarthomej.io.repomanager.internal.RepoManagerConstants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.http.servlet.OpenHABServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link RepoManagerServlet} provides a Servlet for controlling the repo manager
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class RepoManagerServlet extends OpenHABServlet {
    private static final String SERVLET_URL = "/repomanager";
    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(RepoManagerServlet.class);

    private final MavenRepoManager mavenRepoManager;
    private final AddonProvider addonProvider;
    private final Gson gson = new Gson();

    private RepomanagerConfigurationDTO oldConfig = new RepomanagerConfigurationDTO();

    @Activate
    public RepoManagerServlet(@Reference HttpService httpService, @Reference MavenRepoManager mavenRepoManager,
            @Reference AddonProvider addonProvider) {
        super(httpService, httpService.createDefaultHttpContext());

        this.mavenRepoManager = mavenRepoManager;
        this.addonProvider = addonProvider;

        activate(SERVLET_URL);
    }

    @SuppressWarnings("unused")
    @Deactivate
    public void deactivate() {
        httpService.unregister(SERVLET_URL);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String requestUri = Objects.requireNonNull(req.getRequestURI());

        try {
            if (requestUri.equals(SERVLET_URL + "/config")) {
                doPostConfig(req);

            } else {
                resp.sendError(404);
            }
        } catch (IOException e) {
            logger.warn("Processing POST data for '{}' failed: {}", requestUri, e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String requestUri = Objects.requireNonNull(req.getRequestURI());

        try {
            if (requestUri.equals(SERVLET_URL + "/config")) {
                doGetConfig(resp);
            } else if (requestUri.startsWith(SERVLET_URL + "/resources")) {
                doGetResource(resp, requestUri);
            } else if (requestUri.equals(SERVLET_URL)) {
                String returnHtml = loadResource("servlet/repomanager.html");
                if (returnHtml == null) {
                    logger.warn("Could not load RepoManager");
                    resp.sendError(500);
                    return;
                }

                resp.addHeader("content-type", "text/html;charset=UTF-8");
                resp.getWriter().write(returnHtml);
            } else {
                resp.sendError(404);
            }
        } catch (IOException | MavenRepoManagerException e) {
            logger.warn("Returning GET data for '{}' failed: {}", requestUri, e.getMessage());
        }
    }

    private void doGetConfig(HttpServletResponse resp) throws IOException, MavenRepoManagerException {
        RepomanagerConfigurationDTO config = new RepomanagerConfigurationDTO();

        // releases
        config.releasesEnabled = mavenRepoManager.repositoryStatus(RELEASE_REPO_ID);
        if (config.releasesEnabled) {
            config.releases = mavenRepoManager
                    .getAvailableVersions(RELEASE_REPO_ID, KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID).stream()
                    .map(v -> new RepomanagerConfigurationDTO.Release(v,
                            addonProvider.statusFeatureRepository(
                                    new GAV(KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID, v))))
                    .collect(Collectors.toList());
        }

        // snapshots
        config.snapshotsEnabled = mavenRepoManager.repositoryStatus(RepoManagerConstants.SNAPSHOT_REPO_ID);
        if (config.snapshotsEnabled) {
            config.snapshotVersion = getSnapshotVersion();
        }

        // store for later comparison and send
        oldConfig = config;
        resp.getWriter().write(gson.toJson(config));
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
    }

    private void doGetResource(HttpServletResponse resp, String uri) throws IOException {
        String path = uri.replace(SERVLET_URL + "/resources", "");
        String resourceContent = loadResource(path);
        if (resourceContent == null) {
            resp.sendError(404);
            return;
        }
        resp.getWriter().write(resourceContent);

        // set content type
        if (path.endsWith(".css")) {
            resp.setContentType("text/css");
        } else {
            resp.setContentType("text/plain");
        }
    }

    private void doPostConfig(HttpServletRequest req) throws IOException {
        try {
            RepomanagerConfigurationDTO configuration = Objects.requireNonNull(gson.fromJson(
                    req.getReader().lines().collect(Collectors.joining()), RepomanagerConfigurationDTO.TYPE_TOKEN));
            // check repositories
            if (configuration.releasesEnabled && !oldConfig.releasesEnabled) {
                mavenRepoManager.addRepository(RELEASE_REPO_ID, RELEASE_REPO_URL);
            } else if (!configuration.releasesEnabled && oldConfig.releasesEnabled) {
                // first disable all enabled feature repositories
                configuration.releases.forEach(release -> {
                    try {
                        GAV featureRepoGAV = new GAV(KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID,
                                release.version);
                        if (addonProvider.statusFeatureRepository(featureRepoGAV)) {
                            addonProvider.removeFeatureRepository(featureRepoGAV);
                        }
                    } catch (MavenRepoManagerException e) {
                        logger.warn(
                                "Failed to modify feature repository configuration when trying to remove release version {}: {}",
                                release.version, e.getMessage());
                    }
                });
                mavenRepoManager.removeRepository(RELEASE_REPO_ID);
            }
            if (configuration.snapshotsEnabled && !oldConfig.snapshotsEnabled) {
                mavenRepoManager.addRepository(SNAPSHOT_REPO_ID, SNAPSHOT_REPO_URL);
                try {
                    GAV gav = new GAV(KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID, getSnapshotVersion());
                    addonProvider.addFeatureRepository(gav);
                } catch (MavenRepoManagerException e) {
                    logger.warn(
                            "Failed to modify feature repository configuration when trying to add snapshot version {}: {}",
                            configuration.snapshotVersion, e.getMessage());
                }
            } else if (!configuration.snapshotsEnabled && oldConfig.snapshotsEnabled) {
                try {
                    GAV gav = new GAV(KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID, configuration.snapshotVersion);
                    addonProvider.removeFeatureRepository(gav);
                } catch (MavenRepoManagerException e) {
                    logger.warn(
                            "Failed to modify feature repository configuration when trying to remove snapshot version {}: {}",
                            configuration.snapshotVersion, e.getMessage());
                }
                mavenRepoManager.removeRepository(SNAPSHOT_REPO_ID);
            }

            // check release version features
            if (configuration.releasesEnabled) {
                configuration.releases.forEach(release -> {
                    try {
                        GAV featureRepoGAV = new GAV(KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID,
                                release.version);
                        if (release.enabled && !addonProvider.statusFeatureRepository(featureRepoGAV)) {
                            addonProvider.addFeatureRepository(featureRepoGAV);
                        } else if (!release.enabled && addonProvider.statusFeatureRepository(featureRepoGAV)) {
                            addonProvider.removeFeatureRepository(featureRepoGAV);
                        }
                    } catch (MavenRepoManagerException e) {
                        logger.warn(
                                "Failed to modify feature repository configuration when trying to {} release version {}: {}",
                                release.enabled ? "add" : "remove", release.version, e.getMessage());
                    }
                });
            }
        } catch (JsonSyntaxException e) {
            logger.warn("Did not receive a valid response");
        }
    }

    /**
     * load a resource from the bundle
     * 
     * @param path the path to the resource
     * @return a string containing the resource content
     */
    private @Nullable String loadResource(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            logger.warn("Could not get classloader.");
            return null;
        }

        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            if (inputStream == null) {
                logger.warn("Requested resource '{}' not found.", path);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (IOException e) {
            logger.warn("Requested resource '{}' could not be loaded: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * get the highest compatible SNAPSHOT version
     *
     * @return version string (can be empty if no compatible version found)
     * @throws MavenRepoManagerException
     */
    private String getSnapshotVersion() throws MavenRepoManagerException {
        return Objects.requireNonNull(mavenRepoManager
                .getAvailableVersions(RepoManagerConstants.SNAPSHOT_REPO_ID,
                        RepoManagerConstants.KARAF_FEATURE_GROUP_ID, RepoManagerConstants.KARAF_FEATURE_ARTIFACT_ID)
                .stream().min(Comparator.reverseOrder()).orElse(""));
    }
}
