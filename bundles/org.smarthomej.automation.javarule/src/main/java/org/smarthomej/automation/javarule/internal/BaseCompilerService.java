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

import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_LOCAL;
import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_RECURSE;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.DEPENDENCY_JAR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.DEPLIB_DIR;
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.WORKING_DIR;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.automation.javarule.Util;

/**
 * The {@link BaseCompilerService} compiled the dependency jar and provides the compile service
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = BaseCompilerService.class, configurationPid = "automation.javarule")
public class BaseCompilerService {
    private static final List<String> DEPENDENCY_BUNDLES = List.of("javax.measure.unit-api", "org.openhab.core",
            "org.openhab.core.model.script", "org.openhab.core.thing", "org.openhab.core.persistence",
            "org.smarthomej.automation.javarule");

    private final Logger logger = LoggerFactory.getLogger(BaseCompilerService.class);
    private final JavaRuleJavaFileManager fileManager;

    @Activate
    public BaseCompilerService(@Reference JavaRuleJavaFileManager fileManager, Map<String, Object> properties) {
        if (!(Util.checkFolder(WORKING_DIR) && Util.checkFolder(DEPLIB_DIR))) {
            throw new IllegalStateException("Failed to initialize folders!");
        }

        this.fileManager = fileManager;

        createDependencyJar();
    }

    private boolean inExportedPackage(String clazz, List<String> exportedPackages) {
        int classNameStart = clazz.lastIndexOf("/");
        if (classNameStart == -1) {
            return false;
        }
        return exportedPackages.contains(clazz.substring(0, classNameStart));
    }

    private void createDependencyJar() {
        try (FileOutputStream outFile = new FileOutputStream(DEPLIB_DIR.resolve(DEPENDENCY_JAR).toFile())) {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            JarOutputStream target = new JarOutputStream(outFile, manifest);

            BundleContext context = FrameworkUtil.getBundle(Util.class).getBundleContext();
            List<Bundle> bundles = Arrays.stream(context.getBundles())
                    .filter(bundle -> DEPENDENCY_BUNDLES.contains(bundle.getSymbolicName()))
                    .collect(Collectors.toList());

            for (Bundle bundle : bundles) {
                List<String> exportedPackages = Arrays
                        .stream(bundle.getHeaders().get("Export-Package").replaceAll("\".*?\"", "").split(","))
                        .map(p -> p.split(";")[0].replaceAll("\\.", "/")).collect(Collectors.toList());

                List<String> entries = bundle.adapt(BundleWiring.class)
                        .listResources("", "*.class", LISTRESOURCES_LOCAL + LISTRESOURCES_RECURSE).stream()
                        .filter(c -> inExportedPackage(c, exportedPackages)).collect(Collectors.toList());

                for (String entry : entries) {
                    URL urlEntry = bundle.getEntry(entry);
                    if (urlEntry == null) {
                        logger.warn("URL for {} is empty, skipping", entry);
                        continue;
                    }
                    byte[] content = urlEntry.openStream().readAllBytes();
                    JarEntry jarEntry = new JarEntry(entry);
                    target.putNextEntry(jarEntry);
                    target.write(content);
                    target.closeEntry();
                }
            }
            target.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create jar");
        }
    }

    /**
     * Compile one or more java files to classes
     *
     * @param sourcePath the input path
     *
     * @return true if at least one file was compiles
     */
    public boolean compile(Path sourcePath) {
        try {
            List<URI> javaSourceFiles;

            if (Files.isDirectory(sourcePath)) {
                try (Stream<Path> pathStream = Files.list(sourcePath)) {
                    List<Path> files = pathStream.collect(Collectors.toList());
                    List<String> javaClassFiles = files.stream().filter(JavaRuleConstants.CLASS_FILE_FILTER)
                            .map(Path::toString).map(Util::removeExtension).collect(Collectors.toList());
                    javaSourceFiles = files.stream().filter(JavaRuleConstants.JAVA_FILE_FILTER)
                            .filter(source -> !javaClassFiles.contains(Util.removeExtension(source.toString())))
                            .filter(Files::isReadable).map(Path::toUri).collect(Collectors.toList());
                }
            } else {
                // compile a single file only
                javaSourceFiles = List.of(sourcePath.toUri());
            }

            if (javaSourceFiles.isEmpty()) {
                logger.debug("Nothing to compile.");
                return false;
            }

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            fileManager.addBundles(DEPENDENCY_BUNDLES);
            DiagnosticCollector<JavaFileObject> diagnostics = fileManager.getDiagnostics();

            List<String> optionList = List.of("-Xlint:unchecked", "-Xlint:varargs");
            logger.debug("Compiling java sources: {}", javaSourceFiles);

            List<? extends JavaFileObject> compilationUnit = javaSourceFiles.stream()
                    .map(JavaRuleJavaFileObject::sourceFileObject).collect(Collectors.toList());

            final JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, optionList, null,
                    compilationUnit);
            if (task.call()) {
                logger.debug("Compilation of classes successful!");
            } else {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    logger.warn("Found error {}/{} in {}: {}", diagnostic.getLineNumber(), diagnostic.getColumnNumber(),
                            diagnostic.getSource(), diagnostic.getMessage(Locale.getDefault()));
                }
            }
            fileManager.close();
        } catch (RuntimeException | IOException e) {
            logger.warn("Compiler failed: {}", e.getMessage(), e);
            return false;
        }

        return true;
    }
}
