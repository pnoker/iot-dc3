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
import io.github.pnoker.common.agentic.dal.MessageManager;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.MessageDO;
import io.github.pnoker.common.agentic.entity.vo.MessageVO;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class MessageServiceImpl implements MessageService {

    private static final byte STATUS_OK = 0;

    private static final List<String> INTERNAL_TEXT_MARKERS = List.of(
            "\n\nBefore executing any write, delete, control, or external side-effect action, ask me for explicit confirmation.",
            "\n\nAttached files available to the user:",
            "\n\nBackend context:");

    private final MessageManager messageManager;

    public MessageServiceImpl(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    @Override
    public MessageVO save(String conversationId, String role, AgenticMessageContent content, String model,
                          RequestHeader.UserHeader header) {
        MessageDO entity = new MessageDO();
        entity.setConversationId(conversationId);
        entity.setRole(role);
        entity.setContent(Objects.nonNull(content) ? content : AgenticMessageContent.ofText(""));
        entity.setModel(model);
        entity.setMessageIndex(nextMessageIndex(conversationId, header));
        entity.setStatus(STATUS_OK);
        entity.setTenantId(header.getTenantId());
        entity.setUserId(header.getUserId());
        entity.setCreateTime(LocalDateTime.now());
        entity.setOperateTime(entity.getCreateTime());
        entity.setCreatorId(header.getUserId());
        entity.setOperatorId(header.getUserId());
        entity.setCreatorName(header.getUserName());
        entity.setOperatorName(header.getUserName());
        messageManager.save(entity);
        return toVO(entity);
    }

    @Override
    public List<MessageVO> list(String conversationId, RequestHeader.UserHeader header) {
        LambdaQueryWrapper<MessageDO> wrapper = Wrappers.<MessageDO>query()
                .lambda()
                .eq(MessageDO::getConversationId, conversationId)
                .eq(MessageDO::getTenantId, header.getTenantId())
                .eq(MessageDO::getUserId, header.getUserId())
                .orderByAsc(MessageDO::getMessageIndex)
                .orderByAsc(MessageDO::getCreateTime);
        return messageManager.list(wrapper).stream().map(this::toVO).toList();
    }

    private long nextMessageIndex(String conversationId, RequestHeader.UserHeader header) {
        LambdaQueryWrapper<MessageDO> wrapper = Wrappers.<MessageDO>query()
                .lambda()
                .eq(MessageDO::getConversationId, conversationId)
                .eq(MessageDO::getTenantId, header.getTenantId())
                .eq(MessageDO::getUserId, header.getUserId())
                .orderByDesc(MessageDO::getMessageIndex)
                .last("LIMIT 1");
        MessageDO latest = messageManager.getOne(wrapper);
        return Objects.isNull(latest) || Objects.isNull(latest.getMessageIndex()) ? 1 : latest.getMessageIndex() + 1;
    }

    private MessageVO toVO(MessageDO entity) {
        AgenticMessageContent content = Objects.nonNull(entity.getContent()) ? entity.getContent()
                : AgenticMessageContent.ofText("");
        normalizeContentText(entity.getRole(), content);
        MessageVO vo = new MessageVO();
        vo.setId(entity.getId());
        vo.setConversationId(entity.getConversationId());
        vo.setRole(entity.getRole());
        vo.setContent(StringUtils.defaultString(content.getText()));
        vo.setContentExt(content);
        vo.setModel(entity.getModel());
        vo.setSkills(content.getSkills());
        vo.setMessageIndex(entity.getMessageIndex());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setOperateTime(entity.getOperateTime());
        return vo;
    }

    private void normalizeContentText(String role, AgenticMessageContent content) {
        if (!"user".equals(role) || Objects.isNull(content)) {
            return;
        }
        String text = StringUtils.defaultString(content.getText());
        int firstMarkerIndex = INTERNAL_TEXT_MARKERS.stream()
                .mapToInt(text::indexOf)
                .filter(index -> index >= 0)
                .min()
                .orElse(-1);
        if (firstMarkerIndex >= 0) {
            content.setText(StringUtils.stripEnd(text.substring(0, firstMarkerIndex), null));
        }
    }

}
