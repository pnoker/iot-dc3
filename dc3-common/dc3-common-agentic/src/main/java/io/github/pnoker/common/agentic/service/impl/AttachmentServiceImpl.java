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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.dal.AttachmentManager;
import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.agentic.entity.builder.AttachmentBuilder;
import io.github.pnoker.common.agentic.entity.model.AttachmentDO;
import io.github.pnoker.common.agentic.service.AttachmentService;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.RequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implements attachment upload, listing, and AI-based summarization.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final long MAX_BYTES = 10 * 1024 * 1024;

    private final AttachmentManager attachmentManager;
    private final AttachmentBuilder attachmentBuilder;

    private final AgenticProperties properties;

    @Override
    public Mono<AttachmentBO> upload(String conversationId, FilePart filePart, RequestHeader.UserHeader header) {
        if (StringUtils.isBlank(conversationId) || Objects.isNull(filePart)
                || StringUtils.isBlank(filePart.filename())) {
            throw new RequestException("Attachment data is required");
        }
        assertContentLength(filePart);

        Path filePath = resolveFilePath(conversationId, header, filePart.filename());
        return filePart.transferTo(filePath.toFile())
                .then(Mono.fromCallable(() -> saveAttachment(conversationId, filePart, filePath, header))
                        .subscribeOn(Schedulers.boundedElastic()))
                .doOnError(error -> deleteFile(filePath))
                .onErrorMap(Exception.class, error -> error instanceof RequestException ? error
                        : new RequestException("Attachment file save failed", error));
    }

    private AttachmentBO saveAttachment(String conversationId, FilePart filePart, Path filePath,
                                        RequestHeader.UserHeader header) throws Exception {
        long size = Files.size(filePath);
        if (size > MAX_BYTES) {
            Files.deleteIfExists(filePath);
            throw new RequestException("Attachment size exceeds 10 MB");
        }

        AttachmentBO entityBO = new AttachmentBO();
        entityBO.setConversationId(conversationId);
        entityBO.setFileName(filePart.filename());
        MediaType contentType = filePart.headers().getContentType();
        entityBO.setContentType(Objects.nonNull(contentType) ? contentType.toString() : "application/octet-stream");
        entityBO.setSize(size);
        entityBO.setFilePath(filePath.toString());
        entityBO.setTenantId(header.getTenantId());
        entityBO.setUserId(header.getUserId());
        fillCreateAudit(entityBO, header);
        AttachmentDO entityDO = attachmentBuilder.buildDOByBO(entityBO);
        attachmentManager.save(entityDO);
        return attachmentBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<AttachmentBO> list(String conversationId, RequestHeader.UserHeader header) {
        LambdaQueryWrapper<AttachmentDO> wrapper = Wrappers.<AttachmentDO>query()
                .lambda()
                .eq(AttachmentDO::getConversationId, conversationId)
                .eq(AttachmentDO::getTenantId, header.getTenantId())
                .eq(AttachmentDO::getUserId, header.getUserId())
                .orderByDesc(AttachmentDO::getCreateTime);
        return attachmentBuilder.buildBOListByDOList(attachmentManager.list(wrapper));
    }

    @Override
    public String summarize(List<Long> attachmentIds, RequestHeader.UserHeader header) {
        if (Objects.isNull(attachmentIds) || attachmentIds.isEmpty()) {
            return "";
        }
        LambdaQueryWrapper<AttachmentDO> wrapper = Wrappers.<AttachmentDO>query()
                .lambda()
                .in(AttachmentDO::getId, attachmentIds)
                .eq(AttachmentDO::getTenantId, header.getTenantId())
                .eq(AttachmentDO::getUserId, header.getUserId());
        List<AttachmentDO> attachments = attachmentManager.list(wrapper);
        if (attachments.isEmpty()) {
            return "";
        }
        String summary = attachmentBuilder.buildBOListByDOList(attachments).stream()
                .map(item -> "- id=" + item.getId() + ", name=" + item.getFileName() + ", contentType="
                        + item.getContentType() + ", size=" + item.getSize() + " bytes")
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return "Attachment metadata:\n" + summary;
    }

    private Path resolveFilePath(String conversationId, RequestHeader.UserHeader header, String fileName) {
        Path storageRoot = Paths.get(properties.getAttachmentStoragePath()).toAbsolutePath().normalize();
        Path directory = storageRoot
                .resolve("tenant_" + safePathPart(String.valueOf(header.getTenantId())))
                .resolve("user_" + safePathPart(String.valueOf(header.getUserId())))
                .resolve(safePathPart(conversationId))
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(directory);
        } catch (Exception e) {
            throw new RequestException("Attachment directory create failed: {}", directory, e);
        }

        Path filePath = directory.resolve(UUID.randomUUID() + SymbolConstant.HYPHEN + safePathPart(fileName)).normalize();
        if (!filePath.startsWith(storageRoot)) {
            throw new RequestException("Attachment file path is invalid");
        }
        return filePath;
    }

    private void assertContentLength(FilePart filePart) {
        long contentLength = filePart.headers().getContentLength();
        if (contentLength > MAX_BYTES) {
            throw new RequestException("Attachment size exceeds 10 MB");
        }
    }

    private void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("Failed to clean up attachment file after upload error: {}", filePath, e);
        }
    }

    private String safePathPart(String value) {
        String sanitized = StringUtils.defaultIfBlank(value, "attachment")
                .replaceAll("[^a-zA-Z0-9._-]", SymbolConstant.UNDERSCORE);
        return StringUtils.defaultIfBlank(sanitized, "attachment");
    }

    private void fillCreateAudit(AttachmentBO entityBO, RequestHeader.UserHeader header) {
        LocalDateTime now = LocalDateTime.now();
        entityBO.setCreateTime(now);
        entityBO.setOperateTime(now);
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getUserName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getUserName());
    }

}
