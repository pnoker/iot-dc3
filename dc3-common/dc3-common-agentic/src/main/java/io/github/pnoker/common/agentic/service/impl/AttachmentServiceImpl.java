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
import io.github.pnoker.common.agentic.entity.model.AttachmentDO;
import io.github.pnoker.common.agentic.entity.request.AttachmentUploadRequest;
import io.github.pnoker.common.agentic.entity.vo.AttachmentVO;
import io.github.pnoker.common.agentic.service.AttachmentService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.RequestException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private static final long MAX_BYTES = 10 * 1024 * 1024;

    private final AttachmentManager attachmentManager;

    private final AgenticProperties properties;

    public AttachmentServiceImpl(AttachmentManager attachmentManager, AgenticProperties properties) {
        this.attachmentManager = attachmentManager;
        this.properties = properties;
    }

    @Override
    public AttachmentVO upload(AttachmentUploadRequest request, RequestHeader.UserHeader header) {
        if (Objects.isNull(request) || StringUtils.isAnyBlank(request.getConversationId(), request.getFileName(),
                request.getData())) {
            throw new RequestException("Attachment data is required");
        }
        byte[] content = decode(request.getData());
        if (content.length > MAX_BYTES) {
            throw new RequestException("Attachment size exceeds 10 MB");
        }

        Path filePath = writeFile(request.getConversationId(), request.getFileName(), content);

        AttachmentDO entity = new AttachmentDO();
        entity.setConversationId(request.getConversationId());
        entity.setFileName(request.getFileName());
        entity.setContentType(StringUtils.defaultIfBlank(request.getContentType(), "application/octet-stream"));
        entity.setSize(Objects.nonNull(request.getSize()) ? request.getSize() : (long) content.length);
        entity.setFilePath(filePath.toString());
        entity.setTenantId(header.getTenantId());
        entity.setUserId(header.getUserId());
        entity.setCreateTime(LocalDateTime.now());
        entity.setOperateTime(entity.getCreateTime());
        entity.setCreatorId(header.getUserId());
        entity.setOperatorId(header.getUserId());
        entity.setCreatorName(header.getUserName());
        entity.setOperatorName(header.getUserName());
        attachmentManager.save(entity);
        return toVO(entity);
    }

    @Override
    public List<AttachmentVO> list(String conversationId, RequestHeader.UserHeader header) {
        LambdaQueryWrapper<AttachmentDO> wrapper = Wrappers.<AttachmentDO>query()
                .lambda()
                .eq(AttachmentDO::getConversationId, conversationId)
                .eq(AttachmentDO::getTenantId, header.getTenantId())
                .eq(AttachmentDO::getUserId, header.getUserId())
                .orderByDesc(AttachmentDO::getCreateTime);
        return attachmentManager.list(wrapper).stream().map(this::toVO).toList();
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
        String summary = attachments.stream()
                .map(item -> "- " + item.getFileName() + " (" + item.getContentType() + ", " + item.getSize()
                        + " bytes, path=" + item.getFilePath() + ")")
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return "Attachment metadata:\n" + summary;
    }

    private byte[] decode(String data) {
        String normalized = data;
        int comma = normalized.indexOf(',');
        if (comma >= 0) {
            normalized = normalized.substring(comma + 1);
        }
        return Base64.getDecoder().decode(normalized);
    }

    private Path writeFile(String conversationId, String fileName, byte[] content) {
        try {
            Path directory = Paths.get(properties.getAttachmentStoragePath(), safePathPart(conversationId));
            Files.createDirectories(directory);
            Path filePath = directory.resolve(UUID.randomUUID() + "-" + safePathPart(fileName));
            Files.write(filePath, content);
            return filePath.toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new RequestException("Attachment file save failed");
        }
    }

    private String safePathPart(String value) {
        String sanitized = StringUtils.defaultIfBlank(value, "attachment")
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        return StringUtils.defaultIfBlank(sanitized, "attachment");
    }

    private AttachmentVO toVO(AttachmentDO entity) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(entity.getId());
        vo.setConversationId(entity.getConversationId());
        vo.setFileName(entity.getFileName());
        vo.setContentType(entity.getContentType());
        vo.setSize(entity.getSize());
        vo.setFilePath(entity.getFilePath());
        vo.setCreateTime(entity.getCreateTime());
        vo.setOperateTime(entity.getOperateTime());
        return vo;
    }

}
