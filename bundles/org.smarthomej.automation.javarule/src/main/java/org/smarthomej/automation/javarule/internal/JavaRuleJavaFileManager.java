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
import static org.smarthomej.automation.javarule.internal.JavaRuleConstants.CLASS_FILE_TYPE;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.smarthomej.automation.javarule.Util;

/**
 * The {@link JavaRuleJavaFileManager} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
public class JavaRuleJavaFileManager<M extends JavaFileManager> extends ForwardingJavaFileManager<M> {

    private final Map<String, List<URI>> additionalPackages = new HashMap<>();

    /**
     * Creates a new instance of ForwardingJavaFileManager.
     *
     * @param fileManager delegate to this file manager
     * @param additionalBundles a list of additional OSGi bundles
     */
    public JavaRuleJavaFileManager(M fileManager, List<String> additionalBundles) {
        super(ToolProvider.getSystemJavaCompiler().getStandardFileManager());


        BundleContext context = FrameworkUtil.getBundle(Util.class).getBundleContext();
        List<Bundle> bundles = Arrays.stream(context.getBundles())
                .filter(bundle -> additionalBundles.contains(bundle.getSymbolicName())).collect(Collectors.toList());
        bundles.forEach(bundle -> {
            List<String> exportedPackages = Arrays.stream(getExportedPackages(bundle)).map(p -> p.split(";")[0])
                    .collect(Collectors.toList());

            exportedPackages.forEach(packageName -> {
                List<URI> entries = bundle.adapt(BundleWiring.class)
                        .listResources("", "*.class", LISTRESOURCES_LOCAL + LISTRESOURCES_RECURSE).stream()
                        .map(className -> uriFromClassName(className, packageName, bundle)).filter(Optional::isPresent)
                        .map(Optional::get).collect(Collectors.toList());

                Objects.requireNonNull(additionalPackages.computeIfAbsent(packageName, k -> new ArrayList<>()))
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

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
            boolean recurse) throws IOException {
        Iterable<JavaFileObject> stdResult = fileManager.list(location, packageName, kinds, recurse);

        if (location != StandardLocation.CLASS_PATH || !kinds.contains(JavaFileObject.Kind.CLASS)) {
            return stdResult;
        }
        List<JavaFileObject> mergedFileObjects = new ArrayList<>();
        stdResult.forEach(mergedFileObjects::add);

        List<URI> additionalClasses = additionalPackages.getOrDefault(packageName, List.of());
        additionalClasses.stream()
                .map(classUri -> new JavaRuleSimpleJavaFileObject(classUri, JavaFileObject.Kind.CLASS))
                .forEach(mergedFileObjects::add);

        return mergedFileObjects;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
            FileObject sibling) throws IOException {
        if (sibling instanceof JavaRuleSimpleJavaFileObject) {
            URI outFile = URI.create(Util.removeExtension(sibling.toUri().toString()) + CLASS_FILE_TYPE);
            return new JavaRuleSimpleJavaFileObject(outFile, JavaFileObject.Kind.CLASS);
        }
        return fileManager.getJavaFileForOutput(location, className, kind, sibling);
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof JavaRuleSimpleJavaFileObject) {
            return Util.removeExtension(file.toUri().getPath().replaceAll("/", ".").substring(1));
        }
        return fileManager.inferBinaryName(location, file);
    }
}
