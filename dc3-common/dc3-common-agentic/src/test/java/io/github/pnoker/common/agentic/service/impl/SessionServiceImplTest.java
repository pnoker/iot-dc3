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
import io.github.pnoker.common.agentic.dal.SessionManager;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.builder.SessionBuilder;
import io.github.pnoker.common.agentic.entity.model.SessionDO;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.agentic.entity.request.SessionUpdateRequest;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.exception.RequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionBuilder sessionBuilder;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private ChatMemory agenticChatMemory;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private SessionServiceImpl service;

    private static LambdaQueryWrapper<SessionDO> anySessionQuery() {
        return any();
    }

    @Test
    void touchCreatesNewSessionWhenConversationIdUnknown() {
        when(sessionManager.getOne(anySessionQuery())).thenReturn(null);
        SessionBO bo = new SessionBO();
        when(sessionBuilder.buildBOByDO(any(SessionDO.class))).thenReturn(bo);

        SessionExt sessionExt = new SessionExt();
        sessionExt.setModel("deepseek-chat");
        sessionExt.setReasoningEnabled(true);
        sessionExt.setTemperature(0.3);
        sessionExt.setMaxTokens(1024);

        SessionBO result = service.touch("conv-1", 1L, 2L, sessionExt);

        ArgumentCaptor<SessionDO> captor = ArgumentCaptor.forClass(SessionDO.class);
        verify(sessionManager).save(captor.capture());
        SessionDO saved = captor.getValue();
        assertThat(saved.getConversationId()).isEqualTo("conv-1");
        assertThat(saved.getTenantId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isEqualTo(2L);
        assertThat(saved.getTitle()).isEqualTo("New Conversation");
        assertThat(saved.getSessionExt().getModel()).isEqualTo("deepseek-chat");
        assertThat(saved.getSessionExt().getReasoningEnabled()).isTrue();
        assertThat(saved.getSessionExt().getTemperature()).isEqualTo(0.3);
        assertThat(saved.getSessionExt().getMaxTokens()).isEqualTo(1024);
        assertThat(result).isSameAs(bo);
    }

    @Test
    void touchUpdatesExistingSessionInPlace() {
        SessionDO existing = new SessionDO();
        existing.setId(99L);
        existing.setConversationId("conv-1");
        existing.setTitle("Old title");
        existing.setTenantId(1L);
        existing.setUserId(2L);
        when(sessionManager.getOne(anySessionQuery())).thenReturn(existing);
        when(sessionBuilder.buildBOByDO(existing)).thenReturn(new SessionBO());

        SessionExt sessionExt = new SessionExt();
        sessionExt.setModel("qwen-plus");
        service.touch("conv-1", 1L, 2L, sessionExt);

        assertThat(existing.getSessionExt().getModel()).isEqualTo("qwen-plus");
        verify(sessionManager).updateById(existing);
        verify(sessionManager, never()).save(any(SessionDO.class));
    }

    @Test
    void getByConversationIdReturnsNullWhenAbsent() {
        when(sessionManager.getOne(anySessionQuery())).thenReturn(null);
        assertThat(service.getByConversationId("missing")).isNull();
    }

    @Test
    void deleteByConversationIdClearsChatMemoryWhenSessionExists() {
        SessionDO existing = new SessionDO();
        existing.setId(99L);
        when(sessionManager.getOne(anySessionQuery())).thenReturn(existing);

        service.deleteByConversationId("conv-1");

        verify(sessionManager).removeById(99L);
        verify(agenticChatMemory).clear("conv-1");
    }

    @Test
    void deleteByConversationIdIsNoOpWhenSessionMissing() {
        when(sessionManager.getOne(anySessionQuery())).thenReturn(null);

        service.deleteByConversationId("missing");

        verify(sessionManager, never()).removeById(any());
        verify(agenticChatMemory, never()).clear(any());
    }

    @Test
    void updateReturnsNullWhenSessionMissing() {
        when(sessionManager.getOne(anySessionQuery())).thenReturn(null);
        assertThat(service.update("missing", new SessionUpdateRequest())).isNull();
    }

    @Test
    void updateReturnsNullWhenRequestIsNull() {
        SessionDO existing = new SessionDO();
        when(sessionManager.getOne(anySessionQuery())).thenReturn(existing);
        assertThat(service.update("conv-1", null)).isNull();
    }

    @Test
    void updateAppliesTrimmedTitle() {
        SessionDO existing = new SessionDO();
        existing.setTitle("Old");
        when(sessionManager.getOne(anySessionQuery())).thenReturn(existing);
        when(sessionBuilder.buildBOByDO(existing)).thenReturn(new SessionBO());

        SessionUpdateRequest request = new SessionUpdateRequest();
        request.setTitle("  New title  ");
        SessionExt sessionExt = new SessionExt();
        sessionExt.setModel("  glm-4  ");
        request.setSessionExt(sessionExt);
        service.update("conv-1", request);

        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getSessionExt().getModel()).isEqualTo("glm-4");
        verify(sessionManager).updateById(existing);
    }

    @Test
    void updateLeavesTitleUntouchedWhenRequestTitleBlank() {
        SessionDO existing = new SessionDO();
        existing.setTitle("Old");
        when(sessionManager.getOne(anySessionQuery())).thenReturn(existing);
        when(sessionBuilder.buildBOByDO(existing)).thenReturn(new SessionBO());

        SessionUpdateRequest request = new SessionUpdateRequest();
        request.setTitle("   ");
        service.update("conv-1", request);

        assertThat(existing.getTitle()).isEqualTo("Old");
    }

    @Test
    void updateMergesSessionExt() {
        SessionDO existing = new SessionDO();
        SessionExt existingConfig = new SessionExt();
        existingConfig.setReasoningEnabled(false);
        existingConfig.setTemperature(0.7);
        existingConfig.setMaxTokens(2048);
        existing.setSessionExt(existingConfig);
        when(sessionManager.getOne(anySessionQuery())).thenReturn(existing);
        when(sessionBuilder.buildBOByDO(existing)).thenReturn(new SessionBO());

        SessionExt patch = new SessionExt();
        patch.setReasoningEnabled(true);
        patch.setMaxTokens(4096);
        SessionUpdateRequest request = new SessionUpdateRequest();
        request.setSessionExt(patch);
        service.update("conv-1", request);

        assertThat(existing.getSessionExt().getReasoningEnabled()).isTrue();
        assertThat(existing.getSessionExt().getTemperature()).isEqualTo(0.7);
        assertThat(existing.getSessionExt().getMaxTokens()).isEqualTo(4096);
        verify(sessionManager).updateById(existing);
    }

    @Test
    void updateRejectsInvalidSessionExt() {
        SessionDO existing = new SessionDO();
        when(sessionManager.getOne(anySessionQuery())).thenReturn(existing);

        SessionExt patch = new SessionExt();
        patch.setTemperature(3.0);
        SessionUpdateRequest request = new SessionUpdateRequest();
        request.setSessionExt(patch);

        assertThatThrownBy(() -> service.update("conv-1", request))
                .isInstanceOf(RequestException.class)
                .hasMessage("Temperature must be between 0.0 and 2.0");
        verify(sessionManager, never()).updateById(any(SessionDO.class));
    }
}
