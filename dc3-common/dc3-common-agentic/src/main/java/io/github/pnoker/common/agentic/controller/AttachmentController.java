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
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing attachment upload, listing, and delete endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(
        name = "attachment",
        description =
                "Agent conversation attachments: manage files, images, and structured data objects associated with AI agent conversation messages")
@RestController
@RequestMapping(AgenticConstant.ATTACHMENT_URL_PREFIX)
@RequiredArgsConstructor
public class AttachmentController implements BaseController {

    private final AttachmentBuilder attachmentBuilder;

    private final AttachmentService attachmentService;

    /**
     * Upload a file as an attachment to the given AI conversation.
     *
     * @param conversationId client-visible id of the conversation to attach the file to; scoped to the current tenant and user
     * @param filePart       multipart file part carrying the file bytes to upload
     * @return the stored AttachmentVO metadata; the file then becomes available as context the assistant can reference
     */
    @PreAuthorize("@perm.can('attachment', 'list')")
    @Operation(
            summary = "Upload Attachment",
            description =
                    "Upload a file as an attachment to the given AI conversation for the current tenant and user. "
                            + "Returns the stored attachment metadata; the file then becomes available as context the assistant can reference in that conversation.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/upload")
    public Mono<AttachmentVO> upload(
            @Parameter(
                            description =
                                    "Unique identifier of the AI conversation to attach the file to; must belong to the current tenant and user.",
                            example = "conv-20240101-abcde")
                    @NotBlank
                    @RequestParam(value = "conversation_id")
                    String conversationId,
            @RequestPart("file") Mono<FilePart> filePart) {
        return getPrincipalHeader()
                .flatMap(header -> filePart.flatMap(part ->
                        attachmentService.upload(conversationId, part, header).map(attachmentBuilder::buildVOByBO)));
    }

    /**
     * List attachments uploaded to the given AI conversation.
     *
     * @param conversationId client-visible id of the conversation whose attachments are listed; scoped to the current tenant and user
     * @return a list of AttachmentVO metadata entries for files the assistant can reference in that conversation
     */
    @PreAuthorize("@perm.can('attachment', 'list')")
    @Operation(
            summary = "List Attachments",
            description =
                    "List the attachments uploaded to the given AI conversation, scoped to the current tenant and user. "
                            + "Returns attachment metadata entries; use to discover files the assistant can reference in that conversation.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list")
    public Mono<List<AttachmentVO>> list(
            @Parameter(
                            description =
                                    "Unique identifier of the AI conversation whose attachments should be listed; must belong to the current tenant and user.",
                            example = "conv-20240101-abcde")
                    @NotBlank
                    @RequestParam(value = "conversation_id")
                    String conversationId) {
        return getPrincipalHeader()
                .flatMap(header -> attachmentService
                        .list(conversationId, header)
                        .map(attachmentBuilder::buildVOByBO)
                        .collectList());
    }

    /**
     * Delete one uploaded attachment by id for the current tenant and user.
     *
     * @param id primary key of the attachment to delete; must belong to the current tenant and user
     * @return empty body; the metadata row is soft-deleted and the stored file removed best-effort
     */
    @PreAuthorize("@perm.can('attachment', 'delete')")
    @Operation(
            summary = "Delete Attachment",
            description =
                    "Delete one uploaded attachment by id within the current tenant and user. "
                            + "Soft-deletes the metadata row and removes the stored file best-effort; the file is no longer referenceable in the conversation.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @Parameter(
                            description =
                                    "Primary key of the attachment to delete. Must belong to the current tenant and user.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id) {
        return getPrincipalHeader().flatMap(header -> attachmentService.delete(id, header).then());
    }
}
