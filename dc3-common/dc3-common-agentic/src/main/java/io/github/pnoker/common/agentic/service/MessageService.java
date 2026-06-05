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
package io.github.pnoker.common.agentic.service;

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.entity.common.RequestHeader;

import java.util.List;


/**
 * Service for persisting and retrieving chat message history.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface MessageService {

    MessageBO save(String conversationId, String role, AgenticMessageContent content, String model,
                   RequestHeader.UserHeader header);

    List<MessageBO> list(String conversationId, RequestHeader.UserHeader header);

    /**
     * Load conversation history for chat memory replay. Returns the most recent
     * {@code limit} messages for the scoped conversation, ordered chronologically.
     * Unlike {@link #list(String, RequestHeader.UserHeader)} this does not depend on
     * the request user header — chat memory is keyed solely by the already
     * tenant/user-scoped conversation ID, and a non-existent conversation simply
     * returns an empty list.
     *
     * @param scopedConversationId tenant/user scoped conversation ID
     * @param limit                maximum number of messages to return
     * @return chronologically ordered (oldest first) message BO list
     */
    List<MessageBO> loadHistory(String scopedConversationId, int limit);

    /**
     * Logically delete every message that belongs to the given scoped conversation.
     *
     * @param scopedConversationId tenant/user scoped conversation ID
     * @return number of rows soft-deleted
     */
    int deleteByConversationId(String scopedConversationId);

}
