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

import io.github.pnoker.common.agentic.config.AgenticProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Best-effort filesystem cleanup for stored conversation attachments.
 *
 * <p>Owns the attachment storage-root policy so upload path resolution and delete-time
 * file unlinking share one root and one boundary guard and can never drift apart. All
 * operations are warn-only: a missing, undeletable, or out-of-root file must never fail
 * the database side of an attachment or session delete.
 *
 * @author pnoker
 * @since 2026.9.22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttachmentFileCleaner {

    private final AgenticProperties properties;

    private Path storageRoot;

    @PostConstruct
    void initializeStorageRoot() {
        storageRoot = storageRoot();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Attachment storage directory create failed", exception);
        }
    }

    /** Unlink one stored attachment file, resolving it and enforcing the storage-root boundary. */
    public Mono<Void> unlink(String storedFilePath) {
        if (StringUtils.isBlank(storedFilePath)) return Mono.empty();
        return Mono.fromRunnable(() -> {
            try {
                Files.deleteIfExists(resolveWithinRoot(storedFilePath));
            } catch (Exception exception) {
                log.warn("Attachment file cleanup failed, file={}", storedFilePath, exception);
            }
        });
    }

    /** Unlink one already-resolved attachment file, used for upload rollback of just-created paths. */
    Mono<Void> unlink(Path path) {
        return Mono.fromRunnable(() -> {
            try {
                Files.deleteIfExists(path);
            } catch (Exception exception) {
                log.warn("Attachment file cleanup failed, file={}", path, exception);
            }
        });
    }

    /** Unlink every stored attachment file in the collection, each best-effort. */
    public Mono<Void> unlinkAll(Collection<String> storedFilePaths) {
        if (storedFilePaths == null || storedFilePaths.isEmpty()) return Mono.empty();
        return Flux.fromIterable(storedFilePaths).flatMap(this::unlink).then();
    }

    /**
     * The normalized absolute attachment storage root; safe to call before container
     * initialization by recomputing it from the configured storage path.
     */
    Path storageRoot() {
        return storageRoot == null
                ? Paths.get(properties.getAttachmentStoragePath())
                        .toAbsolutePath()
                        .normalize()
                : storageRoot;
    }

    private Path resolveWithinRoot(String storedFilePath) {
        Path path = Paths.get(storedFilePath).toAbsolutePath().normalize();
        if (!path.startsWith(storageRoot())) throw new IllegalStateException("Attachment file path is invalid");
        return path;
    }
}
