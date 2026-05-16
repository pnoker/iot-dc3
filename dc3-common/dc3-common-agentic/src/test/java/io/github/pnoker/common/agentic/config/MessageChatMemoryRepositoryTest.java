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

package io.github.pnoker.common.agentic.config;

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageChatMemoryRepositoryTest {

    @Mock
    private MessageService messageService;

    private AgenticProperties properties;
    private MessageChatMemoryRepository repository;

    @BeforeEach
    void setUp() {
        properties = new AgenticProperties();
        repository = new MessageChatMemoryRepository(messageService, properties);
    }

    @Test
    void findByConversationIdStripsTrailingUserMessages() {
        when(messageService.loadHistory("conv", properties.getHistoryWindowSize()))
                .thenReturn(List.of(row("assistant", "hello"), row("user", "current question")));

        List<Message> result = repository.findByConversationId("conv");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessageType()).isEqualTo(MessageType.ASSISTANT);
        verify(messageService).loadHistory("conv", properties.getHistoryWindowSize());
    }

    @Test
    void findByConversationIdFallsBackToMessageServiceWhenCacheMissing() {
        when(messageService.loadHistory("conv", properties.getHistoryWindowSize()))
                .thenReturn(List.of(row("assistant", "hello")));

        List<Message> result = repository.findByConversationId("conv");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessageType()).isEqualTo(MessageType.ASSISTANT);
        verify(messageService).loadHistory("conv", properties.getHistoryWindowSize());
    }

    private MessageBO row(String role, String text) {
        MessageBO row = new MessageBO();
        row.setRole(role);
        row.setContent(AgenticMessageContent.ofText(text));
        return row;
    }

}
