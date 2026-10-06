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
import io.github.pnoker.common.agentic.service.impl.AttachmentFileCleaner;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.core.dialect.R2dbcDialect;
import io.github.pnoker.db.core.page.OffsetPage;
import io.github.pnoker.db.core.page.SortSpec;
import io.github.pnoker.db.core.time.DatabaseInstant;
import io.github.pnoker.db.core.transaction.PageTransaction;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import tools.jackson.databind.ObjectMapper;

/** Explicit SQL adapter for agentic sessions. */
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
    private final AttachmentFileCleaner attachmentFileCleaner;

    @Override
    public Mono<SessionBO> touch(String conversationId, SessionExt sessionExt, RequestHeader.PrincipalHeader header) {
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.error(new IllegalArgumentException("conversationId must not be blank"));
        }
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        Mono<SessionBO> transaction = lock(conversationId, header)
                .flatMap(existing -> updateExisting(conversationId, sessionExt, existing, header))
                .switchIfEmpty(insert(conversationId, serialize(sessionExt), header));
        return transactionalOperator
                .transactional(transaction)
                .onErrorResume(
                        DataIntegrityViolationException.class,
                        ignored -> get(conversationId, header).switchIfEmpty(Mono.error(ignored)));
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
        return get(conversationId, header).flatMap(existing -> {
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
                    .bind("operate_time", utcNow())
                    .bind("id", existing.getId())
                    .bind("tenant_id", header.getTenantId())
                    .bind("user_id", header.getUserId())
                    .fetch()
                    .rowsUpdated()
                    .flatMap(rows -> rows == 1 ? get(conversationId, header) : Mono.empty());
        });
    }

    @Override
    public Mono<Long> delete(String conversationId, RequestHeader.PrincipalHeader header) {
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.error(new IllegalArgumentException("conversationId must not be blank"));
        }
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        Mono<Long> deleteMessages = databaseClient
                .sql("UPDATE dc3_agentic.dc3_message SET deleted = 1,"
                        + " operate_time = :operate_time WHERE conversation_id = :conversation_id"
                        + " AND tenant_id = :tenant_id AND user_id = :user_id AND deleted = 0")
                .bind("operate_time", utcNow())
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
        Mono<Long> deleteAttachments = softDeleteChildren("dc3_agentic.dc3_attachment", conversationId, header);
        Mono<Long> deleteActions = softDeleteChildren("dc3_agentic.dc3_action", conversationId, header);
        Mono<Long> deleteSession = databaseClient
                .sql("UPDATE " + TABLE + " SET deleted = 1,"
                        + " operator_id = :operator_id, operate_time = :operate_time WHERE conversation_id = :conversation_id"
                        + " AND tenant_id = :tenant_id AND user_id = :user_id AND deleted = 0")
                .bind("operator_id", header.getUserId())
                .bind("operate_time", utcNow())
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
        return transactionalOperator
                .transactional(listAttachmentFilePaths(conversationId, header)
                        .collectList()
                        .flatMap(filePaths -> deleteMessages
                                .then(deleteAttachments)
                                .then(deleteActions)
                                .then(deleteSession)
                                .map(deleted -> Tuples.of(deleted, filePaths))))
                .flatMap(deletedSession -> unlinkAttachmentFiles(deletedSession).thenReturn(deletedSession.getT1()));
    }

    /** Read attachment file locations while the rows are still live, so the files can be unlinked after commit. */
    private Flux<String> listAttachmentFilePaths(String conversationId, RequestHeader.PrincipalHeader header) {
        return databaseClient
                .sql("SELECT file_path FROM dc3_agentic.dc3_attachment"
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map((row, metadata) -> row.get("file_path", String.class))
                .all();
    }

    /**
     * Unlink the conversation's stored attachment files after the session delete commits. Runs
     * strictly after the transaction so file cleanup can never roll the delete back, and is
     * warn-only so a missing or undeletable file never fails the committed delete.
     */
    private Mono<Void> unlinkAttachmentFiles(Tuple2<Long, List<String>> deletedSession) {
        return attachmentFileCleaner.unlinkAll(deletedSession.getT2());
    }

    private Mono<Long> softDeleteChildren(String table, String conversationId, RequestHeader.PrincipalHeader header) {
        return databaseClient
                .sql("UPDATE " + table + " SET deleted = 1, operate_time = :operate_time"
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0")
                .bind("operate_time", utcNow())
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

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

    private Mono<SessionBO> updateExisting(
            String conversationId, SessionExt sessionExt, SessionBO existing, RequestHeader.PrincipalHeader header) {
        String nextJson = serialize(merge(existing.getSessionExt(), sessionExt));
        return databaseClient
                .sql("UPDATE " + TABLE + " SET session_ext = " + dialect.jsonWriteExpression(":session_ext")
                        + ", operator_id = :operator_id," + " operate_time = :operate_time WHERE id = :id")
                .bind("session_ext", nextJson)
                .bind("operator_id", header.getUserId())
                .bind("operate_time", utcNow())
                .bind("id", existing.getId())
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
                .bind("create_time", utcNow())
                .bind("operate_time", utcNow())
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

    private LocalDateTime utcNow() {
        return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    }
}
