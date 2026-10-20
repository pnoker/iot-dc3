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

import io.github.pnoker.common.agentic.entity.builder.MessageBuilder;
import io.github.pnoker.common.agentic.entity.dto.ConversationPageQueryDTO;
import io.github.pnoker.common.agentic.entity.vo.MessageVO;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing chat message history endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(
        name = "message",
        description =
                "Agent conversation messages: manage messages exchanged between AI agents and language models within conversation sessions including user prompts and assistant responses")
@RestController("agenticMessageController")
@RequestMapping(AgenticConstant.MESSAGE_URL_PREFIX)
@RequiredArgsConstructor
public class MessageController implements BaseController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final MessageBuilder messageBuilder;

    private final MessageService messageService;

    /**
     * Page the ordered message history of one AI chat session.
     *
     * @param query page query carrying the conversation id and optional offset/limit (defaults 0/50, limit capped at 200)
     * @return an offset page of MessageVO turns (user prompts and assistant replies) in chronological order
     */
    @PreAuthorize("@perm.can('message', 'list')")
    @Operation(
            summary = "List Session Messages",
            description =
                    "Page the ordered message history of one AI chat session for the current user and tenant. "
                            + "Returns each turn (user prompts and assistant replies) in chronological order; use to read or resume a past conversation.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list")
    public Mono<OffsetPage<MessageVO>> list(@RequestBody(required = false) ConversationPageQueryDTO query) {
        ConversationPageQueryDTO request = query == null ? new ConversationPageQueryDTO() : query;
        if (request.getConversationId() == null || request.getConversationId().isBlank()) {
            return Mono.error(new RequestException("conversation_id is required"));
        }
        long offset = request.getOffset() == null ? 0L : request.getOffset();
        int limit = request.getLimit() == null ? DEFAULT_LIMIT : request.getLimit();
        if (offset < 0 || limit < 1 || limit > MAX_LIMIT) {
            return Mono.error(new RequestException("offset must be non-negative and limit must be between 1 and 200"));
        }
        return getPrincipalHeader()
                .flatMap(header -> messageService
                        .list(offset, limit, request.getConversationId(), header)
                        .map(page -> new OffsetPage<>(
                                page.items().stream()
                                        .map(messageBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total(),
                                page.hasNext())));
    }
}
