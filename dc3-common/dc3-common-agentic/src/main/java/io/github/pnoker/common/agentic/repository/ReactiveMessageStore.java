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
package io.github.pnoker.common.agentic.repository;

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for the single source of truth, {@code dc3_message}. */
public interface ReactiveMessageStore {

    /** Save the message, inserting or updating as needed. */
    Mono<MessageBO> save(
            String conversationId,
            String role,
            AgenticMessageContent content,
            String model,
            AgenticMessageStatusEnum status,
            RequestHeader.PrincipalHeader header);

    /** Stream messages matching the request. */
    Flux<MessageBO> list(String conversationId, RequestHeader.PrincipalHeader header);

    /**
     * Count the live messages of one conversation for the principal.
     *
     * @param conversationId conversation id
     * @param header request header carrying tenant and user scope
     * @return the live message count
     */
    default Mono<Long> count(String conversationId, RequestHeader.PrincipalHeader header) {
        return list(conversationId, header).count();
    }

    /**
     * Stream one page of the conversation's messages in stable index order.
     * Implementations are encouraged to push offset/limit down to the database;
     * the default materializes the full ordered stream and slices in memory.
     *
     * @param conversationId conversation id
     * @param header request header carrying tenant and user scope
     * @param offset zero-based row offset
     * @param limit page size, 1..200 enforced by the caller
     * @return the page rows in message-index order
     */
    default Flux<MessageBO> list(String conversationId, RequestHeader.PrincipalHeader header, long offset, int limit) {
        Flux<MessageBO> window = list(conversationId, header).skip(offset);
        return limit > 0 ? window.take(limit) : window;
    }

    /** Stream the conversation history newest-first for the principal. */
    Flux<MessageBO> loadHistory(String conversationId, RequestHeader.PrincipalHeader header, int limit);

    /** Delete the records matched by conversation id. */
    Mono<Long> deleteByConversationId(String conversationId, RequestHeader.PrincipalHeader header);
}
