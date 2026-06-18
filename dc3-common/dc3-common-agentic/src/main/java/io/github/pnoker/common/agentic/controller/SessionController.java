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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.builder.SessionBuilder;
import io.github.pnoker.common.agentic.entity.query.SessionQuery;
import io.github.pnoker.common.agentic.entity.vo.SessionVO;
import io.github.pnoker.common.agentic.entity.vo.SessionVO;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.agentic.utils.AgenticConversationIdUtil;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller exposing session management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "session", description = "Agent conversation sessions: create, manage, and terminate AI agent conversation contexts including message history and tool invocation state")
@Slf4j
@RestController
@RequestMapping(AgenticConstant.SESSION_URL_PREFIX)
@RequiredArgsConstructor
public class SessionController implements BaseController {

    private final SessionService sessionService;

    private final SessionBuilder sessionBuilder;

    @PreAuthorize("@perm.can('session', 'list')")
    @Operation(summary = "List Sessions", description = "Page through the current user's AI chat sessions for this tenant. " +
            "Returns a page of session summaries; use to resume or browse past conversations.")
    @PostMapping("/list")
    public Mono<R<Page<SessionVO>>> list(@RequestBody(required = false) SessionQuery query) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            SessionQuery scopedQuery = Objects.isNull(query) ? new SessionQuery() : query;
            scopedQuery.setTenantId(header.getTenantId());
            scopedQuery.setUserId(header.getUserId());
            Page<SessionBO> page = sessionService.listByPage(scopedQuery);
            Page<SessionVO> voPage = sessionBuilder.buildVOPageByBOPage(page);
            voPage.getRecords().forEach(session -> sanitizeSession(header, session));
            return R.ok(voPage);
        }));
    }

    @PreAuthorize("@perm.can('session', 'get')")
    @Operation(summary = "Get Session", description = "Fetch a single AI chat session by its conversation id, scoped to the current user and tenant. " +
            "Returns the session details, or a failure when no matching session exists.")
    @GetMapping("/get_by_conversation_id")
    public Mono<R<SessionVO>> get(@Parameter(description = "Conversation ID") @NotBlank @RequestParam(value = "conversation_id") String conversationId) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            SessionBO session = sessionService.getByConversationId(AgenticConversationIdUtil.scope(header.getTenantId(),
                    header.getUserId(), conversationId));
            if (Objects.isNull(session)) {
                return R.fail("Session not found");
            }
            SessionVO vo = sessionBuilder.buildVOByBO(session);
            sanitizeSession(header, vo);
            return R.ok(vo);
        }));
    }

    @PreAuthorize("@perm.can('session', 'delete')")
    @Operation(summary = "Delete Session", description = "Permanently delete an AI chat session and its message history by conversation id, scoped to the current user and tenant. " +
            "Use to discard a conversation; this cannot be undone.")
    @PostMapping("/delete")
    public Mono<R<Boolean>> delete(@Parameter(description = "Conversation ID") @NotBlank @RequestParam(value = "conversation_id") String conversationId) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            sessionService.deleteByConversationId(AgenticConversationIdUtil.scope(header.getTenantId(), header.getUserId(),
                    conversationId));
            return R.ok();
        }));
    }

    @PreAuthorize("@perm.can('session', 'update')")
    @Operation(summary = "Update Session", description = "Update editable fields of an AI chat session identified by conversation id, scoped to the current user and tenant. " +
            "Use to rename or adjust a session; returns the updated session, or a failure when it does not exist.")
    @PostMapping("/update")
    public Mono<R<SessionVO>> update(@Parameter(description = "Conversation ID") @NotBlank @RequestParam(value = "conversation_id") String conversationId,
                                     @RequestBody(required = false) SessionVO request) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            SessionBO session = sessionService.update(AgenticConversationIdUtil.scope(header.getTenantId(),
                    header.getUserId(), conversationId), request);
            if (Objects.isNull(session)) {
                return R.fail("Session not found");
            }
            SessionVO vo = sessionBuilder.buildVOByBO(session);
            sanitizeSession(header, vo);
            return R.ok(vo);
        }));
    }

    private void sanitizeSession(RequestHeader.PrincipalHeader header, SessionVO session) {
        session.setConversationId(AgenticConversationIdUtil.stripScope(header.getTenantId(), header.getUserId(),
                session.getConversationId()));
        session.setTenantId(null);
        session.setUserId(null);
    }

}
