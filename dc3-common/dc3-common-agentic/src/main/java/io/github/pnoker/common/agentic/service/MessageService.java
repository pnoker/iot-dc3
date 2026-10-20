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
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/**
 * Reactive service for tenant-scoped agentic conversation message history.
 *
 * @author pnoker
 * @since 2026.10.21
 */
public interface MessageService {

    /**
     * Page the ordered message history of one conversation, scoped to the authenticated
     * tenant and user.
     *
     * @param offset         zero-based result offset, non-negative
     * @param limit          page size between 1 and 200
     * @param conversationId client-visible conversation identifier, non-blank
     * @param header         authenticated caller principal and tenant
     * @return an offset page of messages in chronological order
     */
    Mono<OffsetPage<MessageBO>> list(
            long offset, int limit, String conversationId, RequestHeader.PrincipalHeader header);
}
