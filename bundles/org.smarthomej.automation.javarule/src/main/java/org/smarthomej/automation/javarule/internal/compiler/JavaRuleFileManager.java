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
package org.smarthomej.automation.javarule.internal.compiler;

import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.CLASS_FILE_TYPE;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.CORE_DEPENDENCY_JAR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.LIB_DIR;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.internal.JavaRuleConstants;

/**
 * The {@link JavaRuleFileManager} is an implementation of {@link JavaFileManager} with extensions for JAR files
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JavaRuleFileManager<M extends JavaFileManager> extends ForwardingJavaFileManager<M> {
    private final Logger logger = LoggerFactory.getLogger(JavaRuleFileManager.class);

    private Map<String, List<JavaFileObject>> additionalPackages = Map.of();
    private final ClassLoader parentClassLoader;
    private final Lock fileManagerLock = new ReentrantLock();
    private ClassLoader classLoader;

    public JavaRuleFileManager(M fileManager, ClassLoader parentClassLoader) {
        super(fileManager);

        this.parentClassLoader = parentClassLoader;
        this.classLoader = this.parentClassLoader;

        rebuildLibPackages();
    }

    public Lock getFileManagerLock() {
        return fileManagerLock;
    }

    public void rebuildLibPackages() {
        fileManagerLock.lock();
        try (Stream<Path> libFileStream = Files.list(LIB_DIR)) {
            List<Path> libFiles = libFileStream.filter(JavaRuleConstants.JAR_FILE_FILTER).collect(Collectors.toList());
            logger.debug("Libraries to load from '{}' to memory: {}", LIB_DIR, libFiles);

            JarClassLoader classLoader = new JarClassLoader(parentClassLoader);
            Map<String, List<JavaFileObject>> additionalPackages = new HashMap<>();
            libFiles.forEach(libFile -> processLibrary(libFile, classLoader, additionalPackages));

            this.classLoader = classLoader;
            this.additionalPackages = additionalPackages;
        } catch (IOException e) {
            logger.warn("Could not load libraries: {}", e.getMessage());
        } finally {
            fileManagerLock.unlock();
        }
    }

    /*
     * the following methods implement the modified JavaFileManager behaviour
     */

    @Override
    public ClassLoader getClassLoader(@Nullable Location location) {
        // create a new classloader each time, because the compiler closes the classloader
        return classLoader;
    }

    @Override
    public @NonNullByDefault({}) Iterable<JavaFileObject> list(@Nullable Location location,
            @Nullable String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        Iterable<JavaFileObject> stdResult = fileManager.list(location, packageName, kinds, recurse);

        if (location != StandardLocation.CLASS_PATH || !kinds.contains(JavaFileObject.Kind.CLASS)) {
            return stdResult;
        }

        List<JavaFileObject> mergedFileObjects = new ArrayList<>();
        stdResult.forEach(mergedFileObjects::add);
        mergedFileObjects.addAll(additionalPackages.getOrDefault(packageName, List.of()));

        return mergedFileObjects;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(@Nullable Location location, @Nullable String className,
            @Nullable Kind kind, @Nullable FileObject sibling) throws IOException {
        if (sibling instanceof JavaRuleFileObject) {
            URI outFile = URI.create(removeExtension(sibling.toUri().toString()) + CLASS_FILE_TYPE);
            return JavaRuleFileObject.classFileObject(outFile);
        }
        return fileManager.getJavaFileForOutput(location, className, kind, sibling);
    }

    @Override
    public String inferBinaryName(@Nullable Location location, @Nullable JavaFileObject file) {
        if (file instanceof JavaRuleFileObject) {
            return removeExtension(getPath(file.toUri()).replace("/", ".").substring(1));
        }
        return fileManager.inferBinaryName(location, file);
    }

    private String removeExtension(String name) {
        return name.substring(0, name.lastIndexOf("."));
    }

    private void processLibrary(Path jarFile, JarClassLoader jarClassLoader,
            Map<String, List<JavaFileObject>> additionalPackages) {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFile.toFile()))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                int fileNameStart = entryName.lastIndexOf("/");
                String packageName = fileNameStart == -1 ? ""
                        : entry.getName().substring(0, fileNameStart).replace("/", ".");
                URI classUri = URI.create("jar:" + jarFile.toUri() + "!/" + entryName);

                Objects.requireNonNull(additionalPackages.computeIfAbsent(packageName, k -> new ArrayList<>()))
                        .add(JavaRuleFileObject.classFileObject(classUri));
                logger.trace("Added entry {} to additional libraries with package {}.", entry, packageName);
            }
        } catch (IOException e) {
            logger.warn("Failed to process {}: {}", jarFile, e.getMessage());
        }

        if (!CORE_DEPENDENCY_JAR.equals(jarFile)) {
            // the core dependencies are already part of the OSGi classloader
            jarClassLoader.addJar(jarFile);
        }
    }

    private String getPath(URI uri) {
        if ("jar".equals(uri.getScheme())) {
            String uriString = uri.toString();
            return uriString.substring(uriString.lastIndexOf("!"));
        } else {
            return uri.getPath();
        }
    }
}
