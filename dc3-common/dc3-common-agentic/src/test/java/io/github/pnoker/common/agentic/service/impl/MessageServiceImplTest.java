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
import io.github.pnoker.common.agentic.dal.MessageManager;
import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.builder.MessageBuilder;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.MessageDO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageManager messageManager;

    @Mock
    private MessageBuilder messageBuilder;

    private MessageServiceImpl service;
    private RequestHeader.UserHeader header;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(messageManager, messageBuilder);
        header = new RequestHeader.UserHeader();
        header.setTenantId(1L);
        header.setUserId(2L);
        header.setUserName("admin");
    }

    @Test
    void saveStartsMessageIndexAtOneForFirstMessage() {
        when(messageManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        MessageDO entityDO = new MessageDO();
        when(messageBuilder.buildDOByBO(any(MessageBO.class))).thenReturn(entityDO);
        when(messageBuilder.buildBOByDO(entityDO)).thenAnswer(inv -> {
            MessageBO bo = new MessageBO();
            bo.setMessageIndex(1L);
            return bo;
        });

        ArgumentCaptor<MessageBO> captor = ArgumentCaptor.forClass(MessageBO.class);
        service.save("conv", "user", AgenticMessageContent.ofText("hi"), "claude", header);

        org.mockito.Mockito.verify(messageBuilder).buildDOByBO(captor.capture());
        MessageBO captured = captor.getValue();
        assertThat(captured.getMessageIndex()).isEqualTo(1L);
        assertThat(captured.getStatus()).isEqualTo(AgenticMessageStatusEnum.OK);
        assertThat(captured.getRole()).isEqualTo("user");
        assertThat(captured.getTenantId()).isEqualTo(1L);
        assertThat(captured.getUserId()).isEqualTo(2L);
        assertThat(captured.getCreateTime()).isNotNull();
    }

    @Test
    void saveIncrementsMessageIndexFromLatestRow() {
        MessageDO latest = new MessageDO();
        latest.setMessageIndex(10L);
        when(messageManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(latest);
        when(messageBuilder.buildDOByBO(any(MessageBO.class))).thenReturn(new MessageDO());
        when(messageBuilder.buildBOByDO(any(MessageDO.class))).thenReturn(new MessageBO());

        ArgumentCaptor<MessageBO> captor = ArgumentCaptor.forClass(MessageBO.class);
        service.save("conv", "assistant", AgenticMessageContent.ofText("ok"), "claude", header);

        org.mockito.Mockito.verify(messageBuilder).buildDOByBO(captor.capture());
        assertThat(captor.getValue().getMessageIndex()).isEqualTo(11L);
    }

    @Test
    void saveCoercesNullContentToEmptyText() {
        when(messageManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(messageBuilder.buildDOByBO(any(MessageBO.class))).thenReturn(new MessageDO());
        when(messageBuilder.buildBOByDO(any(MessageDO.class))).thenReturn(new MessageBO());

        ArgumentCaptor<MessageBO> captor = ArgumentCaptor.forClass(MessageBO.class);
        service.save("conv", "user", null, "claude", header);

        org.mockito.Mockito.verify(messageBuilder).buildDOByBO(captor.capture());
        assertThat(captor.getValue().getContent()).isNotNull();
        assertThat(captor.getValue().getContent().getText()).isEmpty();
    }

    @Test
    void listInjectsEmptyTextContentWhenMissing() {
        MessageDO dirty = new MessageDO();
        when(messageManager.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(dirty));

        MessageBO bo = new MessageBO();
        bo.setRole("user");
        bo.setContent(null);
        when(messageBuilder.buildBOListByDOList(List.of(dirty))).thenReturn(List.of(bo));

        List<MessageBO> result = service.list("conv", header);

        assertThat(result.get(0).getContent()).isNotNull();
        assertThat(result.get(0).getContent().getText()).isEmpty();
    }
}
