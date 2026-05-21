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
package io.github.pnoker.common.agentic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.agentic.dal.SessionManager;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.builder.SessionBuilder;
import io.github.pnoker.common.agentic.entity.model.SessionDO;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.agentic.entity.query.SessionQuery;
import io.github.pnoker.common.agentic.entity.request.SessionUpdateRequest;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Implements session touch, query, update, and logical delete with chat memory cleanup.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionBuilder sessionBuilder;

    private final SessionManager sessionManager;

    private final ChatMemory agenticChatMemory;

    private final MessageService messageService;

    @Override
    public SessionBO touch(String conversationId, Long tenantId, Long userId) {
        return touch(conversationId, tenantId, userId, null);
    }

    @Override
    public SessionBO touch(String conversationId, Long tenantId, Long userId, SessionExt sessionExt) {
        SessionDO existing = findByConversationId(conversationId);
        if (Objects.nonNull(existing)) {
            existing.setTenantId(tenantId);
            existing.setUserId(userId);
            applySessionExt(existing, sessionExt);
            sessionManager.updateById(existing);
            return sessionBuilder.buildBOByDO(existing);
        }

        SessionDO entityDO = new SessionDO();
        entityDO.setConversationId(conversationId);
        entityDO.setTenantId(tenantId);
        entityDO.setUserId(userId);
        entityDO.setTitle(AgenticConstant.Session.DEFAULT_TITLE);
        applySessionExt(entityDO, sessionExt);
        sessionManager.save(entityDO);
        return sessionBuilder.buildBOByDO(entityDO);
    }

    @Override
    public SessionBO getByConversationId(String conversationId) {
        SessionDO entityDO = findByConversationId(conversationId);
        return Objects.nonNull(entityDO) ? sessionBuilder.buildBOByDO(entityDO) : null;
    }

    @Override
    public void removeByConversationId(String conversationId) {
        SessionDO entityDO = findByConversationId(conversationId);
        if (Objects.isNull(entityDO)) {
            return;
        }
        sessionManager.removeById(entityDO.getId());
        int removedMessages = messageService.removeByConversationId(conversationId);
        agenticChatMemory.clear(conversationId);
        log.info("Agentic session removed, conversationId={}, messagesRemoved={}", conversationId, removedMessages);
    }

    @Override
    public SessionBO update(String conversationId, SessionUpdateRequest request) {
        SessionDO entityDO = findByConversationId(conversationId);
        if (Objects.isNull(entityDO) || Objects.isNull(request)) {
            return null;
        }
        if (StringUtils.isNotBlank(request.getTitle())) {
            entityDO.setTitle(request.getTitle().trim());
        }
        applySessionExt(entityDO, request.getSessionExt());
        sessionManager.updateById(entityDO);
        return sessionBuilder.buildBOByDO(entityDO);
    }

    private void applySessionExt(SessionDO entityDO, SessionExt requestExt) {
        if (Objects.isNull(requestExt)) {
            return;
        }
        validateSessionExt(requestExt);
        SessionExt target = Objects.nonNull(entityDO.getSessionExt()) ? entityDO.getSessionExt()
                : new SessionExt();
        if (StringUtils.isNotBlank(requestExt.getModel())) {
            target.setModel(requestExt.getModel().trim());
        }
        if (Objects.nonNull(requestExt.getReasoningEnabled())) {
            target.setReasoningEnabled(requestExt.getReasoningEnabled());
        }
        if (Objects.nonNull(requestExt.getTemperature())) {
            target.setTemperature(requestExt.getTemperature());
        }
        if (Objects.nonNull(requestExt.getMaxTokens())) {
            target.setMaxTokens(requestExt.getMaxTokens());
        }
        entityDO.setSessionExt(target);
    }

    private void validateSessionExt(SessionExt sessionExt) {
        if (Objects.nonNull(sessionExt.getTemperature())
                && (sessionExt.getTemperature() < 0.0 || sessionExt.getTemperature() > 2.0)) {
            throw new RequestException("Temperature must be between 0.0 and 2.0");
        }
        if (Objects.nonNull(sessionExt.getMaxTokens()) && sessionExt.getMaxTokens() < 1) {
            throw new RequestException("Max tokens must be greater than 0");
        }
    }

    @Override
    public Page<SessionBO> listByPage(SessionQuery query) {
        if (Objects.isNull(query)) {
            query = new SessionQuery();
        }
        if (Objects.isNull(query.getPage())) {
            query.setPage(new io.github.pnoker.common.entity.common.Pages());
        }
        Page<SessionDO> entityPageDO = sessionManager.page(PageUtil.page(query.getPage()), fuzzyQuery(query));
        return sessionBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private SessionDO findByConversationId(String conversationId) {
        LambdaQueryWrapper<SessionDO> wrapper = Wrappers.<SessionDO>query()
                .lambda()
                .eq(SessionDO::getConversationId, conversationId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        return sessionManager.getOne(wrapper);
    }

    private LambdaQueryWrapper<SessionDO> fuzzyQuery(SessionQuery query) {
        LambdaQueryWrapper<SessionDO> wrapper = Wrappers.<SessionDO>query().lambda();
        wrapper.eq(Objects.nonNull(query.getTenantId()), SessionDO::getTenantId, query.getTenantId());
        wrapper.eq(FieldUtil.isValidIdField(query.getUserId()), SessionDO::getUserId, query.getUserId());
        wrapper.like(StringUtils.isNotEmpty(query.getConversationId()), SessionDO::getConversationId,
                query.getConversationId());
        wrapper.orderByDesc(SessionDO::getOperateTime);
        return wrapper;
    }

}
