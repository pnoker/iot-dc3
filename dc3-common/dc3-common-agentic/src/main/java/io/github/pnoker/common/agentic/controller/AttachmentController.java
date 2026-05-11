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

import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.agentic.entity.builder.AttachmentBuilder;
import io.github.pnoker.common.agentic.entity.request.AttachmentUploadRequest;
import io.github.pnoker.common.agentic.entity.vo.AttachmentVO;
import io.github.pnoker.common.agentic.service.AttachmentService;
import io.github.pnoker.common.agentic.util.AgenticConversationIds;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping(AgenticConstant.ATTACHMENT_URL_PREFIX)
public class AttachmentController implements BaseController {

    private final AttachmentBuilder attachmentBuilder;

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentBuilder attachmentBuilder, AttachmentService attachmentService) {
        this.attachmentBuilder = attachmentBuilder;
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload")
    public Mono<R<AttachmentVO>> upload(@Validated @RequestBody AttachmentUploadRequest request) {
        return getUserHeader().flatMap(header -> async(() -> {
            request.setConversationId(AgenticConversationIds.scope(header.getTenantId(), header.getUserId(),
                    request.getConversationId()));
            AttachmentBO attachmentBO = attachmentService.upload(request, header);
            AttachmentVO attachment = attachmentBuilder.buildVOByBO(attachmentBO);
            sanitize(header, attachment);
            return R.ok(attachment);
        }));
    }

    @GetMapping("/{conversationId}")
    public Mono<R<List<AttachmentVO>>> list(@NotBlank @PathVariable(value = "conversationId") String conversationId) {
        return getUserHeader().flatMap(header -> async(() -> {
            String scopedConversationId = AgenticConversationIds.scope(header.getTenantId(), header.getUserId(),
                    conversationId);
            List<AttachmentVO> attachments = attachmentBuilder.buildVOListByBOList(attachmentService.list(
                    scopedConversationId, header));
            attachments.forEach(attachment -> sanitize(header, attachment));
            return R.ok(attachments);
        }));
    }

    private void sanitize(RequestHeader.UserHeader header, AttachmentVO attachment) {
        attachment.setConversationId(AgenticConversationIds.stripScope(header.getTenantId(), header.getUserId(),
                attachment.getConversationId()));
    }

}
