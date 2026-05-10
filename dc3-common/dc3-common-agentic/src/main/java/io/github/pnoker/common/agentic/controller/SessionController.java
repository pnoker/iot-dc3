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
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.agentic.util.AgenticConversationIds;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.Objects;

/**
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AgenticConstant.SESSION_URL_PREFIX)
public class SessionController implements BaseController {

    private final SessionService sessionService;

    private final SessionBuilder sessionBuilder;

    public SessionController(SessionService sessionService, SessionBuilder sessionBuilder) {
        this.sessionService = sessionService;
        this.sessionBuilder = sessionBuilder;
    }

    @GetMapping
    public Mono<R<Page<SessionVO>>> list(SessionQuery query) {
        return getUserHeader().flatMap(header -> async(() -> {
            SessionQuery scopedQuery = Objects.isNull(query) ? new SessionQuery() : query;
            scopedQuery.setTenantId(header.getTenantId());
            scopedQuery.setUserId(header.getUserId());
            Page<SessionBO> page = sessionService.selectByPage(scopedQuery);
            Page<SessionVO> voPage = sessionBuilder.buildVOPageByBOPage(page);
            voPage.getRecords().forEach(session -> sanitizeSession(header, session));
            return R.ok(voPage);
        }));
    }

    @GetMapping("/{conversationId}")
    public Mono<R<SessionVO>> get(@PathVariable String conversationId) {
        return getUserHeader().flatMap(header -> async(() -> {
            SessionBO session = sessionService.getByConversationId(AgenticConversationIds.scope(header.getTenantId(),
                    header.getUserId(), conversationId));
            if (Objects.isNull(session)) {
                return R.fail("Session not found");
            }
            SessionVO vo = sessionBuilder.buildVOByBO(session);
            sanitizeSession(header, vo);
            return R.ok(vo);
        }));
    }

    @DeleteMapping("/{conversationId}")
    public Mono<R<Boolean>> delete(@PathVariable String conversationId) {
        return getUserHeader().flatMap(header -> async(() -> {
            sessionService.removeByConversationId(AgenticConversationIds.scope(header.getTenantId(), header.getUserId(),
                    conversationId));
            return R.ok();
        }));
    }

    private void sanitizeSession(RequestHeader.UserHeader header, SessionVO session) {
        session.setConversationId(AgenticConversationIds.stripScope(header.getTenantId(), header.getUserId(),
                session.getConversationId()));
        session.setTenantId(null);
        session.setUserId(null);
    }

}
