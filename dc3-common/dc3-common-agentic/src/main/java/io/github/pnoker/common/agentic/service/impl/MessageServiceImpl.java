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

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.repository.ReactiveMessageStore;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Tenant-scoped message history service backing the message REST resource.
 *
 * <p>Pagination is currently applied in memory: the store port has no offset/limit
 * overload yet, so the full ordered conversation is materialized and sliced here. The
 * pushdown point is a {@code list(conversationId, offset, limit, header)} overload on
 * {@link ReactiveMessageStore}; until that port lands (owned by the storage war zone),
 * this keeps the controller contract paginated and bounded.
 *
 * @author pnoker
 * @since 2026.10.21
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ReactiveMessageStore messageStore;

    @Override
    public Mono<OffsetPage<MessageBO>> list(
            long offset, int limit, String conversationId, RequestHeader.PrincipalHeader header) {
        if (StringUtils.isBlank(conversationId)) {
            return Mono.error(new RequestException("conversation_id is required"));
        }
        if (offset < 0) {
            return Mono.error(new RequestException("offset must be non-negative"));
        }
        if (limit < 1 || limit > 200) {
            return Mono.error(new RequestException("limit must be between 1 and 200"));
        }
        return Mono.zip(
                        messageStore.count(conversationId, header),
                        messageStore.list(conversationId, header, offset, limit).collectList())
                .map(tuple -> OffsetPage.of(
                        tuple.getT2(), offset, limit, tuple.getT1().intValue()));
    }
}
