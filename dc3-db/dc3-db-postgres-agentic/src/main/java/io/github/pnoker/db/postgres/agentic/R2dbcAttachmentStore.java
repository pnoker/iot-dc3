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
package io.github.pnoker.db.postgres.agentic;

import io.github.pnoker.common.agentic.repository.ReactiveAttachmentStore;

import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

/** Explicit SQL adapter for attachment metadata. */
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

    @Override
    public Mono<AttachmentBO> save(AttachmentBO attachment) {
        Objects.requireNonNull(attachment, "attachment must not be null");
        String sql = "INSERT INTO " + TABLE
                + " (conversation_id, file_name, content_type, size, file_path, tenant_id, user_id,"
                + " creator_id, creator_name, operator_id, operator_name, create_time, operate_time, deleted)"
                + " VALUES (:conversation_id, :file_name, :content_type, :size, :file_path, :tenant_id, :user_id,"
                + " :creator_id, :creator_name, :operator_id, :operator_name, :create_time, :operate_time, 0)";
        LocalDateTime now = utcNow();
        return transactionalOperator
                .transactional(databaseClient
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
                        .bind("create_time", now)
                        .bind("operate_time", now)
                        .fetch()
                        .rowsUpdated()
                        .flatMap(rows -> rows == 1
                                ? findByPath(attachment.getFilePath(), attachment.getTenantId(), attachment.getUserId())
                                : Mono.error(
                                        new IllegalStateException("attachment insert affected " + rows + " rows"))))
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
    public Flux<AttachmentBO> findByIds(Collection<Long> ids, RequestHeader.PrincipalHeader header) {
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

    private Mono<AttachmentBO> findByPath(String path, Long tenantId, Long userId) {
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
        attachment.setCreateTime(toLocalDateTime(row.get("create_time")));
        attachment.setOperatorId(row.get("operator_id", Long.class));
        attachment.setOperatorName(row.get("operator_name", String.class));
        attachment.setOperateTime(toLocalDateTime(row.get("operate_time")));
        return attachment;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return null;
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    }
}
