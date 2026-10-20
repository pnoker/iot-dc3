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

import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.agentic.repository.ReactiveAttachmentStore;
import io.github.pnoker.common.agentic.service.AttachmentService;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.BusinessException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.utils.UuidV7;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import io.github.pnoker.common.agentic.config.AgenticProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Reactive attachment upload, listing, delete, and metadata summarization. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final long MAX_BYTES = 10 * 1024 * 1024;

    private final ReactiveAttachmentStore attachmentStore;
    private final SessionService sessionService;
    private final AttachmentFileCleaner fileCleaner;
    private final AgenticProperties properties;

    @Override
    public Mono<AttachmentBO> upload(String conversationId, FilePart filePart, RequestHeader.PrincipalHeader header) {
        if (StringUtils.isBlank(conversationId)
                || filePart == null
                || StringUtils.isBlank(filePart.filename())
                || header == null) {
            return Mono.error(new RequestException("Attachment data is required"));
        }
        if (!isAllowedExtension(filePart.filename())) {
            return Mono.error(new RequestException(
                    "Attachment type is not allowed; permitted extensions: " + properties.getAttachmentAllowedExtensions()));
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
        Mono<Void> write = Mono.usingWhen(
                open(finalPath),
                channel -> {
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
                },
                channel -> close(channel),
                (channel, error) -> close(channel),
                channel -> close(channel));
        return Mono.usingWhen(
                        Mono.just(finalPath),
                        ignored -> write.then(sessionService.touch(conversationId, header, null))
                                .then(Mono.defer(() ->
                                        saveAttachment(conversationId, filePart, header, bytes.get(), finalPath))),
                        ignored -> Mono.empty(),
                        (ignored, error) -> fileCleaner.unlink(finalPath),
                        ignored -> fileCleaner.unlink(finalPath))
                .onErrorMap(error -> classifyUploadFailure(conversationId, header, error));
    }

    @Override
    public Flux<AttachmentBO> list(String conversationId, RequestHeader.PrincipalHeader header) {
        if (StringUtils.isBlank(conversationId)) return Flux.empty();
        return attachmentStore.list(conversationId, header);
    }

    @Override
    public Mono<String> summarize(List<Long> attachmentIds, RequestHeader.PrincipalHeader header) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return Mono.just("");
        return attachmentStore
                .getByIds(attachmentIds, header)
                .map(item -> "- id=" + item.getId() + ", name=" + item.getFileName() + ", contentType="
                        + item.getContentType() + ", size=" + item.getSize() + " bytes")
                .collectList()
                .map(items -> items.isEmpty() ? "" : "Attachment metadata:\n" + String.join("\n", items));
    }

    @Override
    public Mono<Long> delete(Long id, RequestHeader.PrincipalHeader header) {
        if (id == null) return Mono.error(new RequestException("Attachment id is required"));
        if (header == null) return Mono.error(new IllegalStateException("Principal header is required"));
        return attachmentStore
                .getById(id, header)
                .switchIfEmpty(Mono.error(new NotFoundException("Attachment does not exist")))
                .flatMap(row -> attachmentStore
                        .delete(id, header)
                        .doOnSuccess(deleted -> log.info(
                                "Attachment deleted, id={}, tenantId={}, userId={}",
                                id,
                                header.getTenantId(),
                                header.getUserId()))
                        .flatMap(deleted -> fileCleaner.unlink(row.getFilePath()).thenReturn(deleted)));
    }

    /**
     * Log and classify one upload failure: client-contract violations ({@link BusinessException},
     * for example a streamed size overflow) pass through untouched, while infrastructure faults
     * (disk full, R2DBC outage) become a {@link ServiceException} so they surface as server
     * errors instead of masquerading as client validation failures.
     */
    private Throwable classifyUploadFailure(
            String conversationId, RequestHeader.PrincipalHeader header, Throwable error) {
        log.warn(
                "Attachment upload failed, conversationId={}, tenantId={}, userId={}, error={}: {}",
                conversationId,
                header.getTenantId(),
                header.getUserId(),
                error.getClass().getSimpleName(),
                error.getMessage());
        if (error instanceof BusinessException businessError) {
            return businessError;
        }
        log.warn(
                "Attachment upload failed, conversationId={}, tenantId={}, userId={}, error={}: {}",
                conversationId,
                header.getTenantId(),
                header.getUserId(),
                error.getClass().getSimpleName(),
                error.getMessage(),
                error);
        return new ServiceException("Attachment file save failed", error);
    }

    private Mono<AsynchronousFileChannel> open(Path path) {
        return Mono.fromCallable(
                        () -> AsynchronousFileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))
                // Filesystem syscalls must never run on the event loop.
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> close(AsynchronousFileChannel channel) {
        return Mono.<Void>fromRunnable(() -> {
                    try {
                        channel.close();
                    } catch (IOException exception) {
                        log.warn("Attachment channel close failed", exception);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Extension whitelist check: anything outside the configured list is rejected
     * before a single byte is written to disk.
     */
    private boolean isAllowedExtension(String fileName) {
        String extension = StringUtils.lowerCase(StringUtils.substringAfterLast(fileName, "."));
        if (StringUtils.isBlank(extension) || extension.equals(StringUtils.lowerCase(fileName))) {
            return false;
        }
        for (String allowed : StringUtils.split(properties.getAttachmentAllowedExtensions(), ',')) {
            if (extension.equals(StringUtils.lowerCase(StringUtils.trim(allowed)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalize a client file name before it is persisted and replayed into prompts:
     * strip control characters (they cannot be rendered and could smuggle delimiters)
     * and cap the length at the configured budget while preserving the extension (it
     * carries the whitelist classification).
     */
    private String normalizeFileName(String fileName) {
        String cleaned = StringUtils.defaultIfBlank(fileName, "attachment").replaceAll("\\p{Cntrl}", "");
        int maxLength = properties.getAttachmentNameMaxLength();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        String extension = StringUtils.substringAfterLast(cleaned, ".");
        if (StringUtils.isBlank(extension) || extension.length() >= maxLength) {
            return cleaned.substring(0, maxLength);
        }
        int stemBudget = maxLength - extension.length() - 1;
        return cleaned.substring(0, stemBudget) + "." + extension;
    }

    private Path resolveFilePath(String conversationId, RequestHeader.PrincipalHeader header, String fileName) {
        Path root = fileCleaner.storageRoot();
        String prefix = "tenant_" + safePathPart(String.valueOf(header.getTenantId()))
                + "_user_" + safePathPart(String.valueOf(header.getUserId()))
                + "_conversation_" + safePathPart(conversationId);
        // The sanitized name must also fit a filesystem path segment (255 bytes on
        // common filesystems); the UUID prefix already spends ~40 of them.
        String namePart = safePathPart(fileName);
        int nameBudget = 128;
        if (namePart.length() > nameBudget) {
            namePart = namePart.substring(0, nameBudget);
        }
        Path path = root
                .resolve(prefix + SymbolConstant.HYPHEN + UuidV7.next() + SymbolConstant.HYPHEN + namePart)
                .normalize();
        if (!path.startsWith(root)) throw new IllegalStateException("Attachment file path is invalid");
        return path;
    }

    private String safePathPart(String value) {
        return StringUtils.defaultIfBlank(value, "attachment").replaceAll("[^a-zA-Z0-9._-]", SymbolConstant.UNDERSCORE);
    }

    private Mono<AttachmentBO> saveAttachment(
            String conversationId, FilePart filePart, RequestHeader.PrincipalHeader header, long size, Path path) {
        AttachmentBO attachment = new AttachmentBO();
        attachment.setConversationId(conversationId);
        attachment.setFileName(normalizeFileName(filePart.filename()));
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
}
