/**
 * Copyright (c) 2021 Contributors to the Smarthome/J project
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.smarthomej.io.connector.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MavenRepoManager} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = MavenRepoManager.class)
@NonNullByDefault
public class MavenRepoManager {
    private static final String KARAF_MAVEN_REPO_PID = "org.ops4j.pax.url.mvn";
    private static final String KARAF_MAVEN_REPO_CONFIG_ID = "org.ops4j.pax.url.mvn.repositories";
    private static final Pattern METADATA_VERSION_PATTERN = Pattern.compile("<version>(.*?)</version>");

    private final Logger logger = LoggerFactory.getLogger(MavenRepoManager.class);

    private final ConfigurationAdmin configurationAdmin;
    private final HttpClient httpClient;

    @Activate
    public MavenRepoManager(@Reference ConfigurationAdmin configurationAdmin,
            @Reference HttpClientFactory httpClientFactory) {
        this.configurationAdmin = configurationAdmin;
        this.httpClient = httpClientFactory.getCommonHttpClient();
        logger.debug("MavenRepoManager started.");
    }

    @Deactivate
    public void deactivate() {
        logger.debug("MavenRepoManager stopped.");
    }

    private boolean modifyRepo(String id, @Nullable String url, RepoAction repoAction) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(KARAF_MAVEN_REPO_PID);
            Dictionary<String, Object> properties = configuration.getProperties();
            String mavenRepos = (String) properties.get(KARAF_MAVEN_REPO_CONFIG_ID);
            logger.trace("Configured maven repositories: {}", mavenRepos);
            switch (repoAction) {
                case ADD:
                    if (url == null) {
                        logger.warn("Adding maven repository with id '{}' not possible with url=null", id);
                        return false;
                    }
                    if (mavenRepos.contains(id)) {
                        logger.debug("Maven repository with id '{}' already present.", id);
                        return true;
                    } else {
                        logger.info("Adding maven repository with id '{}' and URL '{}'", id, url);
                        mavenRepos += "," + url + id;
                    }
                    break;
                case REMOVE:
                    if (!mavenRepos.contains(id)) {
                        logger.debug("Maven repository with id '{}' not present.", id);
                        return true;
                    } else {
                        logger.info("Removing maven repository with id '{}'", id);
                        mavenRepos = Arrays.asList(mavenRepos.split(",")).stream().filter(r -> !r.contains(id))
                                .collect(Collectors.joining(","));
                    }
                    break;
                case STATUS:
                    return mavenRepos.contains(id);
            }
            properties.put(KARAF_MAVEN_REPO_CONFIG_ID, mavenRepos);
            configuration.update(properties);
            return true;
        } catch (IOException e) {
            logger.warn("Could not {} maven repository with id '{}' and URL '{}': {}", repoAction, id, url,
                    e.getMessage());
            return false;
        }
    }

    /**
     * Add Maven repository
     *
     * @param id repository id
     * @return true if successful, false if failes
     */
    public boolean addRepository(String id, String url) {
        return modifyRepo(id, url, RepoAction.ADD);
    }

    /**
     * Remove Maven repository
     *
     * @param id repository id
     * @return true if successful, false if failes
     */
    public boolean removeRepository(String id) {
        return modifyRepo(id, null, RepoAction.REMOVE);
    }

    /**
     * Check Maven repository status
     *
     * @param id repository id
     * @return true if present, false of not present
     */
    public boolean repositoryStatus(String id) {
        return modifyRepo(id, null, RepoAction.STATUS);
    }

    public List<String> getAvailableVersions(String repoId, String groupId, String artifactId) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(KARAF_MAVEN_REPO_PID);
            Dictionary<String, Object> properties = configuration.getProperties();
            String mavenRepos = (String) properties.get(KARAF_MAVEN_REPO_CONFIG_ID);
            String url = Arrays.asList(mavenRepos.split(",")).stream().filter(r -> r.contains(repoId)).findAny()
                    .orElseThrow(IllegalArgumentException::new).split("@")[0];
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += groupId.replace(".", "/") + "/" + artifactId + "/maven-metadata.xml";
            String metaData = httpClient.newRequest(URI.create(url)).send().getContentAsString();
            Matcher matcher = METADATA_VERSION_PATTERN.matcher(metaData);
            return matcher.results().map(m -> m.group(1)).collect(Collectors.toList());
        } catch (IOException e) {
            logger.warn("Could not get maven repository list: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("Could not find repository {}", repoId);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.warn("Failed to get maven-metadata.xml for '{}/{}' from repository '{}' : {}", groupId, artifactId,
                    repoId, e.getMessage());
        }
        return List.of();
    }

    private enum RepoAction {
        ADD,
        REMOVE,
        STATUS
    }
}
