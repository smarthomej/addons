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
import java.net.URI;
import java.util.*;
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
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MavenRepoManager} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = MavenRepoManager.class, configurationPid = MavenRepoManager.CONFIGURATION_PID, configurationPolicy = ConfigurationPolicy.OPTIONAL)
@NonNullByDefault
public class MavenRepoManager {
    public static final String CONFIGURATION_PID = "smarthomej.MavenRepoManager";
    private static final String KARAF_MAVEN_REPO_PID = "org.ops4j.pax.url.mvn";
    private static final String KARAF_MAVEN_REPO_CONFIG_ID = "org.ops4j.pax.url.mvn.repositories";
    private static final Pattern METADATA_VERSION_PATTERN = Pattern.compile("<version>(.*?)</version>");
    private static final String MAVEN_REPO_CONFIG_ID = "enabledRepos";

    private final Logger logger = LoggerFactory.getLogger(MavenRepoManager.class);

    private final ConfigurationAdmin configurationAdmin;
    private final HttpClient httpClient;
    private Map<String, String> installedRepositories = new HashMap<>();

    @Activate
    public MavenRepoManager(@Reference ConfigurationAdmin configurationAdmin,
            @Reference HttpClientFactory httpClientFactory, Map<String, Object> configuration) {
        this.configurationAdmin = configurationAdmin;
        this.httpClient = httpClientFactory.getCommonHttpClient();

        // configure repos
        processRepoList(deserializeRepoMap((String) configuration.get(MAVEN_REPO_CONFIG_ID)));

        logger.debug("Maven repository manager started.");
        logger.trace("Configuration: {}", configuration);
    }

    @SuppressWarnings("unused")
    @Modified
    public void modified(Map<String, Object> cfg) {
        // ignore configuration update, we already handle it by ourself
    }

    @SuppressWarnings("unused")
    @Deactivate
    public void deactivate() {
        logger.debug("Maven repository manager stopped.");
    }

    private void storeConfiguration() {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(CONFIGURATION_PID);
            Dictionary<String, Object> properties = Objects.requireNonNullElse(configuration.getProperties(),
                    new Hashtable<>());
            properties.put(MAVEN_REPO_CONFIG_ID, serializeRepoMap(installedRepositories));
            configurationAdmin.getConfiguration(CONFIGURATION_PID).update(properties);
        } catch (IOException e) {
            logger.warn("Could not store configuration: {}", e.getMessage());
        }
    }

    /**
     * process new repo configuration and add/remove repos if necessary
     *
     * @param newRepositoryConfig map containing the new repository config
     */
    private void processRepoList(Map<String, String> newRepositoryConfig) {
        // find repos that need to be installed
        Map<String, String> toBeAdded = new HashMap<>(newRepositoryConfig);
        toBeAdded.entrySet().removeAll(installedRepositories.entrySet());
        logger.trace("Adding repositories: {}", toBeAdded);
        toBeAdded.forEach((k, v) -> modifyRepo(k, v, RepoAction.ADD));

        logger.trace("New repository config: {}", newRepositoryConfig);
        this.installedRepositories = newRepositoryConfig;
    }

    private void modifyRepo(String id, @Nullable String url, RepoAction repoAction) {
        try {
            Configuration karafConfiguration = configurationAdmin.getConfiguration(KARAF_MAVEN_REPO_PID);
            Dictionary<String, Object> properties = karafConfiguration.getProperties();
            String mavenRepos = (String) properties.get(KARAF_MAVEN_REPO_CONFIG_ID);
            logger.trace("Configured maven repositories: {}", mavenRepos);
            switch (repoAction) {
                case ADD:
                    if (url == null) {
                        logger.warn("Adding maven repository with id '{}' not possible with url=null", id);
                        return;
                    }
                    if (mavenRepos.contains(id)) {
                        logger.debug("Maven repository with id '{}' already present.", id);
                        return;
                    } else {
                        logger.info("Adding maven repository with id '{}' and URL '{}'", id, url);
                        mavenRepos += "," + url + id;
                    }
                    break;
                case REMOVE:
                    if (!mavenRepos.contains(id)) {
                        logger.debug("Maven repository with id '{}' not present.", id);
                        return;
                    } else {
                        logger.info("Removing maven repository with id '{}'", id);
                        mavenRepos = Arrays.stream(mavenRepos.split(",")).filter(r -> !r.contains(id))
                                .collect(Collectors.joining(","));
                    }
                    break;
            }
            properties.put(KARAF_MAVEN_REPO_CONFIG_ID, mavenRepos);
            karafConfiguration.update(properties);
        } catch (IOException e) {
            logger.warn("Could not {} maven repository with id '{}' and URL '{}': {}", repoAction, id, url,
                    e.getMessage());
        }
    }

    /**
     * Add Maven repository
     *
     * @param id repository id
     */
    public void addRepository(String id, String url) {
        Map<String, String> newRepositories = new HashMap<>(installedRepositories);
        String oldUrl = newRepositories.putIfAbsent(id, url);
        if (oldUrl != null) {
            logger.warn("Tried adding repository with id {} and URL {} but id is already present with URL {}", id, url,
                    oldUrl);
        } else {
            processRepoList(newRepositories);
            storeConfiguration();
        }
    }

    /**
     * Remove Maven repository
     *
     * @param id repository id
     */
    public void removeRepository(String id) {
        Map<String, String> newRepositories = new HashMap<>(installedRepositories);
        String oldUrl = newRepositories.remove(id);
        if (oldUrl == null) {
            logger.warn("Tried removing repository with id {} but id not found", id);
        } else {
            processRepoList(newRepositories);
            storeConfiguration();
        }
    }

    /**
     * Check Maven repository status
     *
     * @param id repository id
     * @return true if present, false of not present
     */
    public boolean repositoryStatus(String id) {
        return installedRepositories.get(id) != null;
    }

    public List<String> getAvailableVersions(String repoId, String groupId, String artifactId) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(KARAF_MAVEN_REPO_PID);
            Dictionary<String, Object> properties = configuration.getProperties();
            String mavenRepos = (String) properties.get(KARAF_MAVEN_REPO_CONFIG_ID);
            String url = Arrays.stream(mavenRepos.split(",")).filter(r -> r.contains(repoId)).findAny()
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

    /**
     * deserialize the map of the enabled maven repositories
     *
     * @param string a nullable String containing the repo configuration
     * @return a HashMap from the input string
     */
    private HashMap<String, String> deserializeRepoMap(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return new HashMap<>();
        }
        return Arrays.stream(string.split(",")).map(s -> s.split("\\|")).filter(a -> a.length == 2)
                .collect(Collectors.toMap(a -> a[0], a -> a[1], (prev, next) -> next, HashMap::new));
    }

    /**
     * serialize the map of the enabled maven repositories
     *
     * @param map a Map containing the repo configuration
     * @return a string from the input map
     */
    private String serializeRepoMap(Map<String, String> map) {
        return map.entrySet().stream().map(e -> e.getKey() + "|" + e.getValue()).collect(Collectors.joining(","));
    }

    private enum RepoAction {
        ADD,
        REMOVE
    }
}
