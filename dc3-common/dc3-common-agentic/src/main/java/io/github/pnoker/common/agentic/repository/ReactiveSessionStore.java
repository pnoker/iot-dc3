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

import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import reactor.core.publisher.Mono;

/** Reactive persistence port for agentic conversation sessions. */
public interface ReactiveSessionStore {

    /** Create or refresh the session and emit the stored state. */
    Mono<SessionBO> touch(String conversationId, SessionExt sessionExt, RequestHeader.PrincipalHeader header);

    /** Load the session for the request. */
    Mono<SessionBO> get(String conversationId, RequestHeader.PrincipalHeader header);

    /** Page sessions matching the tenant-scoped filters. */
    Mono<OffsetPage<SessionBO>> list(
            long offset,
            int limit,
            String conversationId,
            java.util.List<SortSpec> sort,
            RequestHeader.PrincipalHeader header);

    /** Update one session and emit the updated row. */
    Mono<SessionBO> update(
            String conversationId, SessionExt sessionExt, String title, RequestHeader.PrincipalHeader header);

    /** Delete the session. */
    Mono<Long> delete(String conversationId, RequestHeader.PrincipalHeader header);
}
