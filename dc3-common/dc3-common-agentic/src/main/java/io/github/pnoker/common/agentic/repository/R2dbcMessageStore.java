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
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/** Explicit SQL adapter for the agentic message store. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcMessageStore implements ReactiveMessageStore {

    private static final String TABLE = "dc3_agentic.dc3_message";
    private static final String COLUMNS = "id, conversation_id, role, content, model, message_index, status, "
            + "tenant_id, user_id, remark, creator_id, creator_name, create_time, operator_id, operator_name, "
            + "operate_time, deleted";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<MessageBO> save(String conversationId, String role, AgenticMessageContent content,
                                String model, AgenticMessageStatusEnum status,
                                RequestHeader.PrincipalHeader header) {
        if (header == null) {
            return Mono.error(new IllegalArgumentException("header must not be null"));
        }
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.error(new IllegalArgumentException("conversationId must not be blank"));
        }
        if (status == null) {
            return Mono.error(new IllegalArgumentException("status must not be null"));
        }
        AgenticMessageContent safeContent = content == null ? AgenticMessageContent.ofText("") : content;
        String json;
        try {
            json = objectMapper.writeValueAsString(safeContent);
        } catch (Exception exception) {
            return Mono.error(new IllegalArgumentException("message content is not valid JSON", exception));
        }
        String lockSql = "SELECT id FROM dc3_agentic.dc3_session"
                + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                + " AND user_id = :user_id AND deleted = 0 FOR UPDATE";
        Mono<Long> nextIndex = databaseClient.sql("SELECT COALESCE(MAX(message_index), -1) + 1 AS next_index FROM "
                        + TABLE + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map((row, metadata) -> row.get("next_index", Long.class))
                .one();
        return transactionalOperator.transactional(databaseClient.sql(lockSql)
                        .bind("conversation_id", conversationId)
                        .bind("tenant_id", header.getTenantId())
                        .bind("user_id", header.getUserId())
                        .fetch().all()
                        .switchIfEmpty(Mono.error(new IllegalStateException("session must exist before saving a message")))
                        .then()
                        .then(nextIndex)
                        .flatMap(index -> insertWithIndex(conversationId, role, json, model, status, header, index)))
                .switchIfEmpty(Mono.error(new IllegalStateException("message insert returned no row")));
    }

    private Mono<MessageBO> insertWithIndex(String conversationId, String role, String json, String model,
                                             AgenticMessageStatusEnum status,
                                             RequestHeader.PrincipalHeader header, long index) {
        Instant now = Instant.now();
        return databaseClient.sql("INSERT INTO " + TABLE
                        + " (conversation_id, role, content, model, message_index, status, tenant_id, user_id, "
                        + "creator_id, creator_name, operator_id, operator_name, create_time, operate_time, deleted)"
                        + " VALUES (:conversation_id, :role, :content, :model, :message_index, :status, :tenant_id, :user_id,"
                        + " :creator_id, :creator_name, :operator_id, :operator_name, :create_time, :operate_time, 0)")
                .bind("conversation_id", conversationId)
                .bind("role", role == null ? "" : role)
                .bind("content", json)
                .bind("model", model == null ? "" : model)
                .bind("message_index", index)
                .bind("status", status.getIndex())
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .bind("creator_id", header.getUserId())
                .bind("creator_name", header.getUserName() == null ? "" : header.getUserName())
                .bind("operator_id", header.getUserId())
                .bind("operator_name", header.getUserName() == null ? "" : header.getUserName())
                .bind("create_time", LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .bind("operate_time", LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .fetch().rowsUpdated()
                .flatMap(rows -> rows == 1 ? findInserted(conversationId, header, Mono.just(index))
                        : Mono.error(new IllegalStateException("message insert affected " + rows + " rows")));
    }

    private Mono<MessageBO> findInserted(String conversationId, RequestHeader.PrincipalHeader header, Mono<Long> index) {
        return index.flatMap(value -> databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id AND user_id = :user_id"
                        + " AND message_index = :message_index AND deleted = 0 ORDER BY id DESC LIMIT 1")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .bind("message_index", value)
                .map(this::map)
                .one());
    }

    @Override
    public Flux<MessageBO> list(String conversationId, RequestHeader.PrincipalHeader header) {
        if (header == null) return Flux.error(new IllegalArgumentException("header must not be null"));
        if (conversationId == null || conversationId.isBlank()) return Flux.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id AND user_id = :user_id"
                        + " AND deleted = 0 ORDER BY message_index ASC, id ASC")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map(this::map)
                .all();
    }

    @Override
    public Flux<MessageBO> loadHistory(String conversationId, RequestHeader.PrincipalHeader header, int limit) {
        if (header == null) return Flux.error(new IllegalArgumentException("header must not be null"));
        if (conversationId == null || conversationId.isBlank()) {
            return Flux.error(new IllegalArgumentException("conversationId must not be blank"));
        }
        if (limit < 1 || limit > 200) {
            return Flux.error(new IllegalArgumentException("limit must be between 1 and 200"));
        }
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0"
                        + " ORDER BY message_index DESC, id DESC LIMIT :limit")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .bind("limit", limit)
                .map(this::map)
                .all()
                .collectList()
                .flatMapMany(messages -> Flux.fromIterable(messages.reversed()));
    }

    @Override
    public Mono<Long> deleteByConversationId(String conversationId, RequestHeader.PrincipalHeader header) {
        if (header == null) return Mono.error(new IllegalArgumentException("header must not be null"));
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.just(0L);
        }
        return databaseClient.sql("UPDATE " + TABLE + " SET deleted = 1, operate_time = :operate_time"
                        + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id"
                        + " AND user_id = :user_id AND deleted = 0")
                .bind("operate_time", LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .fetch().rowsUpdated()
                .map(Long::valueOf);
    }

    private MessageBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        MessageBO message = new MessageBO();
        message.setId(row.get("id", Long.class));
        message.setConversationId(row.get("conversation_id", String.class));
        message.setRole(row.get("role", String.class));
        String contentValue = row.get("content", String.class);
        if (contentValue != null) {
            try {
                message.setContent(objectMapper.readValue(contentValue, AgenticMessageContent.class));
            } catch (Exception exception) {
                throw new IllegalStateException("invalid dc3_message content", exception);
            }
        }
        message.setModel(row.get("model", String.class));
        message.setMessageIndex(row.get("message_index", Long.class));
        Number status = row.get("status", Number.class);
        message.setStatus(AgenticMessageStatusEnum.ofIndex(status == null ? null : status.byteValue()));
        message.setTenantId(row.get("tenant_id", Long.class));
        message.setUserId(row.get("user_id", Long.class));
        message.setRemark(row.get("remark", String.class));
        message.setCreatorId(row.get("creator_id", Long.class));
        message.setCreatorName(row.get("creator_name", String.class));
        message.setCreateTime(toLocalDateTime(row.get("create_time")));
        message.setOperatorId(row.get("operator_id", Long.class));
        message.setOperatorName(row.get("operator_name", String.class));
        message.setOperateTime(toLocalDateTime(row.get("operate_time")));
        return message;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return null;
    }
}
