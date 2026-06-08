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
package io.github.pnoker.common.agentic.controller;

import io.github.pnoker.common.agentic.entity.builder.AttachmentBuilder;
import io.github.pnoker.common.agentic.entity.vo.AttachmentVO;
import io.github.pnoker.common.agentic.service.AttachmentService;
import io.github.pnoker.common.agentic.utils.AgenticConversationIdUtil;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST controller exposing attachment upload and listing endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@RestController
@RequestMapping(AgenticConstant.ATTACHMENT_URL_PREFIX)
@RequiredArgsConstructor
public class AttachmentController implements BaseController {

    private final AttachmentBuilder attachmentBuilder;

    private final AttachmentService attachmentService;

    @PreAuthorize("@perm.can('attachment', 'list')")
    @PostMapping("/upload")
    public Mono<R<AttachmentVO>> upload(@NotBlank @RequestParam(value = "conversation_id") String conversationId,
                                        @RequestPart("file") Mono<FilePart> filePart) {
        return getUserHeader().flatMap(header -> filePart.flatMap(part -> {
            String scopedConversationId = AgenticConversationIdUtil.scope(header.getTenantId(), header.getUserId(),
                    conversationId);
            return attachmentService.upload(scopedConversationId, part, header).map(attachmentBO -> {
                AttachmentVO attachment = attachmentBuilder.buildVOByBO(attachmentBO);
                sanitize(header, attachment);
                return R.ok(attachment);
            });
        }));
    }

    @PreAuthorize("@perm.can('attachment', 'list')")
    @GetMapping("/list")
    public Mono<R<List<AttachmentVO>>> list(@NotBlank @RequestParam(value = "conversation_id") String conversationId) {
        return getUserHeader().flatMap(header -> async(() -> {
            String scopedConversationId = AgenticConversationIdUtil.scope(header.getTenantId(), header.getUserId(),
                    conversationId);
            List<AttachmentVO> attachments = attachmentBuilder.buildVOListByBOList(attachmentService.list(
                    scopedConversationId, header));
            attachments.forEach(attachment -> sanitize(header, attachment));
            return R.ok(attachments);
        }));
    }

    private void sanitize(RequestHeader.UserHeader header, AttachmentVO attachment) {
        attachment.setConversationId(AgenticConversationIdUtil.stripScope(header.getTenantId(), header.getUserId(),
                attachment.getConversationId()));
    }

}
