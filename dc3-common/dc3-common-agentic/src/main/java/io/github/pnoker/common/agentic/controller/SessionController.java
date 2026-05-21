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

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.builder.SessionBuilder;
import io.github.pnoker.common.agentic.entity.query.SessionQuery;
import io.github.pnoker.common.agentic.entity.request.SessionUpdateRequest;
import io.github.pnoker.common.agentic.entity.vo.SessionVO;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.agentic.utils.AgenticConversationIdUtil;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Slf4j
@RestController
@RequestMapping(AgenticConstant.SESSION_URL_PREFIX)
@RequiredArgsConstructor
public class SessionController implements BaseController {

    private final SessionService sessionService;

    private final SessionBuilder sessionBuilder;

    @PostMapping("/list")
    public Mono<R<Page<SessionVO>>> list(@RequestBody(required = false) SessionQuery query) {
        return getUserHeader().flatMap(header -> async(() -> {
            SessionQuery scopedQuery = Objects.isNull(query) ? new SessionQuery() : query;
            scopedQuery.setTenantId(header.getTenantId());
            scopedQuery.setUserId(header.getUserId());
            Page<SessionBO> page = sessionService.listByPage(scopedQuery);
            Page<SessionVO> voPage = sessionBuilder.buildVOPageByBOPage(page);
            voPage.getRecords().forEach(session -> sanitizeSession(header, session));
            return R.ok(voPage);
        }));
    }

    @GetMapping("/get_by_conversation_id")
    public Mono<R<SessionVO>> get(@NotBlank @RequestParam(value = "conversation_id") String conversationId) {
        return getUserHeader().flatMap(header -> async(() -> {
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

    @DeleteMapping("/delete")
    public Mono<R<Boolean>> delete(@NotBlank @RequestParam(value = "conversation_id") String conversationId) {
        return getUserHeader().flatMap(header -> async(() -> {
            sessionService.removeByConversationId(AgenticConversationIdUtil.scope(header.getTenantId(), header.getUserId(),
                    conversationId));
            return R.ok();
        }));
    }

    @PostMapping("/update")
    public Mono<R<SessionVO>> update(@NotBlank @RequestParam(value = "conversation_id") String conversationId,
                                     @RequestBody(required = false) SessionUpdateRequest request) {
        return getUserHeader().flatMap(header -> async(() -> {
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

    private void sanitizeSession(RequestHeader.UserHeader header, SessionVO session) {
        session.setConversationId(AgenticConversationIdUtil.stripScope(header.getTenantId(), header.getUserId(),
                session.getConversationId()));
        session.setTenantId(null);
        session.setUserId(null);
    }

}
