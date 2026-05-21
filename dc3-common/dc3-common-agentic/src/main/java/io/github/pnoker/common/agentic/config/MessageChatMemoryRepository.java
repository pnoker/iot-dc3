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
import io.github.pnoker.common.constant.service.AgenticConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * {@link ChatMemoryRepository} backed by the existing {@code dc3_message} business
 * table. Treats {@code dc3_message} as the single source of truth for both UI history
 * playback and model context — eliminates the {@code dc3_chat_memory} double-write.
 *
 * <p>Behaviour notes:
 * <ul>
 *   <li>{@link #findByConversationId} returns at most {@code historyWindowSize}
 *       persisted messages, oldest first.</li>
 *   <li>Trailing {@code user} messages are removed from the replay window so the
 *       in-flight turn (already persisted by the orchestration service before the
 *       LLM call) does not appear twice in the prompt — once via memory and once
 *       via {@code .user(...)}.</li>
 *   <li>{@link #saveAll} is intentionally a no-op. Persistence is owned by the
 *       orchestration service which writes a richer payload (tools, contexts,
 *       tokens) than the {@link Message} envelope can carry.</li>
 *   <li>{@link #deleteByConversationId} delegates to
 *       {@link MessageService#removeByConversationId(String)} so wiping a session
 *       wipes both replay history and persisted business records together.</li>
 * </ul>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RequiredArgsConstructor
public class MessageChatMemoryRepository implements ChatMemoryRepository {

    private final MessageService messageService;
    private final AgenticProperties properties;

    @Override
    public List<String> findConversationIds() {
        return Collections.emptyList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        if (StringUtils.isBlank(conversationId) || !properties.isMemoryEnabled()) {
            return Collections.emptyList();
        }
        List<MessageBO> history;
        try {
            history = messageService.loadHistory(conversationId, properties.getHistoryWindowSize());
        } catch (Exception e) {
            log.warn("Agentic chat memory load failed, conversationId={}", conversationId, e);
            return Collections.emptyList();
        }
        if (Objects.isNull(history) || history.isEmpty()) {
            log.debug("Agentic memory replay empty, conversationId={}", conversationId);
            return Collections.emptyList();
        }
        List<Message> messages = new ArrayList<>(history.size());
        for (MessageBO row : history) {
            Message message = toMessage(row);
            if (Objects.nonNull(message)) {
                messages.add(message);
            }
        }
        int rawCount = messages.size();
        stripTrailingUserMessages(messages);
        log.debug("Agentic memory replay, conversationId={}, raw={}, afterStrip={}",
                conversationId, rawCount, messages.size());
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // Intentionally no-op. dc3_message persistence is performed by the
        // orchestration service so that tools, contexts, and token
        // accounting are captured atomically alongside text content.
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (StringUtils.isBlank(conversationId)) {
            return;
        }
        try {
            int removed = messageService.removeByConversationId(conversationId);
            log.debug("Agentic chat memory cleared, conversationId={}, rows={}", conversationId, removed);
        } catch (Exception e) {
            log.warn("Agentic chat memory clear failed, conversationId={}", conversationId, e);
        }
    }

    private Message toMessage(MessageBO row) {
        if (Objects.isNull(row) || StringUtils.isBlank(row.getRole())) {
            return null;
        }
        AgenticMessageContent content = Objects.nonNull(row.getContent()) ? row.getContent()
                : AgenticMessageContent.ofText("");
        String text = StringUtils.defaultString(content.getText());
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return switch (row.getRole().toLowerCase()) {
            case AgenticConstant.Chat.ROLE_USER -> new UserMessage(text);
            case AgenticConstant.Chat.ROLE_ASSISTANT -> new AssistantMessage(text);
            case AgenticConstant.Chat.ROLE_SYSTEM -> new SystemMessage(text);
            default -> null;
        };
    }

    private void stripTrailingUserMessages(List<Message> messages) {
        while (!messages.isEmpty()) {
            Message tail = messages.get(messages.size() - 1);
            if (Objects.nonNull(tail) && MessageType.USER.equals(tail.getMessageType())) {
                messages.remove(messages.size() - 1);
                continue;
            }
            break;
        }
    }

}
