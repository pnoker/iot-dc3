/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.agentic.service.impl;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.agentic.repository.ReactiveAttachmentStore;
import io.github.pnoker.common.agentic.service.AttachmentService;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.RequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import io.github.pnoker.common.utils.UuidV7;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

/** Reactive attachment upload, listing, and metadata summarization. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final long MAX_BYTES = 10 * 1024 * 1024;

    private final ReactiveAttachmentStore attachmentStore;
    private final SessionService sessionService;
    private final AgenticProperties properties;

    private Path storageRoot;

    @PostConstruct
    void initializeStorageRoot() {
        storageRoot = Paths.get(properties.getAttachmentStoragePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Attachment storage directory create failed", exception);
        }
    }


    @Override
    public Mono<AttachmentBO> upload(String conversationId, FilePart filePart, RequestHeader.PrincipalHeader header) {
        if (StringUtils.isBlank(conversationId) || filePart == null || StringUtils.isBlank(filePart.filename())
                || header == null) {
            return Mono.error(new RequestException("Attachment data is required"));
        }
        long declaredLength = filePart.headers().getContentLength();
        if (declaredLength > MAX_BYTES) return Mono.error(new RequestException("Attachment size exceeds 10 MB"));
        Path path;
        try {
            path = resolveFilePath(conversationId, header, filePart.filename());
        } catch (RuntimeException exception) {
            return Mono.error(exception);
        }
        AtomicLong bytes = new AtomicLong();
        Path finalPath = path;
        Mono<Void> write = Mono.usingWhen(open(finalPath), channel -> {
            Flux<DataBuffer> bounded = filePart.content().handle((buffer, sink) -> {
                long next = bytes.addAndGet(buffer.readableByteCount());
                if (next > MAX_BYTES) {
                    DataBufferUtils.release(buffer);
                    sink.error(new RequestException("Attachment size exceeds 10 MB"));
                } else {
                    sink.next(buffer);
                }
            });
            return DataBufferUtils.write(bounded, channel)
                    .doOnNext(DataBufferUtils.releaseConsumer())
                    .then();
        }, channel -> close(channel), (channel, error) -> close(channel), channel -> close(channel));
        return Mono.usingWhen(Mono.just(finalPath), ignored -> write
                        .then(sessionService.touch(conversationId, header, null))
                        .then(Mono.defer(() -> saveAttachment(conversationId, filePart, header, bytes.get(), finalPath))),
                ignored -> Mono.empty(),
                (ignored, error) -> deleteFile(finalPath),
                ignored -> deleteFile(finalPath))
                .onErrorMap(error -> error instanceof RequestException ? error
                        : new RequestException("Attachment file save failed", error));
    }

    @Override
    public Flux<AttachmentBO> list(String conversationId, RequestHeader.PrincipalHeader header) {
        if (StringUtils.isBlank(conversationId)) return Flux.empty();
        return attachmentStore.list(conversationId, header);
    }

    @Override
    public Mono<String> summarize(List<Long> attachmentIds, RequestHeader.PrincipalHeader header) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return Mono.just("");
        return attachmentStore.findByIds(attachmentIds, header)
                .map(item -> "- id=" + item.getId() + ", name=" + item.getFileName() + ", contentType="
                        + item.getContentType() + ", size=" + item.getSize() + " bytes")
                .collectList()
                .map(items -> items.isEmpty() ? "" : "Attachment metadata:\n" + String.join("\n", items));
    }

    private Mono<AsynchronousFileChannel> open(Path path) {
        return Mono.fromCallable(() -> AsynchronousFileChannel.open(path, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE));
    }

    private Mono<Void> close(AsynchronousFileChannel channel) {
        return Mono.fromRunnable(() -> {
            try {
                channel.close();
            } catch (IOException exception) {
                log.warn("Attachment channel close failed", exception);
            }
        });
    }

    private Path resolveFilePath(String conversationId, RequestHeader.PrincipalHeader header, String fileName) {
        Path root = storageRoot == null
                ? Paths.get(properties.getAttachmentStoragePath()).toAbsolutePath().normalize()
                : storageRoot;
        String prefix = "tenant_" + safePathPart(String.valueOf(header.getTenantId()))
                + "_user_" + safePathPart(String.valueOf(header.getUserId()))
                + "_conversation_" + safePathPart(conversationId);
        Path path = root.resolve(prefix + SymbolConstant.HYPHEN + UuidV7.next()
                + SymbolConstant.HYPHEN + safePathPart(fileName)).normalize();
        if (!path.startsWith(root)) throw new RequestException("Attachment file path is invalid");
        return path;
    }

    private String safePathPart(String value) {
        return StringUtils.defaultIfBlank(value, "attachment")
                .replaceAll("[^a-zA-Z0-9._-]", SymbolConstant.UNDERSCORE);
    }

    private Mono<AttachmentBO> saveAttachment(String conversationId, FilePart filePart,
                                               RequestHeader.PrincipalHeader header, long size, Path path) {
        AttachmentBO attachment = new AttachmentBO();
        attachment.setConversationId(conversationId);
        attachment.setFileName(filePart.filename());
        MediaType contentType = filePart.headers().getContentType();
        attachment.setContentType(contentType == null ? "application/octet-stream" : contentType.toString());
        attachment.setSize(size);
        attachment.setFilePath(path.toString());
        attachment.setTenantId(header.getTenantId());
        attachment.setUserId(header.getUserId());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        attachment.setCreateTime(now);
        attachment.setOperateTime(now);
        attachment.setCreatorId(header.getUserId());
        attachment.setCreatorName(header.getUserName());
        attachment.setOperatorId(header.getUserId());
        attachment.setOperatorName(header.getUserName());
        return attachmentStore.save(attachment);
    }

    private Mono<Void> deleteFile(Path path) {
        return Mono.fromRunnable(() -> {
            try {
                Files.deleteIfExists(path);
            } catch (Exception exception) {
                log.warn("Attachment cleanup failed after upload termination, file={}", path, exception);
            }
        });
    }
}
