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
package org.smarthomej.automation.javarule.internal;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_LOCAL;
import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_RECURSE;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.CLASS_FILE_TYPE;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.JAR_FILE_TYPE;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.LIB_DIR;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.service.AbstractWatchService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Component;
import org.smarthomej.automation.javarule.Util;

/**
 * The {@link JavaRuleJavaFileManager} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = JavaRuleJavaFileManager.class)
public class JavaRuleJavaFileManager extends AbstractWatchService implements JavaFileManager {
    private final Map<String, List<JavaFileObject>> additionalOSGiPackages = new HashMap<>();
    private final Map<String, List<JavaFileObject>> additionalLibraryPackages = new HashMap<>();

    private final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    private final JavaFileManager systemFileManager;

    public JavaRuleJavaFileManager() {
        super(LIB_DIR.toString());

        if (!Util.checkFolder(LIB_DIR)) {
            throw new IllegalStateException("Failed to check folder " + LIB_DIR);
        }

        systemFileManager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, null);
    }

    public DiagnosticCollector<JavaFileObject> getDiagnostics() {
        return diagnostics;
    }

    public void addBundles(List<String> additionalBundles) {
        BundleContext context = FrameworkUtil.getBundle(Util.class).getBundleContext();
        List<Bundle> bundles = Arrays.stream(context.getBundles())
                .filter(bundle -> additionalBundles.contains(bundle.getSymbolicName())).collect(Collectors.toList());
        bundles.forEach(bundle -> {
            List<String> exportedPackages = Arrays.stream(getExportedPackages(bundle)).map(p -> p.split(";")[0])
                    .collect(Collectors.toList());

            exportedPackages.forEach(packageName -> {
                List<JavaFileObject> entries = bundle.adapt(BundleWiring.class)
                        .listResources("", "*.class", LISTRESOURCES_LOCAL + LISTRESOURCES_RECURSE).stream()
                        .map(className -> uriFromClassName(className, packageName, bundle)).filter(Optional::isPresent)
                        .map(Optional::get).map(JavaRuleJavaFileObject::classFileObject).collect(Collectors.toList());

                Objects.requireNonNull(additionalOSGiPackages.computeIfAbsent(packageName, k -> new ArrayList<>()))
                        .addAll(entries);
            });
        });
    }

    private String[] getExportedPackages(Bundle bundle) {
        return bundle.getHeaders().get("Export-Package") //
                .replaceAll("\".*?\"", "") // remove all ;uses="..." packages
                .split(",");
    }

    private Optional<URI> uriFromClassName(String className, String packageName, Bundle bundle) {
        // check if class is in package
        int classNameStart = className.lastIndexOf("/");
        if (classNameStart == -1 || !packageName.equals(className.substring(0, classNameStart).replace("/", "."))) {
            return Optional.empty();
        }
        try {
            return Optional.of(bundle.getEntry(className).toURI());
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    private void rebuildLibPackages() {
        try (Stream<Path> ruleFileStream = Files.list(LIB_DIR)) {
            List<Path> jarFiles = ruleFileStream.filter(JavaRuleConstants.JAR_FILE_FILTER).collect(Collectors.toList());
            logger.info("Number of libraries classes to load from '{}' to memory: {}", LIB_DIR, jarFiles.size());

            jarFiles.forEach(this::processJarLibrary);
        } catch (IOException e) {
            logger.warn("Could not load libraries: {}", e.getMessage());
        }
    }

    private void processJarLibrary(Path jarFile) {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFile.toFile()))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                String packageName = entry.getName().substring(0, entryName.lastIndexOf("/")).replaceAll("/", ".");
                URI classUri = URI.create("jar:" + jarFile.toUri() + "!/" + entryName);

                Objects.requireNonNull(additionalLibraryPackages.computeIfAbsent(packageName, k -> new ArrayList<>()))
                        .add(JavaRuleJavaFileObject.classFileObject(classUri));
                logger.trace("Added entry {} to additional libraries with package {}.", entry, packageName);
            }
        } catch (IOException e) {
            logger.warn("Failed to process {}: {}", jarFile, e.getMessage());
        }
    }

    /*
     * the following methods implement the modified JavaFileManager behaviour
     */

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
            boolean recurse) throws IOException {
        Iterable<JavaFileObject> stdResult = systemFileManager.list(location, packageName, kinds, recurse);

        if (location != StandardLocation.CLASS_PATH || !kinds.contains(JavaFileObject.Kind.CLASS)) {
            return stdResult;
        }

        List<JavaFileObject> mergedFileObjects = new ArrayList<>();
        stdResult.forEach(mergedFileObjects::add);
        mergedFileObjects.addAll(additionalOSGiPackages.getOrDefault(packageName, List.of()));
        mergedFileObjects.addAll(additionalLibraryPackages.getOrDefault(packageName, List.of()));

        return mergedFileObjects;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
            FileObject sibling) throws IOException {
        if (sibling instanceof JavaRuleJavaFileObject) {
            URI outFile = URI.create(Util.removeExtension(sibling.toUri().toString()) + CLASS_FILE_TYPE);
            return JavaRuleJavaFileObject.classFileObject(outFile);
        }
        return systemFileManager.getJavaFileForOutput(location, className, kind, sibling);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling)
            throws IOException {
        return systemFileManager.getFileForOutput(location, packageName, relativeName, sibling);
    }

    private String getPath(URI uri) {
        if ("jar".equals(uri.getScheme())) {
            String uriString = uri.toString();
            return uriString.substring(uriString.lastIndexOf("!"));
        } else {
            return uri.getPath();
        }
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof JavaRuleJavaFileObject) {
            return Util.removeExtension(getPath(file.toUri()).replaceAll("/", ".").substring(1));
        }
        return systemFileManager.inferBinaryName(location, file);
    }

    /*
     * the following methods just delegate to the system JavaFileManager
     */

    @Override
    public ClassLoader getClassLoader(Location location) {
        return systemFileManager.getClassLoader(location);
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        return systemFileManager.getFileForInput(location, packageName, relativeName);
    }

    @Override
    public void flush() throws IOException {
        systemFileManager.flush();
    }

    @Override
    public void close() throws IOException {
        systemFileManager.close();
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        return systemFileManager.isSameFile(a, b);
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        return systemFileManager.handleOption(current, remaining);
    }

    @Override
    public boolean hasLocation(Location location) {
        return systemFileManager.hasLocation(location);
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind)
            throws IOException {
        return systemFileManager.getJavaFileForInput(location, className, kind);
    }

    @Override
    public int isSupportedOption(String option) {
        return systemFileManager.isSupportedOption(option);
    }

    @Override
    public Location getLocationForModule(Location location, String moduleName) throws IOException {
        return systemFileManager.getLocationForModule(location, moduleName);
    }

    @Override
    public Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
        return systemFileManager.getLocationForModule(location, fo);
    }

    @Override
    public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service) throws IOException {
        return systemFileManager.getServiceLoader(location, service);
    }

    @Override
    public String inferModuleName(Location location) throws IOException {
        return systemFileManager.inferModuleName(location);
    }

    @Override
    public Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
        return systemFileManager.listLocationsForModules(location);
    }

    @Override
    public boolean contains(Location location, FileObject fo) throws IOException {
        return systemFileManager.contains(location, fo);
    }

    /*
     * following methods implement the watch service
     */
    @Override
    public boolean watchSubDirectories() {
        return true;
    }

    @Override
    public WatchEvent.Kind<?>[] getWatchEventKinds(@Nullable Path directory) {
        return new WatchEvent.Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    public void processWatchEvent(@Nullable WatchEvent<?> event, WatchEvent.@Nullable Kind<?> kind,
            @Nullable Path path) {
        if (path == null || kind == null || (kind != ENTRY_CREATE && kind != ENTRY_MODIFY && kind != ENTRY_DELETE)) {
            logger.trace("Received '{}' for path '{}' - ignoring (null or wrong kind)", kind, path);
            return;
        }
        if (!path.getFileName().toString().endsWith(JAR_FILE_TYPE)) {
            logger.trace("Received '{}' for path '{}' - ignoring (wrong extension)", kind, path);
            return;
        }

        rebuildLibPackages();
    }
}
