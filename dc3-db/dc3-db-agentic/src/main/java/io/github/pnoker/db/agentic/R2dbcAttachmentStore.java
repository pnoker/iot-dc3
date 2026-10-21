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

import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.agentic.repository.ReactiveAttachmentStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.core.dialect.R2dbcDialect;
import io.github.pnoker.db.core.time.DatabaseInstant;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Explicit SQL adapter for attachment metadata.
 *
 * <p>Timestamps are stored in {@code TIMESTAMPTZ} columns and bound as {@link Instant} through
 * {@link R2dbcDialect#bindInstant}, so encoding never depends on the database session timezone.
 * The {@link AttachmentBO} wall-clock fields stay UTC-naive; reads normalize through
 * {@link DatabaseInstant#toLocalDateTimeUtc}.
 */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcAttachmentStore implements ReactiveAttachmentStore {

    private static final String TABLE = "dc3_agentic.dc3_attachment";
    private static final String COLUMNS =
            "id, conversation_id, file_name, content_type, size, file_path, tenant_id, user_id,"
                    + " remark, creator_id, creator_name, create_time, operator_id, operator_name, operate_time, deleted";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcDialect dialect;

    @Override
    public Mono<AttachmentBO> save(AttachmentBO attachment) {
        Objects.requireNonNull(attachment, "attachment must not be null");
        Instant now = Instant.now();
        String sql = "INSERT INTO " + TABLE
                + " (conversation_id, file_name, content_type, size, file_path, tenant_id, user_id,"
                + " creator_id, creator_name, operator_id, operator_name, create_time, operate_time, deleted)"
                + " VALUES (:conversation_id, :file_name, :content_type, :size, :file_path, :tenant_id, :user_id,"
                + " :creator_id, :creator_name, :operator_id, :operator_name, :create_time, :operate_time, 0)";
        // Guard on the live session row under the same lock the session delete takes: a save
        // racing a delete either lands before the delete soft-deletes the children or fails
        // here, never surviving as a live row of a soft-deleted conversation.
        Mono<Void> sessionGuard = databaseClient
                .sql("SELECT id FROM dc3_agentic.dc3_session"
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0 FOR UPDATE")
                .bind("conversation_id", attachment.getConversationId())
                .bind("tenant_id", attachment.getTenantId())
                .bind("user_id", attachment.getUserId())
                .fetch()
                .all()
                .switchIfEmpty(Mono.error(
                        new IllegalStateException("session must exist and must be live before saving an attachment")))
                .then();
        return transactionalOperator
                .transactional(sessionGuard.then(databaseClient
                        .sql(sql)
                        .bind("conversation_id", attachment.getConversationId())
                        .bind("file_name", attachment.getFileName())
                        .bind("content_type", attachment.getContentType())
                        .bind("size", attachment.getSize())
                        .bind("file_path", attachment.getFilePath())
                        .bind("tenant_id", attachment.getTenantId())
                        .bind("user_id", attachment.getUserId())
                        .bind("creator_id", attachment.getCreatorId())
                        .bind("creator_name", attachment.getCreatorName())
                        .bind("operator_id", attachment.getOperatorId())
                        .bind("operator_name", attachment.getOperatorName())
                        .bind("create_time", dialect.bindInstant(now))
                        .bind("operate_time", dialect.bindInstant(now))
                        .fetch()
                        .rowsUpdated()
                        .flatMap(rows -> rows == 1
                                ? getByPath(attachment.getFilePath(), attachment.getTenantId(), attachment.getUserId())
                                : Mono.error(
                                        new IllegalStateException("attachment insert affected " + rows + " rows")))))
                .switchIfEmpty(Mono.error(new IllegalStateException("attachment insert returned no row")));
    }

    @Override
    public Flux<AttachmentBO> list(String conversationId, RequestHeader.PrincipalHeader header) {
        if (header == null) return Flux.error(new IllegalArgumentException("header must not be null"));
        if (conversationId == null || conversationId.isBlank()) return Flux.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id AND user_id = :user_id"
                        + " AND deleted = 0 ORDER BY create_time DESC, id DESC")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map(this::map)
                .all();
    }

    @Override
    public Flux<String> listDeletedFilePaths(String conversationId, RequestHeader.PrincipalHeader header) {
        if (header == null) return Flux.error(new IllegalArgumentException("header must not be null"));
        if (conversationId == null || conversationId.isBlank()) return Flux.empty();
        return databaseClient
                .sql("SELECT file_path FROM " + TABLE
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 1")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map((row, metadata) -> row.get("file_path", String.class))
                .all();
    }

    @Override
    public Flux<AttachmentBO> getByIds(Collection<Long> ids, RequestHeader.PrincipalHeader header) {
        if (header == null) return Flux.error(new IllegalArgumentException("header must not be null"));
        if (ids == null || ids.isEmpty()) return Flux.empty();
        String markers = java.util.stream.IntStream.range(0, ids.size())
                .mapToObj(index -> ":id" + index)
                .collect(Collectors.joining(","));
        DatabaseClient.GenericExecuteSpec statement = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE id IN (" + markers + ") AND tenant_id = :tenant_id AND user_id = :user_id"
                        + " AND deleted = 0 ORDER BY create_time DESC, id DESC")
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId());
        int index = 0;
        for (Long id : ids) statement = statement.bind("id" + index++, id);
        return statement.map(this::map).all();
    }

    @Override
    public Mono<AttachmentBO> getById(Long id, RequestHeader.PrincipalHeader header) {
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        if (id == null) return Mono.error(new IllegalArgumentException("id must not be null"));
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE id = :id AND tenant_id = :tenant_id AND user_id = :user_id AND deleted = 0"
                        + " LIMIT 1")
                .bind("id", id)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map(this::map)
                .one();
    }

    @Override
    public Mono<Long> delete(Long id, RequestHeader.PrincipalHeader header) {
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        if (id == null) return Mono.error(new IllegalArgumentException("id must not be null"));
        return transactionalOperator.transactional(databaseClient
                .sql("UPDATE " + TABLE + " SET deleted = 1, operator_id = :operator_id,"
                        + " operate_time = :operate_time WHERE id = :id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0")
                .bind("operator_id", header.getUserId())
                .bind("operate_time", dialect.bindInstant(Instant.now()))
                .bind("id", id)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf));
    }

    @Override
    public Mono<Long> countByTenant(RequestHeader.PrincipalHeader header) {
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        return databaseClient
                .sql("SELECT COUNT(*) AS total FROM " + TABLE + " WHERE tenant_id = :tenant_id AND deleted = 0")
                .bind("tenant_id", header.getTenantId())
                .map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .first();
    }

    @Override
    public Mono<Long> sumSizeByTenant(RequestHeader.PrincipalHeader header) {
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        return databaseClient
                .sql("SELECT COALESCE(SUM(size), 0) AS total FROM " + TABLE
                        + " WHERE tenant_id = :tenant_id AND deleted = 0")
                .bind("tenant_id", header.getTenantId())
                .map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .first();
    }

    private Mono<AttachmentBO> getByPath(String path, Long tenantId, Long userId) {
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE file_path = :file_path AND tenant_id = :tenant_id AND user_id = :user_id"
                        + " AND deleted = 0 LIMIT 1")
                .bind("file_path", path)
                .bind("tenant_id", tenantId)
                .bind("user_id", userId)
                .map(this::map)
                .one();
    }

    private AttachmentBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        AttachmentBO attachment = new AttachmentBO();
        attachment.setId(row.get("id", Long.class));
        attachment.setConversationId(row.get("conversation_id", String.class));
        attachment.setFileName(row.get("file_name", String.class));
        attachment.setContentType(row.get("content_type", String.class));
        attachment.setSize(row.get("size", Long.class));
        attachment.setFilePath(row.get("file_path", String.class));
        attachment.setTenantId(row.get("tenant_id", Long.class));
        attachment.setUserId(row.get("user_id", Long.class));
        attachment.setRemark(row.get("remark", String.class));
        attachment.setCreatorId(row.get("creator_id", Long.class));
        attachment.setCreatorName(row.get("creator_name", String.class));
        attachment.setCreateTime(DatabaseInstant.toLocalDateTimeUtc(row.get("create_time")));
        attachment.setOperatorId(row.get("operator_id", Long.class));
        attachment.setOperatorName(row.get("operator_name", String.class));
        attachment.setOperateTime(DatabaseInstant.toLocalDateTimeUtc(row.get("operate_time")));
        return attachment;
    }
}
