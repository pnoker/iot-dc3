/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.agentic.repository;

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import io.github.pnoker.common.entity.common.RequestHeader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for the single source of truth, {@code dc3_message}. */
public interface ReactiveMessageStore {

    Mono<MessageBO> save(String conversationId, String role, AgenticMessageContent content,
                         String model, AgenticMessageStatusEnum status, RequestHeader.PrincipalHeader header);

    Flux<MessageBO> list(String conversationId, RequestHeader.PrincipalHeader header);

    Flux<MessageBO> loadHistory(String conversationId, RequestHeader.PrincipalHeader header, int limit);

    Mono<Long> deleteByConversationId(String conversationId, RequestHeader.PrincipalHeader header);
}
