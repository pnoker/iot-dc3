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
import io.github.pnoker.common.agentic.entity.model.SessionConfig;
import io.github.pnoker.common.agentic.entity.model.SessionDO;
import io.github.pnoker.common.agentic.entity.query.SessionQuery;
import io.github.pnoker.common.agentic.entity.request.SessionUpdateRequest;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class SessionServiceImpl implements SessionService {

    @Resource
    private SessionBuilder sessionBuilder;

    @Resource
    private SessionManager sessionManager;

    @Resource
    private ChatMemory agenticChatMemory;

    @Resource
    private MessageService messageService;

    @Override
    public SessionBO touch(String conversationId, Long tenantId, Long userId, String model) {
        return touch(conversationId, tenantId, userId, model, null);
    }

    @Override
    public SessionBO touch(String conversationId, Long tenantId, Long userId, String model,
                           SessionConfig sessionConfig) {
        SessionDO existing = findByConversationId(conversationId);
        if (Objects.nonNull(existing)) {
            existing.setTenantId(tenantId);
            existing.setUserId(userId);
            applyModel(existing, model);
            applySessionConfig(existing, sessionConfig);
            sessionManager.updateById(existing);
            return sessionBuilder.buildBOByDO(existing);
        }

        SessionDO entityDO = new SessionDO();
        entityDO.setConversationId(conversationId);
        entityDO.setTenantId(tenantId);
        entityDO.setUserId(userId);
        entityDO.setTitle("New Conversation");
        applyModel(entityDO, model);
        applySessionConfig(entityDO, sessionConfig);
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
        applyModel(entityDO, request.getModel());
        applySessionConfig(entityDO, request.getSessionConfig());
        sessionManager.updateById(entityDO);
        return sessionBuilder.buildBOByDO(entityDO);
    }

    private void applyModel(SessionDO entityDO, String model) {
        if (StringUtils.isNotBlank(model)) {
            entityDO.setModel(model.trim());
        }
    }

    private void applySessionConfig(SessionDO entityDO, SessionConfig requestConfig) {
        if (Objects.isNull(requestConfig)) {
            return;
        }
        validateSessionConfig(requestConfig);
        SessionConfig target = Objects.nonNull(entityDO.getSessionConfig()) ? entityDO.getSessionConfig()
                : new SessionConfig();
        if (Objects.nonNull(requestConfig.getReasoningEnabled())) {
            target.setReasoningEnabled(requestConfig.getReasoningEnabled());
        }
        if (Objects.nonNull(requestConfig.getTemperature())) {
            target.setTemperature(requestConfig.getTemperature());
        }
        if (Objects.nonNull(requestConfig.getMaxTokens())) {
            target.setMaxTokens(requestConfig.getMaxTokens());
        }
        if (Objects.nonNull(requestConfig.getRequireConfirmation())) {
            target.setRequireConfirmation(requestConfig.getRequireConfirmation());
        }
        entityDO.setSessionConfig(target);
    }

    private void validateSessionConfig(SessionConfig sessionConfig) {
        if (Objects.nonNull(sessionConfig.getTemperature())
                && (sessionConfig.getTemperature() < 0.0 || sessionConfig.getTemperature() > 2.0)) {
            throw new RequestException("Temperature must be between 0.0 and 2.0");
        }
        if (Objects.nonNull(sessionConfig.getMaxTokens()) && sessionConfig.getMaxTokens() < 1) {
            throw new RequestException("Max tokens must be greater than 0");
        }
    }

    @Override
    public Page<SessionBO> selectByPage(SessionQuery query) {
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
        wrapper.eq(Objects.nonNull(query.getUserId()), SessionDO::getUserId, query.getUserId());
        wrapper.like(StringUtils.isNotEmpty(query.getConversationId()), SessionDO::getConversationId,
                query.getConversationId());
        wrapper.orderByDesc(SessionDO::getOperateTime);
        return wrapper;
    }

}
