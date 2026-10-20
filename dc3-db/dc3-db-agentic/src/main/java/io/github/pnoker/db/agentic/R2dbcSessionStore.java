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
package io.github.pnoker.db.agentic;

import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.agentic.repository.ReactiveSessionStore;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.core.dialect.R2dbcDialect;
import io.github.pnoker.db.core.page.OffsetPage;
import io.github.pnoker.db.core.page.SortSpec;
import io.github.pnoker.db.core.time.DatabaseInstant;
import io.github.pnoker.db.core.transaction.PageTransaction;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Explicit SQL adapter for agentic sessions.
 *
 * <p>Timestamps are stored in {@code TIMESTAMPTZ} columns and bound as {@link Instant} through
 * {@link R2dbcDialect#bindInstant}, so encoding never depends on the database session timezone.
 * The {@link SessionBO} wall-clock fields stay UTC-naive; reads normalize through
 * {@link DatabaseInstant#toLocalDateTimeUtc}.
 */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcSessionStore implements ReactiveSessionStore {

    private static final String TABLE = "dc3_agentic.dc3_session";
    private static final String COLUMNS = "id, conversation_id, title, session_ext, tenant_id, user_id, remark, "
            + "creator_id, creator_name, create_time, operator_id, operator_name, operate_time, deleted";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<SessionBO> touch(String conversationId, SessionExt sessionExt, RequestHeader.PrincipalHeader header) {
        return touchAttempt(conversationId, sessionExt, header, true);
    }

    /**
     * One touch attempt: lock the conversation's row regardless of tombstone state, then update,
     * revive, or insert. A unique-key violation means a concurrent touch inserted the same
     * conversation first, so the caller retries the whole decision once instead of surfacing the
     * raw constraint error.
     */
    private Mono<SessionBO> touchAttempt(
            String conversationId,
            SessionExt sessionExt,
            RequestHeader.PrincipalHeader header,
            boolean retryOnConflict) {
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.error(new IllegalArgumentException("conversationId must not be blank"));
        }
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        Mono<SessionBO> transaction = lockAny(conversationId, header)
                .flatMap(locked -> locked.deleted()
                        ? revive(conversationId, sessionExt, locked.session(), header)
                        : updateRow(conversationId, sessionExt, null, locked.session(), header))
                .switchIfEmpty(Mono.defer(() -> insert(conversationId, serialize(sessionExt), header)));
        return transactionalOperator
                .transactional(transaction)
                .onErrorResume(
                        DataIntegrityViolationException.class,
                        ignored -> retryOnConflict
                                ? touchAttempt(conversationId, sessionExt, header, false)
                                : Mono.error(ignored));
    }

    @Override
    public Mono<SessionBO> get(String conversationId, RequestHeader.PrincipalHeader header) {
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.error(new IllegalArgumentException("conversationId must not be blank"));
        }
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0 LIMIT 1")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<SessionBO>> list(
            long offset,
            int limit,
            String conversationId,
            java.util.List<SortSpec> requestedSort,
            RequestHeader.PrincipalHeader header) {
        return Mono.defer(() -> {
            if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
            if (offset < 0 || limit < 1 || limit > 200) {
                return Mono.error(new IllegalArgumentException(
                        "offset must be non-negative and limit must be between 1 and 200"));
            }
            String filter = conversationId == null || conversationId.isBlank()
                    ? ""
                    : " AND conversation_id LIKE :conversation_filter";
            String order = orderBy(requestedSort);
            DatabaseClient.GenericExecuteSpec count = databaseClient
                    .sql("SELECT COUNT(*) AS total FROM " + TABLE
                            + " WHERE tenant_id = :tenant_id AND user_id = :user_id AND deleted = 0" + filter)
                    .bind("tenant_id", header.getTenantId())
                    .bind("user_id", header.getUserId());
            DatabaseClient.GenericExecuteSpec rows = databaseClient
                    .sql("SELECT " + COLUMNS + " FROM " + TABLE
                            + " WHERE tenant_id = :tenant_id AND user_id = :user_id AND deleted = 0" + filter
                            + " ORDER BY " + order + " LIMIT :limit OFFSET :offset")
                    .bind("tenant_id", header.getTenantId())
                    .bind("user_id", header.getUserId())
                    .bind("limit", limit)
                    .bind("offset", offset);
            if (!filter.isEmpty()) {
                String pattern = "%" + conversationId.trim() + "%";
                count = count.bind("conversation_filter", pattern);
                rows = rows.bind("conversation_filter", pattern);
            }
            DatabaseClient.GenericExecuteSpec finalRows = rows;
            Mono<Long> total = count.mapValue(Long.class).one().defaultIfEmpty(0L);
            return total.flatMap(totalCount -> finalRows
                            .map(this::map)
                            .all()
                            .collectList()
                            .map(items -> OffsetPage.of(items, offset, limit, totalCount)))
                    .as(pageTransaction::transactional);
        });
    }

    private String orderBy(java.util.List<SortSpec> requestedSort) {
        if (requestedSort == null || requestedSort.isEmpty()) return "operate_time DESC, id DESC";
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<String> clauses = new java.util.ArrayList<>();
        for (SortSpec sort : requestedSort) {
            if (sort == null || !seen.add(sort.field()))
                throw new IllegalArgumentException("sort field is not allowed");
            String column =
                    switch (sort.field()) {
                        case "operate_time" -> "operate_time";
                        case "create_time" -> "create_time";
                        case "title" -> "title";
                        case "id" -> "id";
                        default -> throw new IllegalArgumentException("sort field is not allowed");
                    };
            clauses.add(column + (sort.direction() == SortSpec.Direction.ASC ? " ASC" : " DESC"));
        }
        if (!seen.contains("id")) clauses.add("id DESC");
        return String.join(", ", clauses);
    }

    @Override
    public Mono<SessionBO> update(
            String conversationId, SessionExt sessionExt, String title, RequestHeader.PrincipalHeader header) {
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.error(new IllegalArgumentException("conversationId must not be blank"));
        }
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        // The row lock serializes concurrent updates and the merge reads the locked row, so
        // concurrent preference changes merge instead of the last writer silently winning.
        Mono<SessionBO> transaction = lock(conversationId, header)
                .flatMap(existing -> updateRow(conversationId, sessionExt, title, existing, header));
        return transactionalOperator.transactional(transaction);
    }

    @Override
    public Mono<Long> delete(String conversationId, RequestHeader.PrincipalHeader header) {
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.error(new IllegalArgumentException("conversationId must not be blank"));
        }
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        // Lock the live session row first: attachment saves take the same lock before inserting,
        // so a concurrent upload either lands before this delete (and is soft-deleted with the
        // rest) or fails its guard once the session is gone. Children then the session row.
        Mono<Long> transaction = lockSessionId(conversationId, header)
                .flatMap(sessionId -> softDeleteChildren("dc3_agentic.dc3_message", conversationId, header)
                        .then(softDeleteChildren("dc3_agentic.dc3_attachment", conversationId, header))
                        .then(softDeleteChildren("dc3_agentic.dc3_action", conversationId, header))
                        .then(deleteSession(conversationId, header)))
                .defaultIfEmpty(0L);
        return transactionalOperator.transactional(transaction);
    }

    /** Lock the live session row and emit its id, or empty when the session is already deleted. */
    private Mono<Long> lockSessionId(String conversationId, RequestHeader.PrincipalHeader header) {
        return databaseClient
                .sql("SELECT id FROM " + TABLE
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0 FOR UPDATE")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map((row, metadata) -> row.get("id", Long.class))
                .one();
    }

    private Mono<Long> deleteSession(String conversationId, RequestHeader.PrincipalHeader header) {
        return databaseClient
                .sql("UPDATE " + TABLE + " SET deleted = 1,"
                        + " operator_id = :operator_id, operate_time = :operate_time WHERE conversation_id = :conversation_id"
                        + " AND tenant_id = :tenant_id AND user_id = :user_id AND deleted = 0")
                .bind("operator_id", header.getUserId())
                .bind("operate_time", dialect.bindInstant(Instant.now()))
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

    private Mono<Long> softDeleteChildren(String table, String conversationId, RequestHeader.PrincipalHeader header) {
        return databaseClient
                .sql("UPDATE " + table + " SET deleted = 1, operate_time = :operate_time"
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0")
                .bind("operate_time", dialect.bindInstant(Instant.now()))
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

    /** Lock the live session row for update; empty when the conversation has no live row. */
    private Mono<SessionBO> lock(String conversationId, RequestHeader.PrincipalHeader header) {
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0 LIMIT 1 FOR UPDATE")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map(this::map)
                .one();
    }

    /**
     * Lock the conversation's row regardless of soft-delete state. The natural key is unique
     * across live and deleted rows (it is the foreign-key target for the child tables), so the
     * tombstone row is the row that must be locked and revived to reuse the conversation id.
     */
    private Mono<LockedSession> lockAny(String conversationId, RequestHeader.PrincipalHeader header) {
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id LIMIT 1 FOR UPDATE")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map((row, metadata) -> new LockedSession(
                        map(row, metadata), row.get("deleted", Number.class).intValue() != 0))
                .one();
    }

    /**
     * Revive a soft-deleted session row as a brand-new conversation: the tombstone's title,
     * extension, creator, and creation time are reset, because the deleted conversation's
     * messages and attachments stay soft-deleted and must never reappear.
     */
    private Mono<SessionBO> revive(
            String conversationId, SessionExt sessionExt, SessionBO tombstone, RequestHeader.PrincipalHeader header) {
        return databaseClient
                .sql("UPDATE " + TABLE + " SET deleted = 0, title = :title, session_ext = "
                        + dialect.jsonWriteExpression(":session_ext") + ", creator_id = :creator_id,"
                        + " creator_name = :creator_name, create_time = :create_time, operator_id = :operator_id,"
                        + " operate_time = :operate_time WHERE id = :id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id")
                .bind("title", AgenticConstant.Session.DEFAULT_TITLE)
                .bind("session_ext", serialize(sessionExt))
                .bind("creator_id", header.getUserId())
                .bind("creator_name", header.getUserName() == null ? "" : header.getUserName())
                .bind("create_time", dialect.bindInstant(Instant.now()))
                .bind("operator_id", header.getUserId())
                .bind("operate_time", dialect.bindInstant(Instant.now()))
                .bind("id", tombstone.getId())
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? get(conversationId, header)
                        : Mono.error(new IllegalStateException("session revive affected " + rows + " rows")));
    }

    /** Merge the incoming extension onto the locked row and persist it with the optional title. */
    private Mono<SessionBO> updateRow(
            String conversationId,
            SessionExt sessionExt,
            String title,
            SessionBO existing,
            RequestHeader.PrincipalHeader header) {
        String nextTitle = title == null || title.isBlank() ? existing.getTitle() : title.trim();
        String nextExt = serialize(merge(existing.getSessionExt(), sessionExt));
        return databaseClient
                .sql("UPDATE " + TABLE + " SET title = :title, session_ext = "
                        + dialect.jsonWriteExpression(":session_ext") + ","
                        + " operator_id = :operator_id, operate_time = :operate_time WHERE id = :id"
                        + " AND tenant_id = :tenant_id AND user_id = :user_id AND deleted = 0")
                .bind("title", nextTitle)
                .bind("session_ext", nextExt)
                .bind("operator_id", header.getUserId())
                .bind("operate_time", dialect.bindInstant(Instant.now()))
                .bind("id", existing.getId())
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? get(conversationId, header)
                        : Mono.error(new IllegalStateException("session update affected " + rows + " rows")));
    }

    private Mono<SessionBO> insert(String conversationId, String json, RequestHeader.PrincipalHeader header) {
        return databaseClient
                .sql("INSERT INTO " + TABLE
                        + " (conversation_id, title, session_ext, tenant_id, user_id, creator_id, operator_id, create_time, operate_time, deleted)"
                        + " VALUES (:conversation_id, :title, " + dialect.jsonWriteExpression(":session_ext")
                        + ", :tenant_id, :user_id, :creator_id, :operator_id, :create_time, :operate_time, 0)")
                .bind("conversation_id", conversationId)
                .bind("title", AgenticConstant.Session.DEFAULT_TITLE)
                .bind("session_ext", json == null ? "{}" : json)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .bind("creator_id", header.getUserId())
                .bind("operator_id", header.getUserId())
                .bind("create_time", dialect.bindInstant(Instant.now()))
                .bind("operate_time", dialect.bindInstant(Instant.now()))
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? get(conversationId, header)
                        : Mono.error(new IllegalStateException("session insert affected " + rows + " rows")));
    }

    private SessionBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        SessionBO session = new SessionBO();
        session.setId(row.get("id", Long.class));
        session.setConversationId(row.get("conversation_id", String.class));
        session.setTitle(row.get("title", String.class));
        session.setSessionExt(deserialize(row.get("session_ext", String.class)));
        session.setTenantId(row.get("tenant_id", Long.class));
        session.setUserId(row.get("user_id", Long.class));
        session.setRemark(row.get("remark", String.class));
        session.setCreatorId(row.get("creator_id", Long.class));
        session.setCreatorName(row.get("creator_name", String.class));
        session.setCreateTime(DatabaseInstant.toLocalDateTimeUtc(row.get("create_time")));
        session.setOperatorId(row.get("operator_id", Long.class));
        session.setOperatorName(row.get("operator_name", String.class));
        session.setOperateTime(DatabaseInstant.toLocalDateTimeUtc(row.get("operate_time")));
        return session;
    }

    private SessionExt deserialize(String value) {
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, SessionExt.class);
        } catch (Exception exception) {
            throw new IllegalStateException("invalid dc3_session session_ext JSON", exception);
        }
    }

    private String serialize(SessionExt value) {
        if (value == null) return "{}";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("session_ext is not valid JSON", exception);
        }
    }

    private SessionExt merge(SessionExt current, SessionExt incoming) {
        if (incoming == null) return current;
        SessionExt merged = current == null ? new SessionExt() : current;
        if (incoming.getModel() != null && !incoming.getModel().isBlank())
            merged.setModel(incoming.getModel().trim());
        if (incoming.getReasoningEnabled() != null) merged.setReasoningEnabled(incoming.getReasoningEnabled());
        if (incoming.getTemperature() != null) merged.setTemperature(incoming.getTemperature());
        if (incoming.getMaxTokens() != null) merged.setMaxTokens(incoming.getMaxTokens());
        if (incoming.getIcon() != null && !incoming.getIcon().isBlank())
            merged.setIcon(incoming.getIcon().trim());
        if (incoming.getCategory() != null && !incoming.getCategory().isBlank()) {
            merged.setCategory(incoming.getCategory().trim());
        }
        if (incoming.getArchived() != null) merged.setArchived(incoming.getArchived());
        if (merged.getTemperature() != null && (merged.getTemperature() < 0 || merged.getTemperature() > 2)) {
            throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
        }
        if (merged.getMaxTokens() != null && merged.getMaxTokens() < 1) {
            throw new IllegalArgumentException("maxTokens must be greater than 0");
        }
        return merged;
    }

    /** A session row read under lock together with its soft-delete tombstone flag. */
    private record LockedSession(SessionBO session, boolean deleted) {}
}
