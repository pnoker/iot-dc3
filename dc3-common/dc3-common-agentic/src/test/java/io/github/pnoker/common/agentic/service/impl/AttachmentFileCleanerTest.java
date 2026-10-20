/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.common.agentic.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Unit coverage of the attachment file cleaner's blocking-IO offload: the NIO unlink must run on
 * the injected scheduler (bounded elastic in production) so no caller can execute file deletion
 * on a Reactor Netty event loop, while the storage-root boundary guard and best-effort contract
 * stay intact.
 */
class AttachmentFileCleanerTest {

    @TempDir
    Path storageRoot;

    private final ConcurrentLinkedQueue<String> unlinkThreads = new ConcurrentLinkedQueue<>();
    private ExecutorServiceHolder holder;

    private AttachmentFileCleaner cleaner;

    /** Keeps a reference to the probe executor so the test can shut it down. */
    private record ExecutorServiceHolder(java.util.concurrent.ExecutorService service) {}

    @BeforeEach
    void setUp() {
        AgenticProperties properties = new AgenticProperties();
        properties.setAttachmentStoragePath(storageRoot.toString());
        java.util.concurrent.ExecutorService probe = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "probe-unlink");
            thread.setDaemon(true);
            return thread;
        });
        holder = new ExecutorServiceHolder(probe);
        // The probe executor records the thread that actually performs each task, proving the
        // cleaner offloads instead of running on the subscribing thread.
        Executor instrumented = command -> probe.execute(() -> {
            unlinkThreads.add(Thread.currentThread().getName());
            command.run();
        });
        Scheduler scheduler = Schedulers.fromExecutor(instrumented);
        cleaner = new AttachmentFileCleaner(properties, scheduler);
        cleaner.initializeStorageRoot();
    }

    @AfterEach
    void tearDown() {
        holder.service().shutdownNow();
    }

    @Test
    void unlinkRunsTheBlockingDeletionOnTheSchedulerThread() throws Exception {
        Path file = Files.writeString(storageRoot.resolve("note.txt"), "payload");
        String subscribingThread = Thread.currentThread().getName();

        cleaner.unlink(file.toString()).block();

        assertThat(Files.exists(file)).as("the file must be deleted").isFalse();
        assertThat(unlinkThreads)
                .as("the unlink must run on the scheduler thread, not on the subscriber")
                .singleElement()
                .isNotEqualTo(subscribingThread)
                .satisfies(thread -> assertThat(thread).startsWith("probe-unlink"));
    }

    @Test
    void unlinkAllRunsEveryDeletionOnTheSchedulerThread() throws Exception {
        Path first = Files.writeString(storageRoot.resolve("a.txt"), "payload");
        Path second = Files.writeString(storageRoot.resolve("b.txt"), "payload");

        cleaner.unlinkAll(List.of(first.toString(), second.toString(), " ")).block();

        assertThat(Files.exists(first)).isFalse();
        assertThat(Files.exists(second)).isFalse();
        assertThat(unlinkThreads).as("each unlink must be offloaded").hasSize(2);
    }

    @Test
    void unlinkSkipsBlankPathsAndRefusesPathsOutsideTheStorageRoot() throws Exception {
        Path outside = Files.createTempDirectory("dc3-cleaner-outside");
        Path escaped = Files.writeString(outside.resolve("escaped.txt"), "payload");

        cleaner.unlink(" ").block();
        assertThat(unlinkThreads).as("blank paths must not schedule any work").isEmpty();

        cleaner.unlink(escaped.toString()).block();
        assertThat(Files.exists(escaped))
                .as("the boundary guard must refuse out-of-root unlinks")
                .isTrue();
    }
}
