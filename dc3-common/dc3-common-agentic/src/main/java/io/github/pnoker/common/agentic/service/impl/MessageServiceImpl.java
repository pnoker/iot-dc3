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

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.agentic.dal.MessageManager;
import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.builder.MessageBuilder;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.MessageDO;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implements chat message persistence and history retrieval for conversation replay.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageManager messageManager;
    private final MessageBuilder messageBuilder;

    @Override
    public MessageBO save(String conversationId, String role, AgenticMessageContent content, String model,
                          RequestHeader.UserHeader header) {
        MessageBO entityBO = new MessageBO();
        entityBO.setConversationId(conversationId);
        entityBO.setRole(role);
        entityBO.setContent(Objects.nonNull(content) ? content : AgenticMessageContent.ofText(""));
        entityBO.setModel(model);
        entityBO.setMessageIndex(nextMessageIndex(conversationId));
        entityBO.setStatus(AgenticMessageStatusEnum.OK);
        entityBO.setTenantId(header.getTenantId());
        entityBO.setUserId(header.getUserId());
        fillCreateAudit(entityBO, header);
        MessageDO entityDO = messageBuilder.buildDOByBO(entityBO);
        messageManager.save(entityDO);
        return messageBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<MessageBO> list(String conversationId, RequestHeader.UserHeader header) {
        LambdaQueryWrapper<MessageDO> wrapper = Wrappers.<MessageDO>query()
                .lambda()
                .eq(MessageDO::getConversationId, conversationId)
                .eq(MessageDO::getTenantId, header.getTenantId())
                .eq(MessageDO::getUserId, header.getUserId())
                .orderByAsc(MessageDO::getCreateTime)
                .orderByAsc(MessageDO::getId);
        return messageBuilder.buildBOListByDOList(messageManager.list(wrapper)).stream()
                .map(this::normalize)
                .toList();
    }

    @Override
    public List<MessageBO> loadHistory(String scopedConversationId, int limit) {
        if (StringUtils.isBlank(scopedConversationId) || limit <= 0) {
            return List.of();
        }
        LambdaQueryWrapper<MessageDO> wrapper = Wrappers.<MessageDO>query()
                .lambda()
                .eq(MessageDO::getConversationId, scopedConversationId)
                .orderByDesc(MessageDO::getCreateTime)
                .orderByDesc(MessageDO::getId)
                .last("LIMIT " + limit);
        List<MessageDO> latest = messageManager.list(wrapper);
        if (latest.isEmpty()) {
            return List.of();
        }
        Collections.reverse(latest);
        return messageBuilder.buildBOListByDOList(latest).stream()
                .map(this::normalize)
                .toList();
    }

    @Override
    public int removeByConversationId(String scopedConversationId) {
        if (StringUtils.isBlank(scopedConversationId)) {
            return 0;
        }
        LambdaQueryWrapper<MessageDO> wrapper = Wrappers.<MessageDO>query()
                .lambda()
                .eq(MessageDO::getConversationId, scopedConversationId);
        long pending = messageManager.count(wrapper);
        if (pending <= 0) {
            return 0;
        }
        messageManager.remove(wrapper);
        return (int) pending;
    }

    private long nextMessageIndex(String conversationId) {
        LambdaQueryWrapper<MessageDO> wrapper = Wrappers.<MessageDO>query()
                .lambda()
                .eq(MessageDO::getConversationId, conversationId)
                .orderByDesc(MessageDO::getCreateTime)
                .orderByDesc(MessageDO::getId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        MessageDO latest = messageManager.getOne(wrapper);
        return Objects.isNull(latest) || Objects.isNull(latest.getMessageIndex()) ? 1 : latest.getMessageIndex() + 1;
    }

    private MessageBO normalize(MessageBO entityBO) {
        AgenticMessageContent content = Objects.nonNull(entityBO.getContent()) ? entityBO.getContent()
                : AgenticMessageContent.ofText("");
        entityBO.setContent(content);
        return entityBO;
    }

    private void fillCreateAudit(MessageBO entityBO, RequestHeader.UserHeader header) {
        LocalDateTime now = LocalDateTime.now();
        entityBO.setCreateTime(now);
        entityBO.setOperateTime(now);
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getUserName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getUserName());
    }

}
