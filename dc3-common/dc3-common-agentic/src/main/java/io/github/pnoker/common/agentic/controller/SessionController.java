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

import io.github.pnoker.common.agentic.entity.builder.SessionBuilder;
import io.github.pnoker.common.agentic.entity.query.SessionQuery;
import io.github.pnoker.common.agentic.entity.vo.SessionVO;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** REST controller exposing tenant-scoped reactive session resources. */
@Tag(name = "session", description = "Agent conversation sessions")
@Slf4j
@RestController
@RequestMapping(AgenticConstant.SESSION_URL_PREFIX)
@RequiredArgsConstructor
public class SessionController implements BaseController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final SessionService sessionService;
    private final SessionBuilder sessionBuilder;

    /** Page sessions matching the tenant-scoped filters. */
    @PreAuthorize("@perm.can('session', 'list')")
    @Operation(
            summary = "List Sessions",
            description = "List the current user's sessions using zero-based offset pagination.",
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
    public Mono<OffsetPage<SessionVO>> list(@RequestBody(required = false) SessionQuery query) {
        SessionQuery request = query == null ? new SessionQuery() : query;
        long offset = request.getOffset() == null ? 0L : request.getOffset();
        int limit = request.getLimit() == null ? DEFAULT_LIMIT : request.getLimit();
        if (offset < 0 || limit < 1 || limit > MAX_LIMIT) {
            return Mono.error(new RequestException("offset must be non-negative and limit must be between 1 and 200"));
        }
        return getPrincipalHeader()
                .flatMap(header -> sessionService
                        .list(offset, limit, request.getConversationId(), request.getSort(), header)
                        .map(page -> new OffsetPage<>(
                                page.items().stream()
                                        .map(sessionBuilder::buildVOByBO)
                                        .peek(this::sanitizeSession)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total(),
                                page.hasNext())));
    }

    /** Load the session for the request. */
    @PreAuthorize("@perm.can('session', 'get')")
    @Operation(
            summary = "Get Session",
            description = "Get one session scoped to the authenticated tenant and user.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_by_conversation_id")
    public Mono<SessionVO> get(
            @Parameter(description = "Client-visible conversation identifier")
                    @NotBlank
                    @RequestParam("conversation_id")
                    String conversationId) {
        return getPrincipalHeader()
                .flatMap(header -> sessionService
                        .get(conversationId, header)
                        .switchIfEmpty(Mono.error(new RequestException("Session not found")))
                        .map(sessionBuilder::buildVOByBO)
                        .doOnNext(this::sanitizeSession));
    }

    /** Delete the session. */
    @PreAuthorize("@perm.can('session', 'delete')")
    @Operation(
            summary = "Delete Session",
            description = "Delete a session and all of its messages atomically.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @Parameter(description = "Client-visible conversation identifier")
                    @NotBlank
                    @RequestParam("conversation_id")
                    String conversationId) {
        return getPrincipalHeader()
                .flatMap(header -> sessionService.delete(conversationId, header).then());
    }

    /** Update one session and emit the updated row. */
    @PreAuthorize("@perm.can('session', 'update')")
    @Operation(
            summary = "Update Session",
            description = "Update editable session metadata and return the resource.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/update")
    public Mono<SessionVO> update(
            @Parameter(description = "Client-visible conversation identifier")
                    @NotBlank
                    @RequestParam("conversation_id")
                    String conversationId,
            @RequestBody(required = false) SessionVO request) {
        return getPrincipalHeader().flatMap(header -> {
            SessionVO payload = Objects.requireNonNullElseGet(request, SessionVO::new);
            return sessionService
                    .update(conversationId, payload.getSessionExt(), payload.getTitle(), header)
                    .switchIfEmpty(Mono.error(new RequestException("Session not found")))
                    .map(sessionBuilder::buildVOByBO)
                    .doOnNext(this::sanitizeSession);
        });
    }

    private void sanitizeSession(SessionVO session) {
        session.setTenantId(null);
        session.setUserId(null);
    }
}
