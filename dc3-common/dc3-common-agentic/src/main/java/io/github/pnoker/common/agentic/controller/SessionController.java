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
import io.github.pnoker.common.agentic.constant.AgenticConstant;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.builder.SessionBuilder;
import io.github.pnoker.common.agentic.entity.query.SessionQuery;
import io.github.pnoker.common.agentic.entity.vo.SessionVO;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
        try {
            Page<SessionBO> page = sessionService.selectByPage(query);
            return Mono.just(R.ok(sessionBuilder.buildVOPageByBOPage(page)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/{conversationId}")
    public Mono<R<SessionVO>> get(@PathVariable String conversationId) {
        try {
            SessionBO session = sessionService.getByConversationId(conversationId);
            if (session == null) {
                return Mono.just(R.fail("Session not found"));
            }
            return Mono.just(R.ok(sessionBuilder.buildVOByBO(session)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{conversationId}")
    public Mono<R<Boolean>> delete(@PathVariable String conversationId) {
        try {
            sessionService.removeByConversationId(conversationId);
            return Mono.just(R.ok());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
