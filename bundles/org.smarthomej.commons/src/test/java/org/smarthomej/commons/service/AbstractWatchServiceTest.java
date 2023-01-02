/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.smarthomej.commons.service;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.test.java.JavaTest;

/**
 * Test for {@link AbstractWatchService}.
 *
 * @author Dimitar Ivanov - Initial contribution
 * @author Svilen Valkanov - Tests are modified to run on different Operating Systems
 * @author Ana Dimova - reduce to a single watch thread for all class instances
 * @author Simon Kaufmann - ported it from Groovy to Java
 * @author Jan N. Klug - null annotations
 */
@NonNullByDefault
public class AbstractWatchServiceTest extends JavaTest {
    private static final String WATCHED_DIRECTORY = "watchDirectory";

    // Fail if no event has been received within the given timeout
    private static int noEventTimeoutInSeconds;

    private @Nullable RelativeWatchService watchService;

    @BeforeAll
    public static void setUpBeforeClass() {
        // set the NO_EVENT_TIMEOUT_IN_SECONDS according to the operating system used
        if (Objects.requireNonNullElse(System.getProperty("os.name"), "").startsWith("Mac OS X")) {
            noEventTimeoutInSeconds = 10;
        } else {
            noEventTimeoutInSeconds = 3;
        }
    }

    @BeforeEach
    public void setup() {
        File watchDir = new File(WATCHED_DIRECTORY);
        watchDir.mkdirs();
    }

    @AfterEach
    public void tearDown() throws Exception {
        RelativeWatchService watchService = this.watchService;
        if (watchService == null) {
            return;
        }
        watchService.deactivate();
        Assertions.assertNull(watchService.watchQueueReader.getWatchService());
        final Path watchedDirectory = Paths.get(WATCHED_DIRECTORY);
        if (Files.exists(watchedDirectory)) {
            try (Stream<Path> walk = Files.walk(watchedDirectory)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
        watchService.allFullEvents.clear();
    }

    @Test
    public void testInRoot() throws Exception {
        RelativeWatchService watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);
        this.watchService = watchService;

        // File created in the watched directory
        assertByRelativePath(watchService, "rootWatchFile");
    }

    @Test
    public void testInSub() throws Exception {
        RelativeWatchService watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);
        this.watchService = watchService;

        // File created in a subdirectory of the watched directory
        assertByRelativePath(watchService, "subDir" + File.separatorChar + "subDirWatchFile");
    }

    @Test
    public void testInSubSub() throws Exception {
        RelativeWatchService watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);
        this.watchService = watchService;

        // File created in a sub sub directory of the watched directory
        assertByRelativePath(watchService,
                "subDir" + File.separatorChar + "subSubDir" + File.separatorChar + "innerWatchFile");
    }

    @Test
    public void testIdenticalNames() throws Exception {
        RelativeWatchService watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);
        this.watchService = watchService;

        String fileName = "duplicateFile";
        String innerFileName = "duplicateDir" + File.separatorChar + fileName;

        File innerfile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName);
        Objects.requireNonNull(innerfile.getParentFile()).mkdirs();

        // Activate the service when the subdir is also present. Else the subdir will not be registered
        watchService.activate();

        innerfile.createNewFile();

        // Assure that the ordering of the events will be always the same
        Thread.sleep(noEventTimeoutInSeconds * 1000);

        new File(WATCHED_DIRECTORY + File.separatorChar + fileName).createNewFile();

        assertEventCount(watchService, 2);

        FullEvent innerFileEvent = watchService.allFullEvents.get(0);
        assertThat(innerFileEvent.eventKind, is(ENTRY_CREATE));
        assertThat(innerFileEvent.eventPath.toString(), is(WATCHED_DIRECTORY + File.separatorChar + innerFileName));

        FullEvent fileEvent = watchService.allFullEvents.get(1);
        assertThat(fileEvent.eventKind, is(ENTRY_CREATE));
        assertThat(fileEvent.eventPath.toString(), is(WATCHED_DIRECTORY + File.separatorChar + fileName));
    }

    @Test
    public void testExcludeSubdirs() throws Exception {
        // Do not watch the subdirectories of the root directory
        RelativeWatchService watchService = new RelativeWatchService(WATCHED_DIRECTORY, false);
        this.watchService = watchService;

        String innerFileName = "watchRequestSubDir" + File.separatorChar + "watchRequestInnerFile";

        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName);
        Objects.requireNonNull(innerFile.getParentFile()).mkdirs();

        watchService.activate();

        // Consequent creation and deletion in order to generate any watch events for the subdirectory
        innerFile.createNewFile();
        innerFile.delete();

        assertNoEventsAreProcessed(watchService);
    }

    @Test
    public void testIncludeSubdirs() throws Exception {
        // Do watch the subdirectories of the root directory
        RelativeWatchService watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);
        this.watchService = watchService;

        String innerFileName = "watchRequestSubDir" + File.separatorChar + "watchRequestInnerFile";
        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName);
        // Make all the subdirectories before running the service
        Objects.requireNonNull(innerFile.getParentFile()).mkdirs();

        watchService.activate();

        innerFile.createNewFile();
        assertFileCreateEventIsProcessed(watchService, innerFile, innerFileName);

        watchService.allFullEvents.clear();
        assertNoEventsAreProcessed(watchService);
    }

    private void assertNoEventsAreProcessed(RelativeWatchService watchService) throws Exception {
        // Wait for a possible event for the maximum timeout
        Thread.sleep(noEventTimeoutInSeconds * 1000);

        assertEventCount(watchService, 0);
    }

    private void assertFileCreateEventIsProcessed(RelativeWatchService watchService, File innerFile,
            String innerFileName) {
        // Single event for file creation is present
        assertEventCount(watchService, 1);
        FullEvent fileEvent = watchService.allFullEvents.get(0);
        assertThat(fileEvent.eventKind, is(ENTRY_CREATE));
        assertThat(fileEvent.eventPath.toString(), is(WATCHED_DIRECTORY + File.separatorChar + innerFileName));
    }

    private void assertByRelativePath(RelativeWatchService watchService, String fileName) throws Exception {
        File file = new File(WATCHED_DIRECTORY + File.separatorChar + fileName);
        Objects.requireNonNull(file.getParentFile()).mkdirs();

        assertThat(file.exists(), is(false));

        // We have to be sure that all the subdirectories of the watched directory are created when the watched service
        // is activated
        watchService.activate();

        file.createNewFile();
        fullEventAssertionsByKind(watchService, fileName, ENTRY_CREATE, false);

        // File modified
        Files.writeString(file.toPath(), "Additional content", StandardOpenOption.APPEND);
        fullEventAssertionsByKind(watchService, fileName, ENTRY_MODIFY, false);

        // File modified but identical content
        Files.writeString(file.toPath(), "Additional content", StandardOpenOption.TRUNCATE_EXISTING);
        assertNoEventsAreProcessed(watchService);

        // File deleted
        file.delete();
        fullEventAssertionsByKind(watchService, fileName, ENTRY_DELETE, true);
    }

    private void assertEventCount(RelativeWatchService watchService, int expected) {
        try {
            waitForAssert(() -> assertThat(watchService.allFullEvents.size(), is(expected)));
        } catch (AssertionError e) {
            watchService.allFullEvents.forEach(event -> event.toString());
            throw e;
        }
    }

    private void fullEventAssertionsByKind(RelativeWatchService watchService, String fileName, Kind<?> kind,
            boolean osSpecific) throws Exception {
        waitForAssert(() -> assertThat(!watchService.allFullEvents.isEmpty(), is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);

        if (osSpecific && ENTRY_DELETE.equals(kind)) {
            // There is possibility that one more modify event is triggered on some OS
            // so sleep a bit extra time
            Thread.sleep(500);
            cleanUpOsSpecificModifyEvent(watchService);
        }

        assertEventCount(watchService, 1);
        FullEvent fullEvent = watchService.allFullEvents.get(0);

        assertThat(fullEvent.eventPath.toString(), is(WATCHED_DIRECTORY + File.separatorChar + fileName));
        assertThat(fullEvent.eventKind, is(kind));
        assertThat(fullEvent.watchEvent.count() >= 1, is(true));
        assertThat(fullEvent.watchEvent.kind(), is(fullEvent.eventKind));
        String fileNameOnly = fileName.contains(File.separatorChar + "")
                ? fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1, fileName.length())
                : fileName;
        assertThat(Objects.requireNonNullElse(fullEvent.watchEvent.context(), "").toString(), is(fileNameOnly));

        // Clear all the asserted events
        watchService.allFullEvents.clear();
    }

    /**
     * Cleanup the OS specific ENTRY_MODIFY event as it will not be needed for the assertion
     */
    private void cleanUpOsSpecificModifyEvent(RelativeWatchService watchService) {
        // As the implementation of the watch events is OS specific, it can happen that when the file is deleted two
        // events are fired - ENTRY_MODIFY followed by an ENTRY_DELETE
        // This is usually observed on Windows and below is the workaround
        // Related discussion in StackOverflow:
        // http://stackoverflow.com/questions/28201283/watchservice-windows-7-when-deleting-a-file-it-fires-both-entry-modify-and-e
        boolean isDeletedWithPrecedingModify = watchService.allFullEvents.size() == 2
                && ENTRY_MODIFY.equals(watchService.allFullEvents.get(0).eventKind);
        if (isDeletedWithPrecedingModify) {
            // Remove the ENTRY_MODIFY element as it is not needed
            watchService.allFullEvents.remove(0);
        }
    }

    private static class RelativeWatchService extends AbstractWatchService {
        boolean watchSubDirs;

        // Synchronize list as several watcher threads can write into it
        public volatile List<FullEvent> allFullEvents = new CopyOnWriteArrayList<>();

        RelativeWatchService(String rootPath, boolean watchSubDirectories) {
            super(rootPath);
            watchSubDirs = watchSubDirectories;
        }

        @Override
        protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
            FullEvent fullEvent = new FullEvent(event, kind, path);
            allFullEvents.add(fullEvent);
        }

        @Override
        protected Kind<?>[] getWatchEventKinds(Path subDir) {
            return new Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
        }

        @Override
        protected boolean watchSubDirectories() {
            return watchSubDirs;
        }
    }

    private static class FullEvent {
        WatchEvent<?> watchEvent;
        Kind<?> eventKind;
        Path eventPath;

        public FullEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
            watchEvent = event;
            eventKind = kind;
            eventPath = path;
        }

        @Override
        public String toString() {
            return "Watch Event: count " + watchEvent.count() + "; kind: " + eventKind + "; path: " + eventPath;
        }
    }
}
