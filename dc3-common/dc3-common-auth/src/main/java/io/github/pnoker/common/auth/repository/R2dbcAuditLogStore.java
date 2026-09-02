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
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.IdentityAuditLogBO;
import io.github.pnoker.common.auth.entity.model.IdentityAuditLogDO;
import io.github.pnoker.common.auth.support.IdentityAuditCursorCodec;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for append-only identity audit events. */
@Repository
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcAuditLogStore implements ReactiveAuditLogStore, ReactiveAuditLogQueryStore {

    private static final String TABLE = "dc3_auth.dc3_identity_audit_log";

    private final DatabaseClient databaseClient;
    private final R2dbcDialect dialect;
    private final IdentityAuditCursorCodec cursorCodec;

    @Override
    public Mono<Void> append(IdentityAuditLogBO event) {
        if (event == null || !valid(event.getTenantId())) {
            return Mono.error(new IllegalArgumentException("audit tenantId is required"));
        }
        long id = event.getId() == null ? uuidV7Long() : event.getId();
        LocalDateTime createTime =
                event.getCreateTime() == null ? LocalDateTime.now(ZoneOffset.UTC) : event.getCreateTime();
        DatabaseClient.GenericExecuteSpec statement = databaseClient
                .sql("INSERT INTO " + TABLE
                        + " (id,tenant_id,principal_id,principal_type,action,resource_type,resource_id,resource_name,status,error_code,detail_ext,create_time,deleted)"
                        + " VALUES (:id,:tenant_id,:principal_id,:principal_type,:action,:resource_type,:resource_id,:resource_name,:status,:error_code,"
                        + dialect.jsonWriteExpression(":detail_ext") + ",:create_time,0)")
                .bind("id", id)
                .bind("tenant_id", event.getTenantId())
                .bind("principal_id", value(event.getPrincipalId()))
                .bind("principal_type", text(event.getPrincipalType(), "USER"))
                .bind("action", text(event.getAction(), ""))
                .bind("resource_type", text(event.getResourceType(), ""))
                .bind("resource_id", value(event.getResourceId()))
                .bind("resource_name", text(event.getResourceName(), ""))
                .bind("status", text(event.getStatus(), ""))
                .bind("error_code", text(event.getErrorCode(), ""))
                .bind("detail_ext", json(event.getDetailExt()))
                .bind("create_time", dialect.bindInstant(createTime.toInstant(ZoneOffset.UTC)));
        return statement
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? Mono.empty()
                        : Mono.error(new IllegalStateException("audit insert affected " + rows + " rows")));
    }

    @Override
    public Mono<CursorPage<IdentityAuditLogDO>> list(IdentityAuditLogFilter filter) {
        if (filter == null) {
            return Mono.error(new IllegalArgumentException("audit filter is required"));
        }
        int limit = filter.limit();
        StringBuilder predicate = new StringBuilder(" WHERE tenant_id=:tenant_id AND deleted=0");
        if (filter.principalId() != null) predicate.append(" AND principal_id=:principal_id");
        if (filter.action() != null && !filter.action().isBlank()) predicate.append(" AND action=:action");
        if (filter.resourceType() != null && !filter.resourceType().isBlank())
            predicate.append(" AND resource_type=:resource_type");
        if (filter.resourceId() != null) predicate.append(" AND resource_id=:resource_id");
        if (filter.status() != null && !filter.status().isBlank()) predicate.append(" AND status=:status");
        String fingerprint = fingerprint(filter);
        IdentityAuditCursorCodec.Position position =
                filter.cursor() == null || filter.cursor().isBlank()
                        ? null
                        : cursorCodec.decode(filter.cursor(), filter.tenantId(), fingerprint);
        if (position != null) predicate.append(" AND (create_time,id)<(:cursor_time,:cursor_id)");
        DatabaseClient.GenericExecuteSpec statement = databaseClient
                .sql(
                        "SELECT id,tenant_id,principal_id,principal_type,action,resource_type,resource_id,resource_name,status,error_code,detail_ext,create_time,deleted FROM "
                                + TABLE + predicate + " ORDER BY create_time DESC,id DESC LIMIT :limit")
                .bind("tenant_id", filter.tenantId())
                .bind("limit", limit + 1);
        if (filter.principalId() != null) statement = statement.bind("principal_id", filter.principalId());
        if (filter.action() != null && !filter.action().isBlank())
            statement = statement.bind("action", filter.action().trim());
        if (filter.resourceType() != null && !filter.resourceType().isBlank())
            statement = statement.bind("resource_type", filter.resourceType().trim());
        if (filter.resourceId() != null) statement = statement.bind("resource_id", filter.resourceId());
        if (filter.status() != null && !filter.status().isBlank())
            statement = statement.bind("status", filter.status().trim());
        if (position != null)
            statement = statement
                    .bind("cursor_time", dialect.bindInstant(position.time()))
                    .bind("cursor_id", position.id());
        return statement.map(this::map).all().collectList().map(rows -> {
            boolean hasNext = rows.size() > limit;
            List<IdentityAuditLogDO> items = hasNext ? rows.subList(0, limit) : rows;
            String next = hasNext
                    ? cursorCodec.encode(
                            filter.tenantId(),
                            fingerprint,
                            items.get(items.size() - 1).getCreateTime().toInstant(ZoneOffset.UTC),
                            items.get(items.size() - 1).getId())
                    : null;
            return new CursorPage<>(items, next, hasNext);
        });
    }

    private long uuidV7Long() {
        return UuidV7.nextLong();
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private String text(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private String json(JsonExt value) {
        return value == null ? "{}" : JsonUtil.toJsonString(value);
    }

    private IdentityAuditLogDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        IdentityAuditLogDO value = new IdentityAuditLogDO();
        value.setId(row.get("id", Long.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setPrincipalType(row.get("principal_type", String.class));
        value.setAction(row.get("action", String.class));
        value.setResourceType(row.get("resource_type", String.class));
        value.setResourceId(row.get("resource_id", Long.class));
        value.setResourceName(row.get("resource_name", String.class));
        value.setStatus(row.get("status", String.class));
        value.setErrorCode(row.get("error_code", String.class));
        value.setDetailExt(parseJson(row.get("detail_ext", String.class)));
        value.setCreateTime(time(row.get("create_time")));
        Number deleted = row.get("deleted", Number.class);
        value.setDeleted(deleted == null ? null : deleted.byteValue());
        return value;
    }

    private LocalDateTime time(Object raw) {
        if (raw instanceof LocalDateTime value) return value;
        if (raw instanceof java.time.OffsetDateTime value)
            return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (raw instanceof Instant value) return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        return null;
    }

    private JsonExt parseJson(String raw) {
        if (raw == null) return null;
        try {
            return JsonUtil.parseObject(raw, JsonExt.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String fingerprint(IdentityAuditLogFilter filter) {
        return "tenant=" + filter.tenantId()
                + ";principal=" + value(filter.principalId())
                + ";action=" + normalized(filter.action())
                + ";resourceType=" + normalized(filter.resourceType())
                + ";resourceId=" + value(filter.resourceId())
                + ";status=" + normalized(filter.status())
                + ";sort=create_time.desc,id.desc";
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}
