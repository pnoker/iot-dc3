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
import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.agentic.entity.builder.ActionBuilder;
import io.github.pnoker.common.agentic.entity.vo.ActionVO;
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.agentic.utils.AgenticConversationIdUtil;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import jakarta.validation.constraints.NotBlank;
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
 * @version 2025.9.0
 * @since 2016.10.1
 */
@RestController
@RequestMapping(AgenticConstant.ACTION_URL_PREFIX)
@RequiredArgsConstructor
public class ActionController implements BaseController {

    private final ActionBuilder actionBuilder;

    private final ActionService actionService;

    @GetMapping("/pending")
    public Mono<R<List<ActionVO>>> pending(@NotBlank @RequestParam(value = "conversation_id") String conversationId) {
        return getUserHeader().flatMap(header -> async(() -> {
            String scopedConversationId = AgenticConversationIdUtil.scope(header.getTenantId(), header.getUserId(),
                    conversationId);
            List<ActionVO> actions = actionBuilder.buildVOListByBOList(actionService.listPending(scopedConversationId,
                    header));
            actions.forEach(action -> sanitize(header, action));
            return R.ok(actions);
        }));
    }

    @PostMapping("/confirm")
    public Mono<R<ActionVO>> confirm(@NotBlank @RequestParam(value = "action_id") String actionId) {
        return getUserHeader().flatMap(header -> async(() -> {
            ActionBO actionBO = actionService.confirm(actionId, header);
            ActionVO action = actionBuilder.buildVOByBO(actionBO);
            sanitize(header, action);
            return R.ok(action);
        }));
    }

    @PostMapping("/reject")
    public Mono<R<ActionVO>> reject(@NotBlank @RequestParam(value = "action_id") String actionId) {
        return getUserHeader().flatMap(header -> async(() -> {
            ActionBO actionBO = actionService.reject(actionId, header);
            ActionVO action = actionBuilder.buildVOByBO(actionBO);
            sanitize(header, action);
            return R.ok(action);
        }));
    }

    private void sanitize(RequestHeader.UserHeader header, ActionVO action) {
        action.setConversationId(AgenticConversationIdUtil.stripScope(header.getTenantId(), header.getUserId(),
                action.getConversationId()));
    }

}
