/**
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
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

import static java.lang.Integer.MAX_VALUE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_LOCAL;
import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_RECURSE;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.CORE_DEPENDENCY_JAR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.JAR_FILE_TYPE;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.JAVA_FILE_TYPE;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.LIB_DIR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemAddedEvent;
import org.openhab.core.items.events.ItemRemovedEvent;
import org.openhab.core.service.AbstractWatchService;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.events.ThingAddedEvent;
import org.openhab.core.thing.events.ThingRemovedEvent;
import org.openhab.core.thing.events.ThingStatusInfoChangedEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.internal.JavaRuleConstants;

import ch.obermuhlner.scriptengine.java.MemoryFileManager;

/**
 * The {@link CompilerService} compiles the dependency jar
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = { CompilerService.class, EventSubscriber.class }, configurationPid = "automation.javarule")
public class CompilerService extends AbstractWatchService implements EventSubscriber {
    private static final Set<String> DEPENDENCY_BUNDLES = Set.of("javax.measure.unit-api", "org.openhab.core",
            "org.openhab.core.model.script", "org.openhab.core.thing", "org.openhab.core.persistence",
            "org.openhab.core.automation", "org.smarthomej.automation.javarule",
            "org.ops4j.pax.logging.pax-logging-api");
    private static final Set<ThingStatus> INITIALIZED = Set.of(ThingStatus.ONLINE, ThingStatus.OFFLINE,
            ThingStatus.UNKNOWN);
    private static final Set<String> ACTION_EVENTS = Set.of(ThingStatusInfoChangedEvent.TYPE);
    private static final Set<String> ITEM_EVENTS = Set.of(ItemAddedEvent.TYPE, ItemRemovedEvent.TYPE);
    private static final Set<String> THING_EVENTS = Set.of(ThingAddedEvent.TYPE, ThingRemovedEvent.TYPE);
    private static final Set<String> EVENTS = Stream.of(ACTION_EVENTS, ITEM_EVENTS, THING_EVENTS).flatMap(Set::stream)
            .collect(Collectors.toSet());

    private final Logger logger = LoggerFactory.getLogger(CompilerService.class);

    private final BundleContext bundleContext;

    private final Path tempFolder;
    private final ClassGenerator classGenerator;
    private final JavaRuleDiagnosticCollector<JavaFileObject> diagnostics = new JavaRuleDiagnosticCollector<>();
    private final JavaRuleFileManager<? extends JavaFileManager> fileManager;

    @Activate
    public CompilerService(@Reference ItemRegistry itemRegistry, @Reference ThingRegistry thingRegistry,
            Map<String, Object> properties, BundleContext bundleContext) throws IOException {
        super(LIB_DIR.toString());

        try {
            Files.createDirectories(LIB_DIR);
        } catch (IOException e) {
            logger.warn("Failed to create directory '{}': {}", LIB_DIR, e.getMessage());
            throw new IllegalStateException("Failed to initialize lib folder.");
        }

        if (!Files.isWritable(LIB_DIR) || !Files.isReadable(LIB_DIR)) {
            logger.warn("Directory '{}' must be available for read and write", LIB_DIR);
            throw new IllegalStateException("Failed to initialize lib folder.");
        }

        this.bundleContext = bundleContext;
        this.tempFolder = Files.createTempDirectory("javarule");
        this.classGenerator = new ClassGenerator(this.tempFolder, itemRegistry, thingRegistry, bundleContext);

        this.fileManager = new JavaRuleFileManager<>(
                ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, null),
                bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader());

        // build initial dependencies and add them to the file manager
        Set<String> additionalBundles = new HashSet<>(DEPENDENCY_BUNDLES);
        String additionalBundlesConfig = (String) properties.getOrDefault("additionalBundles", "");
        additionalBundles.addAll(Arrays.asList(additionalBundlesConfig.split(",")));
        logger.debug("Adding '{}' to {}.", additionalBundles, CORE_DEPENDENCY_JAR);
        createCoreDependencies(additionalBundles);
        fileManager.rebuildLibPackages();

        // build the complete set
        classGenerator.generateItems();
        classGenerator.generateThings();
        classGenerator.generateThingActions();
        copyAdditionalSources();
        buildJavaRuleDependenciesJar();
        fileManager.rebuildLibPackages();
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        // delete all files in temp folder, adapted from https://stackoverflow.com/a/20280989
        try {
            Files.walkFileTree(tempFolder, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(@Nullable Path file, @Nullable BasicFileAttributes attrs)
                        throws IOException {
                    if (file != null) {
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(@Nullable Path dir, @Nullable IOException e)
                        throws IOException {
                    if (e != null) {
                        // we already errored, re-throw
                        throw e;
                    }
                    if (dir != null) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to delete temp folder '{}': {}", tempFolder, e.getMessage());
        }
    }

    public MemoryFileManager getMemoryFileManager() {
        return new MemoryFileManager(fileManager, fileManager.getClassLoader(StandardLocation.CLASS_PATH));
    }

    /**
     * Compile a list of java files
     *
     * @param sourceFiles A {@link List} of {@link JavaFileObject} that shall be compiled
     * @param fileManager The {@link JavaFileManager} to use. Must be
     *            able to resolve the {@param sourceFiles}
     * @param options A {@link List} of compiler options.
     *
     * @throws RuntimeException in case the compilation fails
     */
    public void compile(List<JavaFileObject> sourceFiles, JavaFileManager fileManager, @Nullable List<String> options)
            throws CompilerException {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null,
                    sourceFiles);

            if (!task.call()) {
                String message = diagnostics.getDiagnostics().stream().map(Object::toString)
                        .collect(Collectors.joining("\n"));
                throw new IllegalStateException(message);
            }
        } catch (RuntimeException e) {
            throw new CompilerException(e);
        }
    }

    /*
     * create core dependencies, this only needs to be done once since the service re-initialized if the configuration
     * changes
     */
    private void createCoreDependencies(Set<String> additionalBundles) {
        Lock fileManagerLock = fileManager.getFileManagerLock();
        fileManagerLock.lock();
        try (FileOutputStream outFile = new FileOutputStream(JavaRuleConstants.CORE_DEPENDENCY_JAR.toFile())) {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            JarOutputStream target = new JarOutputStream(outFile, manifest);

            Arrays.stream(bundleContext.getBundles())
                    .filter(bundle -> additionalBundles.contains(bundle.getSymbolicName()))
                    .forEach(bundle -> copyExportedClasses(bundle, target));

            target.close();
        } catch (IOException e) {
            logger.warn("Failed to create '{}': {}", JavaRuleConstants.CORE_DEPENDENCY_JAR, e.getMessage());
        } finally {
            fileManagerLock.unlock();
        }
    }

    private void copyExportedClasses(Bundle bundle, JarOutputStream target) {
        String exportPackage = bundle.getHeaders().get("Export-Package");
        if (exportPackage == null) {
            logger.warn("Bundle '{}' does not export any package!", bundle.getSymbolicName());
            return;
        }
        List<String> exportedPackages = Arrays.stream(exportPackage //
                .split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) // split only on comma not in double quotes
                .map(s -> s.split(";")[0]) // get only package name and drop uses, version, etc.
                .map(b -> b.replace(".", "/")).collect(Collectors.toList());

        bundle.adapt(BundleWiring.class).listResources("", "*.class", LISTRESOURCES_LOCAL + LISTRESOURCES_RECURSE)
                .forEach(classFile -> {
                    try {
                        int classNameStart = classFile.lastIndexOf("/");
                        if (classNameStart == -1
                                || !exportedPackages.contains(classFile.substring(0, classNameStart))) {
                            return;
                        }

                        URL urlEntry = bundle.getEntry(classFile);
                        if (urlEntry == null) {
                            logger.warn("URL for {} is empty, skipping", classFile);
                        } else {
                            addEntryToJar(target, classFile, 0, urlEntry.openStream());
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to copy class '{}' from '{}': {}", classFile, bundle.getSymbolicName(),
                                e.getMessage());
                    }
                });
    }

    private void copyAdditionalSources() {
        try (Stream<Path> pathStream = Files.walk(LIB_DIR, MAX_VALUE)) {
            List<Path> javaSourceFiles = pathStream.filter(JavaRuleConstants.JAVA_FILE_FILTER).filter(Files::isReadable)
                    .collect(Collectors.toList());
            for (Path source : javaSourceFiles) {
                Path target = tempFolder.resolve(LIB_DIR.relativize(source));
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.warn("Failed to copy additional source files, helper libraries not available: {}", e.getMessage());
        }
    }

    /*
     * build the javarule-dependency.jar
     */
    private void buildJavaRuleDependenciesJar() {
        Lock fileManagerLock = fileManager.getFileManagerLock();
        fileManagerLock.lock();
        try {
            try (Stream<Path> pathStream = Files.walk(tempFolder, MAX_VALUE)) {
                List<JavaFileObject> javaSourceFiles = pathStream.filter(JavaRuleConstants.JAVA_FILE_FILTER)
                        .filter(Files::isReadable).map(Path::toUri).map(JavaRuleFileObject::sourceFileObject)
                        .collect(Collectors.toList());

                logger.trace("Compiling java sources: {}", javaSourceFiles);
                compile(javaSourceFiles, fileManager, List.of("-Xlint:unchecked", "-Xlint:varargs"));
                logger.debug("Compilation of classes successful!");
            }

            try (FileOutputStream outFile = new FileOutputStream(JavaRuleConstants.JAVARULE_DEPENDENCY_JAR.toFile())) {
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                JarOutputStream target = new JarOutputStream(outFile, manifest);

                try (Stream<Path> helperPath = Files.walk(tempFolder, MAX_VALUE)) {
                    helperPath.filter(JavaRuleConstants.CLASS_FILE_FILTER).forEach(f -> addClassToJar(f, target));
                }
                target.close();
            }

            logger.debug("Finished generating helper libraries.");
        } catch (CompilerException | IOException e) {
            logger.warn("Failed to generate class files, helper libraries not available: {}", e.getMessage());
        } finally {
            fileManagerLock.unlock();
        }
    }

    private void addClassToJar(Path path, JarOutputStream jar) {
        String name = tempFolder.relativize(path).toString();
        int extensionSeparator = name.lastIndexOf(".");
        name = name.substring(0, extensionSeparator).replace(".", "/").concat(name.substring(extensionSeparator));

        try {
            logger.trace("Adding {}", path);
            File file = path.toFile();
            try (FileInputStream in = new FileInputStream(file)) {
                addEntryToJar(jar, name, file.lastModified(), in);
            }
        } catch (IOException e) {
            logger.warn("Failed to add {} to jar: {}", path, e.getMessage());
        }
    }

    private void addEntryToJar(JarOutputStream jar, String name, long lastModified, InputStream content)
            throws IOException {
        JarEntry jarEntry = new JarEntry(name);
        if (lastModified != 0) {
            jarEntry.setTime(lastModified);
        }
        jar.putNextEntry(jarEntry);
        jar.write(content.readAllBytes());
        jar.closeEntry();
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
    public void processWatchEvent(@Nullable WatchEvent<?> event, @Nullable Kind<?> kind, @Nullable Path path) {
        if (path == null || kind == null || (kind != ENTRY_CREATE && kind != ENTRY_MODIFY && kind != ENTRY_DELETE)) {
            logger.trace("Received '{}' for path '{}' - ignoring (null or wrong kind)", kind, path);
            return;
        }
        if (path.getFileName().toString().endsWith(JAR_FILE_TYPE)) {
            fileManager.rebuildLibPackages();
        } else if (path.getFileName().toString().endsWith(JAVA_FILE_TYPE)) {
            try {
                Path targetPath = tempFolder.resolve(LIB_DIR.relativize(path));
                if (kind == ENTRY_DELETE) {
                    if (!Files.deleteIfExists(targetPath)) {
                        // file did not exist, no need to rebuild
                        return;
                    }
                } else if (kind == ENTRY_MODIFY) {
                    if (Files.exists(targetPath) && Files.readString(path).equals(Files.readString(targetPath))) {
                        // file already exists and has same content, no need to rebuild
                        return;
                    }
                }
                Files.createDirectories(targetPath.getParent());
                Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                buildJavaRuleDependenciesJar();
            } catch (IOException e) {
                logger.warn("Failed to process event '{}' for '{}': {}", kind, path, e.getMessage());
            }
        } else {
            logger.trace("Received '{}' for path '{}' - ignoring (wrong extension)", kind, path);
        }
    }

    /*
     * process events for thing actions and items
     */
    @Override
    public Set<String> getSubscribedEventTypes() {
        return EVENTS;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        String eventType = event.getType();
        if (ACTION_EVENTS.contains(eventType)) {
            ThingStatusInfoChangedEvent event1 = (ThingStatusInfoChangedEvent) event;
            if ((ThingStatus.INITIALIZING.equals(event1.getOldStatusInfo().getStatus())
                    && INITIALIZED.contains(event1.getStatusInfo().getStatus()))
                    || (ThingStatus.UNINITIALIZED.equals(event1.getStatusInfo().getStatus())
                            && INITIALIZED.contains(event1.getOldStatusInfo().getStatus()))) {
                // only regenerate jar if things are changing to or from an initialized
                try {
                    if (classGenerator.generateThingActions()) {
                        buildJavaRuleDependenciesJar();
                    }
                } catch (IOException e) {
                    logger.warn("Failed to (re-)build thing action classes: {}", e.getMessage());
                }
            }
        } else if (ITEM_EVENTS.contains(eventType)) {
            logger.debug("Added/updated item: {}", event);
            try {
                classGenerator.generateItems();
                buildJavaRuleDependenciesJar();
            } catch (IOException e) {
                logger.warn("Failed to (re-)build item class: {}", e.getMessage());
            }
        } else if (THING_EVENTS.contains(eventType)) {
            logger.debug("Added/updated thing: {}", event);
            try {
                classGenerator.generateThings();
                buildJavaRuleDependenciesJar();
            } catch (IOException e) {
                logger.warn("Failed to (re-)build thing class: {}", e.getMessage());
            }
        }
    }

    /*
     * we need to clear the diagnostic list after each run, the default implementation just adds to the end
     */
    private static class JavaRuleDiagnosticCollector<S> implements DiagnosticListener<S> {
        private final List<Diagnostic<? extends S>> diagnostics = new CopyOnWriteArrayList<>();

        @Override
        public void report(@Nullable Diagnostic<? extends S> diagnostic) {
            Objects.requireNonNull(diagnostic);
            diagnostics.add(diagnostic);
        }

        public List<Diagnostic<? extends S>> getDiagnostics() {
            List<Diagnostic<? extends S>> list = new ArrayList<>(diagnostics);
            diagnostics.clear();
            return list;
        }
    }
}
