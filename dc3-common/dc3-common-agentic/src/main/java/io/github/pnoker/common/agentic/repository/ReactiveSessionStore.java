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
import io.github.pnoker.db.core.page.OffsetPage;
import io.github.pnoker.db.core.page.SortSpec;
import reactor.core.publisher.Mono;

/** Reactive persistence port for agentic conversation sessions. */
public interface ReactiveSessionStore {

    /**
     * Create or refresh the session and emit the stored state.
     *
     * <p>Soft-deleted conversation ids are reusable: when the conversation's row is a soft-delete
     * tombstone, {@code touch} revives that row as a brand-new session (title and extension reset,
     * creation restarted) instead of failing on the natural-key uniqueness constraint, which must
     * stay a full constraint because {@code dc3_message}, {@code dc3_attachment}, and
     * {@code dc3_action} reference it with foreign keys.
     */
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

    /**
     * Update one session and emit the updated row. The read-merge-write sequence runs under a
     * row lock inside one transaction, so concurrent updates merge instead of overwriting each
     * other. Completes empty when the session does not exist.
     */
    Mono<SessionBO> update(
            String conversationId, SessionExt sessionExt, String title, RequestHeader.PrincipalHeader header);

    /**
     * Soft-delete the session and its messages, attachments, and actions in one transaction,
     * locking the session row first so concurrent attachment uploads serialize against the
     * delete. Returns the number of deleted session rows (0 when already deleted); stored
     * attachment file cleanup is the caller's responsibility, driven by
     * {@link ReactiveAttachmentStore#listDeletedFilePaths}.
     */
    Mono<Long> delete(String conversationId, RequestHeader.PrincipalHeader header);
}
