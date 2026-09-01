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

import io.github.pnoker.common.agentic.entity.builder.ActionBuilder;
import io.github.pnoker.common.agentic.entity.vo.ActionVO;
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST controller exposing agentic action management endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(name = "action", description = "Agent action definitions: manage AI agent tool definitions including parameter schemas, execution handlers, and result formats for agentic workflow orchestration")
@RestController
@RequestMapping(AgenticConstant.ACTION_URL_PREFIX)
@RequiredArgsConstructor
public class ActionController implements BaseController {

    private final ActionBuilder actionBuilder;

    private final ActionService actionService;

    /**
     * List agent tool calls awaiting human approval in the given conversation.
     *
     * @param conversationId client-visible id of the conversation whose pending tool calls are listed; scoped to the current tenant and user
     * @return an offset page of pending ActionVO entries with tool name and parameters
     */
    @PreAuthorize("@perm.can('action', 'get')")
    @Operation(summary = "List Pending Agent Actions", description = "List agent tool calls awaiting human approval in the given conversation, scoped to the current tenant and user. " +
            "Returns each pending action with its tool name and parameters so the user can confirm or reject before execution.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/pending")
    public Mono<OffsetPage<ActionVO>> pending(@Parameter(description = "Unique identifier of the agentic conversation whose pending tool calls are to be listed; scoped to the current tenant and user.", example = "conv-20240618-abc123") @NotBlank @RequestParam(value = "conversation_id") String conversationId,
                                              @Parameter(description = "Zero-based offset into the pending action collection.", example = "0")
                                              @RequestParam(value = "offset", defaultValue = "0") long offset,
                                              @Parameter(description = "Maximum number of pending actions to return (1-200).", example = "50")
                                              @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return getPrincipalHeader().flatMap(header -> {
            PageRequest pageRequest = new PageRequest(offset, limit);
            return actionService.listPending(pageRequest.offset(), pageRequest.limit(), conversationId, header)
                    .map(page -> {
                        List<ActionVO> items = actionBuilder.buildVOListByBOList(page.items());
                        return OffsetPage.of(items, page.offset(), page.limit(), page.total());
                    });
        });
    }

    /**
     * Approve a pending agent tool call so the assistant may execute it.
     *
     * @param actionId id of the pending agent tool call to approve; must belong to the current tenant and be in pending state
     * @return the ActionVO with its updated execution status
     */
    @PreAuthorize("@perm.can('action', 'list')")
    @Operation(summary = "Confirm Agent Action", description = "Approve a pending agent tool call by id so the assistant may execute it. " +
            "Returns the action with its updated execution status; call after the user accepts a proposed tool invocation.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/confirm")
    public Mono<ActionVO> confirm(@Parameter(description = "Unique identifier of the pending agent tool call to approve; the action must belong to the current tenant and be in pending state.", example = "action-20240618-xyz789") @NotBlank @RequestParam(value = "action_id") String actionId) {
        return getPrincipalHeader().flatMap(header -> actionService.confirm(actionId, header)
                .map(actionBuilder::buildVOByBO));
    }

    /**
     * Decline a pending agent tool call so the assistant does not execute it.
     *
     * @param actionId id of the pending agent tool call to decline; must belong to the current tenant and be in pending state
     * @return the ActionVO with its updated rejected status
     */
    @PreAuthorize("@perm.can('action', 'list')")
    @Operation(summary = "Reject Agent Action", description = "Decline a pending agent tool call by id so the assistant does not execute it. " +
            "Returns the action with its updated rejected status; call when the user denies a proposed tool invocation.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/reject")
    public Mono<ActionVO> reject(@Parameter(description = "Unique identifier of the pending agent tool call to decline; the action must belong to the current tenant and be in pending state.", example = "action-20240618-xyz789") @NotBlank @RequestParam(value = "action_id") String actionId) {
        return getPrincipalHeader().flatMap(header -> actionService.reject(actionId, header)
                .map(actionBuilder::buildVOByBO));
    }

}
