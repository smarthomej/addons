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

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.http.servlet.OpenHABServlet;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final String SNAPSHOT_REPO_ID = "@snapshots@id=smarthomej-snapshot";
    private static final String SNAPSHOT_REPO_URL = "https://oss.sonatype.org/content/repositories/snapshots";
    private static final String RELEASE_REPO_ID = "@id=smarthomej-release";
    private static final String RELEASE_REPO_URL = "https://repo1.maven.org/maven2";

    private static final Map<String, String> REPO_URLS = Map.of(SNAPSHOT_REPO_ID, SNAPSHOT_REPO_URL, RELEASE_REPO_ID,
            RELEASE_REPO_URL);

    public static final String KARAF_FEATURE_GROUP_ID = "org.smarthomej.addons.features.karaf";
    public static final String KARAF_FEATURE_ARTIFACT_ID = "org.smarthomej.addons.features.karaf.smarthomej-addons";

    private final Logger logger = LoggerFactory.getLogger(RepoManagerServlet.class);

    private final MavenRepoManager mavenRepoManager;
    private final AddonProvider addonProvider;
    private final String bundleVersion;

    @Activate
    public RepoManagerServlet(@Reference HttpService httpService, @Reference MavenRepoManager mavenRepoManager,
            @Reference AddonProvider addonProvider) {
        super(httpService, httpService.createDefaultHttpContext());

        this.mavenRepoManager = mavenRepoManager;
        this.addonProvider = addonProvider;

        bundleVersion = FrameworkUtil.getBundle(getClass()).getVersion().toString();

        activate(SERVLET_URL);
    }

    @SuppressWarnings("unused")
    @Deactivate
    public void deactivate() {
        stop();
    }

    @Override
    protected void doGet(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp) throws IOException {
        if (req == null || resp == null) {
            return;
        }

        String requestUri = req.getRequestURI();
        if (requestUri == null) {
            return;
        }
        String uri = requestUri.substring(SERVLET_URL.length());
        String queryString = req.getQueryString();

        if (!uri.isEmpty()) {
            doRepoAction(uri, queryString);
            resp.sendRedirect(SERVLET_URL);
            return;
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>SmartHome/J Repository Manager</title><head><body>");
        html.append("<h1>SmartHome/J Repository Manager Configuration</h1>");

        buildRepoEntry(html, "Snapshot", SNAPSHOT_REPO_ID);
        buildRepoEntry(html, "Release", RELEASE_REPO_ID);

        html.append("<hr/><div style=\"float: right; position:relative;\">RepoManager-Version: ").append(bundleVersion)
                .append("</div></body></html>");
        resp.addHeader("content-type", "text/html;charset=UTF-8");
        try {
            resp.getWriter().write(html.toString());
        } catch (IOException e) {
            logger.warn("Return html failed with uri syntax error", e);
        }
    }

    public void stop() {
        httpService.unregister(SERVLET_URL);
    }

    private void doRepoAction(String uri, @Nullable String queryString) {
        switch (uri) {
            case "/removeMavenRepo":
                if (queryString != null && !queryString.isEmpty()) {
                    // remove all feature repos from this maven repository
                    mavenRepoManager
                            .getAvailableVersions(queryString, KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID)
                            .forEach(v -> addonProvider.removeFeatureRepository(KARAF_FEATURE_GROUP_ID,
                                    KARAF_FEATURE_ARTIFACT_ID, v));
                    mavenRepoManager.removeRepository(queryString);
                }
                break;
            case "/addMavenRepo":
                if (queryString != null && !queryString.isEmpty()) {
                    String repoUrl = REPO_URLS.get(queryString);
                    if (repoUrl != null) {
                        mavenRepoManager.addRepository(queryString, repoUrl);
                    } else {
                        logger.warn("Could not find URL for maven repository '{}'", queryString);
                    }
                }
                break;
            case "/addFeatureRepo":
                if (queryString != null && !queryString.isEmpty()) {
                    addonProvider.addFeatureRepository(KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID, queryString);
                }
                break;
            case "/removeFeatureRepo":
                if (queryString != null && !queryString.isEmpty()) {
                    addonProvider.removeFeatureRepository(KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID,
                            queryString);
                }
                break;
            default:
                logger.warn("Unknown request: {}", uri);
        }
    }

    private void buildRepoEntry(StringBuilder html, String repoName, String repoId) {
        boolean mavenRepoInstalled = mavenRepoManager.repositoryStatus(repoId);
        html.append("<hr/><h2>").append(repoName).append(":</h2><br />Status: ")
                .append(mavenRepoInstalled ? "active" : "not active").append("<br/><br/>");

        if (mavenRepoInstalled) {
            mavenRepoManager.getAvailableVersions(repoId, KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID).stream()
                    .map(this::buildVersionLink).forEach(html::append);

            html.append("<br/><a href=\"").append(SERVLET_URL).append("/removeMavenRepo?").append(repoId)
                    .append("\">Remove</a>");
        } else {
            html.append("<a href=\"").append(SERVLET_URL).append("/addMavenRepo?").append(repoId).append("\">Add</a>");
        }
    }

    private String buildVersionLink(String versionId) {
        String versionLink = "<a href=\"" + SERVLET_URL;
        if (addonProvider.statusFeatureRepository(KARAF_FEATURE_GROUP_ID, KARAF_FEATURE_ARTIFACT_ID, versionId)) {
            versionLink += "/removeFeatureRepo?" + versionId + "\">" + versionId + " (active)</a><br/>";
        } else {
            versionLink += "/addFeatureRepo?" + versionId + "\">" + versionId + " (inactive)</a><br/>";
        }
        return versionLink;
    }
}
