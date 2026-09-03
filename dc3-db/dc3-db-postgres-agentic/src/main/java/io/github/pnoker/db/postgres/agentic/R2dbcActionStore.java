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

import io.github.pnoker.common.agentic.repository.ReactiveActionStore;

import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Explicit SQL adapter for agentic actions. */
@Repository
@ConditionalOnClass({DatabaseClient.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcActionStore implements ReactiveActionStore {

    private static final String TABLE = "dc3_agentic.dc3_action";
    private static final String COLUMNS = "id, action_id, conversation_id, action_type, title, description, payload, "
            + "status, expire_time, tenant_id, user_id, remark, creator_id, creator_name, create_time, "
            + "operator_id, operator_name, operate_time";
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final DatabaseClient databaseClient;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<ActionBO> create(ActionBO action) {
        if (action == null) {
            return Mono.error(new IllegalArgumentException("action must not be null"));
        }
        if (action.getActionId() == null || action.getActionId().isBlank()) {
            return Mono.error(new IllegalArgumentException("actionId must not be blank"));
        }
        if (action.getTenantId() == null || action.getUserId() == null) {
            return Mono.error(new IllegalArgumentException("tenantId and userId must not be null"));
        }
        if (action.getId() == null) {
            action.setId(UuidV7.nextLong());
        }
        String sql = "INSERT INTO " + TABLE
                + " (id, action_id, conversation_id, action_type, title, description, payload, status, expire_time, "
                + "tenant_id, user_id, remark, creator_id, creator_name, create_time, operator_id, operator_name, operate_time, deleted) "
                + "VALUES (:id, :action_id, :conversation_id, :action_type, :title, :description, "
                + dialect.jsonWriteExpression(":payload") + ", "
                + ":status, :expire_time, :tenant_id, :user_id, :remark, :creator_id, :creator_name, :create_time, "
                + ":operator_id, :operator_name, :operate_time, 0)";
        DatabaseClient.GenericExecuteSpec statement = databaseClient
                .sql(sql)
                .bind("id", action.getId())
                .bind("action_id", action.getActionId())
                .bind("conversation_id", valueOrEmpty(action.getConversationId()))
                .bind("action_type", valueOrEmpty(action.getActionType()))
                .bind("title", valueOrEmpty(action.getTitle()))
                .bind("description", valueOrEmpty(action.getDescription()))
                .bind("payload", serialize(action.getPayload()))
                .bind("status", statusIndex(action.getStatus()))
                .bind("tenant_id", action.getTenantId())
                .bind("user_id", action.getUserId())
                .bind("remark", valueOrEmpty(action.getRemark()))
                .bind("creator_id", valueOrZero(action.getCreatorId(), action.getUserId()))
                .bind("creator_name", valueOrEmpty(action.getCreatorName()))
                .bind("create_time", utc(action.getCreateTime(), Instant.now()))
                .bind("operator_id", valueOrZero(action.getOperatorId(), action.getUserId()))
                .bind("operator_name", valueOrEmpty(action.getOperatorName()))
                .bind("operate_time", utc(action.getOperateTime(), Instant.now()));
        if (action.getExpireTime() == null) {
            statement = statement.bindNull("expire_time", LocalDateTime.class);
        } else {
            statement = statement.bind("expire_time", action.getExpireTime());
        }
        return statement
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? find(action.getActionId(), principal(action))
                        : Mono.error(new IllegalStateException("action insert affected " + rows + " rows")));
    }

    @Override
    public Mono<ActionBO> find(String actionId, RequestHeader.PrincipalHeader header) {
        if (actionId == null || actionId.isBlank()) {
            return Mono.error(new IllegalArgumentException("actionId must not be blank"));
        }
        validateHeader(header);
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE action_id = :action_id AND tenant_id = :tenant_id AND user_id = :user_id"
                        + " AND deleted = 0")
                .bind("action_id", actionId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<ActionBO>> listPending(
            long offset, int limit, String conversationId, RequestHeader.PrincipalHeader header, Instant now) {
        validateHeader(header);
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.just(OffsetPage.of(java.util.List.of(), offset, limit, 0));
        }
        new PageRequest(offset, limit);
        LocalDateTime current = utc(now, Instant.now());
        String predicate = " FROM " + TABLE
                + " WHERE conversation_id = :conversation_id AND tenant_id = :tenant_id AND user_id = :user_id"
                + " AND status = :status AND deleted = 0"
                + " AND (expire_time IS NULL OR expire_time >= :now)";
        DatabaseClient.GenericExecuteSpec itemStatement = databaseClient
                .sql("SELECT " + COLUMNS + predicate
                        + " ORDER BY create_time DESC, id DESC LIMIT :limit OFFSET :offset")
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .bind("status", AgenticActionStatusEnum.PENDING.getIndex())
                .bind("now", current)
                .bind("limit", limit)
                .bind("offset", offset);
        DatabaseClient.GenericExecuteSpec countStatement = databaseClient
                .sql("SELECT COUNT(*) AS total" + predicate)
                .bind("conversation_id", conversationId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .bind("status", AgenticActionStatusEnum.PENDING.getIndex())
                .bind("now", current);
        Mono<Long> total = countStatement
                .map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .one();
        Mono<java.util.List<ActionBO>> items =
                itemStatement.map(this::map).all().collectList();
        return total.flatMap(totalCount -> items.map(pageItems -> OffsetPage.of(pageItems, offset, limit, totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<ActionBO> claimPending(
            String actionId, RequestHeader.PrincipalHeader header, AgenticActionStatusEnum nextStatus, Instant now) {
        validateHeader(header);
        if (nextStatus == null || nextStatus == AgenticActionStatusEnum.PENDING) {
            return Mono.error(new IllegalArgumentException("nextStatus must be a terminal action status"));
        }
        LocalDateTime current = utc(now, Instant.now());
        return databaseClient
                .sql("UPDATE " + TABLE + " SET status = :next_status, operator_id = :operator_id, "
                        + "operator_name = :operator_name, operate_time = :operate_time"
                        + " WHERE action_id = :action_id AND tenant_id = :tenant_id AND user_id = :user_id"
                        + " AND status = :pending_status AND deleted = 0"
                        + " AND (expire_time IS NULL OR expire_time >= :now)")
                .bind("next_status", nextStatus.getIndex())
                .bind("operator_id", header.getUserId())
                .bind("operator_name", valueOrEmpty(header.getUserName()))
                .bind("operate_time", current)
                .bind("action_id", actionId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .bind("pending_status", AgenticActionStatusEnum.PENDING.getIndex())
                .bind("now", current)
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? find(actionId, header) : Mono.empty());
    }

    @Override
    public Mono<ActionBO> updateExecutionResult(
            String actionId,
            RequestHeader.PrincipalHeader header,
            AgenticActionStatusEnum status,
            String remark,
            Instant now) {
        validateHeader(header);
        if (status != AgenticActionStatusEnum.EXECUTED && status != AgenticActionStatusEnum.FAILED) {
            return Mono.error(new IllegalArgumentException("execution result must be EXECUTED or FAILED"));
        }
        return databaseClient
                .sql("UPDATE " + TABLE + " SET status = :status, remark = :remark, operator_id = :operator_id, "
                        + "operator_name = :operator_name, operate_time = :operate_time"
                        + " WHERE action_id = :action_id AND tenant_id = :tenant_id AND user_id = :user_id"
                        + " AND status = :confirmed_status AND deleted = 0")
                .bind("status", status.getIndex())
                .bind("remark", valueOrEmpty(remark))
                .bind("operator_id", header.getUserId())
                .bind("operator_name", valueOrEmpty(header.getUserName()))
                .bind("operate_time", utc(now, Instant.now()))
                .bind("action_id", actionId)
                .bind("tenant_id", header.getTenantId())
                .bind("user_id", header.getUserId())
                .bind("confirmed_status", AgenticActionStatusEnum.CONFIRMED.getIndex())
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? find(actionId, header) : Mono.empty());
    }

    private ActionBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        ActionBO action = new ActionBO();
        action.setId(row.get("id", Long.class));
        action.setActionId(row.get("action_id", String.class));
        action.setConversationId(row.get("conversation_id", String.class));
        action.setActionType(row.get("action_type", String.class));
        action.setTitle(row.get("title", String.class));
        action.setDescription(row.get("description", String.class));
        String payload = row.get("payload", String.class);
        action.setPayload(payload == null ? Map.of() : deserialize(payload));
        Number status = row.get("status", Number.class);
        action.setStatus(AgenticActionStatusEnum.ofIndex(status == null ? null : status.byteValue()));
        action.setExpireTime(toLocalDateTime(row.get("expire_time")));
        action.setTenantId(row.get("tenant_id", Long.class));
        action.setUserId(row.get("user_id", Long.class));
        action.setRemark(row.get("remark", String.class));
        action.setCreatorId(row.get("creator_id", Long.class));
        action.setCreatorName(row.get("creator_name", String.class));
        action.setCreateTime(toLocalDateTime(row.get("create_time")));
        action.setOperatorId(row.get("operator_id", Long.class));
        action.setOperatorName(row.get("operator_name", String.class));
        action.setOperateTime(toLocalDateTime(row.get("operate_time")));
        return action;
    }

    private Map<String, Object> deserialize(String value) {
        try {
            return objectMapper.readValue(value, PAYLOAD_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("invalid dc3_action payload JSON", exception);
        }
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("action payload is not valid JSON", exception);
        }
    }

    private RequestHeader.PrincipalHeader principal(ActionBO action) {
        RequestHeader.PrincipalHeader header = new RequestHeader.PrincipalHeader();
        header.setTenantId(action.getTenantId());
        header.setPrincipalId(action.getUserId());
        header.setPrincipalName(action.getOperatorName());
        return header;
    }

    private void validateHeader(RequestHeader.PrincipalHeader header) {
        if (header == null || header.getTenantId() == null || header.getUserId() == null) {
            throw new IllegalArgumentException("tenant and user are required");
        }
    }

    private Byte statusIndex(AgenticActionStatusEnum status) {
        return status == null ? AgenticActionStatusEnum.PENDING.getIndex() : status.getIndex();
    }

    private long valueOrZero(Long value, Long fallback) {
        return value == null ? fallback : value;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private LocalDateTime utc(LocalDateTime value, Instant fallback) {
        return value == null ? utc(fallback, Instant.now()) : value;
    }

    private LocalDateTime utc(Instant value, Instant fallback) {
        return LocalDateTime.ofInstant(value == null ? fallback : value, ZoneOffset.UTC);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return null;
    }
}
