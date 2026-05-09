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
import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.dal.SessionManager;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.builder.SessionBuilder;
import io.github.pnoker.common.agentic.entity.model.SessionDO;
import io.github.pnoker.common.agentic.entity.query.SessionQuery;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class SessionServiceImpl implements SessionService {

    private static final byte STATUS_ACTIVE = 0;

    private static final byte STATUS_EXPIRED = 1;

    @Resource
    private SessionBuilder sessionBuilder;

    @Resource
    private SessionManager sessionManager;

    @Resource
    private ChatMemory agenticChatMemory;

    @Resource
    private AgenticProperties agenticProperties;

    @Override
    public SessionBO touch(String conversationId, String skill, Long tenantId, Long userId) {
        LocalDateTime expireTime = nextExpireTime();
        SessionDO existing = findByConversationId(conversationId);
        if (existing != null) {
            if (StringUtils.isNotEmpty(skill)) {
                existing.setSkill(skill);
            }
            existing.setTenantId(tenantId);
            existing.setUserId(userId);
            existing.setStatus(STATUS_ACTIVE);
            existing.setExpireTime(expireTime);
            sessionManager.updateById(existing);
            return sessionBuilder.buildBOByDO(existing);
        }

        SessionDO entityDO = new SessionDO();
        entityDO.setConversationId(conversationId);
        entityDO.setTenantId(tenantId);
        entityDO.setUserId(userId);
        entityDO.setTitle("New Conversation");
        entityDO.setSkill(StringUtils.defaultString(skill, ""));
        entityDO.setStatus(STATUS_ACTIVE);
        entityDO.setExpireTime(expireTime);
        entityDO.setEnableFlag((byte) 0);
        sessionManager.save(entityDO);
        return sessionBuilder.buildBOByDO(entityDO);
    }

    @Override
    public SessionBO getByConversationId(String conversationId) {
        SessionDO entityDO = findByConversationId(conversationId);
        if (isExpired(entityDO)) {
            expireSession(entityDO);
            return null;
        }
        return entityDO != null ? sessionBuilder.buildBOByDO(entityDO) : null;
    }

    @Override
    public void removeByConversationId(String conversationId) {
        SessionDO entityDO = findByConversationId(conversationId);
        if (entityDO != null) {
            sessionManager.removeById(entityDO.getId());
            agenticChatMemory.clear(conversationId);
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
                .last("LIMIT 1");
        return sessionManager.getOne(wrapper);
    }

    private LambdaQueryWrapper<SessionDO> fuzzyQuery(SessionQuery query) {
        LambdaQueryWrapper<SessionDO> wrapper = Wrappers.<SessionDO>query().lambda();
        wrapper.eq(Objects.nonNull(query.getTenantId()), SessionDO::getTenantId, query.getTenantId());
        wrapper.eq(Objects.nonNull(query.getUserId()), SessionDO::getUserId, query.getUserId());
        wrapper.eq(Objects.nonNull(query.getStatus()), SessionDO::getStatus, query.getStatus());
        if (Objects.isNull(query.getStatus()) || Objects.equals(STATUS_ACTIVE, query.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            wrapper.and(nested -> nested.isNull(SessionDO::getExpireTime).or().ge(SessionDO::getExpireTime, now));
        }
        wrapper.like(StringUtils.isNotEmpty(query.getConversationId()), SessionDO::getConversationId,
                query.getConversationId());
        return wrapper;
    }

    private LocalDateTime nextExpireTime() {
        return LocalDateTime.now().plusHours(agenticProperties.getSessionTtlHours());
    }

    private boolean isExpired(SessionDO session) {
        return session != null && session.getExpireTime() != null
                && session.getExpireTime().isBefore(LocalDateTime.now());
    }

    private void expireSession(SessionDO session) {
        session.setStatus(STATUS_EXPIRED);
        sessionManager.updateById(session);
        agenticChatMemory.clear(session.getConversationId());
    }

}
