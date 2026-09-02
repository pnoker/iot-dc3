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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.entity.ext.EventExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/** Explicit SQL adapter for manager events. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcEventStore implements ReactiveEventStore {
    private static final String TABLE = "dc3_manager.dc3_event";
    private static final String COLUMNS =
            "id, event_name, event_code, event_type_flag, event_level_flag, event_ext, profile_id, enable_flag, tenant_id, remark, signature, version, creator_id, creator_name, create_time, operator_id, operator_name, operate_time";
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<EventBO> get(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<Boolean> existsByNameOrCode(
            Long tenantId, Long profileId, String eventName, String eventCode, Long excludingId) {
        if (tenantId == null || profileId == null || (eventName == null && eventCode == null)) return Mono.just(false);
        List<String> predicates = new ArrayList<>();
        if (eventName != null && !eventName.isBlank()) predicates.add("event_name=:event_name");
        if (eventCode != null && !eventCode.isBlank()) predicates.add("event_code=:event_code");
        if (predicates.isEmpty()) return Mono.just(false);
        String sql =
                "SELECT 1 FROM " + TABLE + " WHERE tenant_id=:tenant_id AND profile_id=:profile_id AND deleted=0 AND ("
                        + String.join(" OR ", predicates) + ")" + (excludingId == null ? "" : " AND id<>:excluding_id")
                        + " LIMIT 1";
        DatabaseClient.GenericExecuteSpec query =
                databaseClient.sql(sql).bind("tenant_id", tenantId).bind("profile_id", profileId);
        if (eventName != null && !eventName.isBlank()) query = query.bind("event_name", eventName);
        if (eventCode != null && !eventCode.isBlank()) query = query.bind("event_code", eventCode);
        if (excludingId != null) query = query.bind("excluding_id", excludingId);
        return query.map((row, metadata) -> true).one().defaultIfEmpty(false);
    }

    @Override
    public Mono<EventBO> insert(EventBO value) {
        if (value == null || value.getTenantId() == null || value.getProfileId() == null)
            return Mono.error(new IllegalArgumentException("tenantId and profileId are required"));
        if (value.getId() == null) value.setId(UuidV7.nextLong());
        if (value.getVersion() == null) value.setVersion(0);
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(
                        "INSERT INTO " + TABLE
                                + " (id,event_name,event_code,event_type_flag,event_level_flag,event_ext,profile_id,enable_flag,tenant_id,remark,signature,version,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted)"
                                + " VALUES (:id,:event_name,:event_code,:event_type,:event_level,"
                                + dialect.jsonWriteExpression(":event_ext")
                                + ",:profile_id,:enable_flag,:tenant_id,:remark,:signature,:version,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", value.getId())
                .bind("event_name", value.getEventName())
                .bind("event_code", value.getEventCode())
                .bind("event_type", index(value.getEventTypeFlag()))
                .bind("event_level", index(value.getEventLevelFlag()))
                .bind("event_ext", serialize(value.getEventExt()))
                .bind("profile_id", value.getProfileId())
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("signature", value.getSignature() == null ? "" : value.getSignature())
                .bind("version", value.getVersion())
                .bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId())
                .bind("creator_name", value.getCreatorName() == null ? "" : value.getCreatorName())
                .bind(
                        "create_time",
                        value.getCreateTime() == null ? LocalDateTime.now(ZoneOffset.UTC) : value.getCreateTime())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName())
                .bind(
                        "operate_time",
                        value.getOperateTime() == null ? LocalDateTime.now(ZoneOffset.UTC) : value.getOperateTime());
        return transactionalOperator.transactional(query.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? get(value.getTenantId(), value.getId())
                        : Mono.error(new IllegalStateException("event insert affected " + rows + " rows"))));
    }

    @Override
    public Mono<EventBO> update(EventBO value, int expectedVersion) {
        if (value == null || value.getTenantId() == null || value.getId() == null)
            return Mono.error(new IllegalArgumentException("tenantId and event id are required"));
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET event_name=:event_name,event_type_flag=:event_type,event_level_flag=:event_level,event_ext="
                                + dialect.jsonWriteExpression(":event_ext")
                                + ",profile_id=:profile_id,enable_flag=:enable_flag,remark=:remark,signature=:signature,version=version+1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE id=:id AND tenant_id=:tenant_id AND version=:expected_version AND deleted=0")
                .bind("event_name", value.getEventName())
                .bind("event_type", index(value.getEventTypeFlag()))
                .bind("event_level", index(value.getEventLevelFlag()))
                .bind("event_ext", serialize(value.getEventExt()))
                .bind("profile_id", value.getProfileId())
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("signature", value.getSignature() == null ? "" : value.getSignature())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName())
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("id", value.getId())
                .bind("tenant_id", value.getTenantId())
                .bind("expected_version", expectedVersion);
        return transactionalOperator.transactional(query.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND version=:expected_version AND deleted=0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("expected_version", expectedVersion)
                .bind("operator_id", operatorId == null ? 0L : operatorId)
                .bind("operator_name", operatorName == null ? "" : operatorName)
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1));
    }

    @Override
    public Flux<EventBO> listByIds(Long tenantId, List<Long> ids) {
        if (tenantId == null || ids == null || ids.isEmpty()) return Flux.empty();
        List<Long> values =
                ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (values.isEmpty()) return Flux.empty();
        String placeholders = java.util.stream.IntStream.range(0, values.size())
                .mapToObj(i -> ":id" + i)
                .reduce((a, b) -> a + "," + b)
                .orElseThrow();
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND deleted=0 AND id IN ("
                        + placeholders + ") ORDER BY id")
                .bind("tenant_id", tenantId);
        for (int i = 0; i < values.size(); i++) spec = spec.bind("id" + i, values.get(i));
        return spec.map(this::map).all();
    }

    @Override
    public Flux<EventBO> listByProfileId(Long tenantId, Long profileId) {
        if (tenantId == null || profileId == null) return Flux.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND profile_id=:profile_id AND deleted=0 ORDER BY id")
                .bind("tenant_id", tenantId)
                .bind("profile_id", profileId)
                .map(this::map)
                .all();
    }

    @Override
    public Flux<EventBO> listByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Flux.empty();
        return databaseClient
                .sql(
                        "SELECT " + qualifiedColumns("e") + " FROM " + TABLE
                                + " e JOIN dc3_manager.dc3_device d ON d.tenant_id=e.tenant_id AND d.profile_id=e.profile_id WHERE d.tenant_id=:tenant_id AND d.id=:device_id AND d.deleted=0 AND e.deleted=0 ORDER BY e.id")
                .bind("tenant_id", tenantId)
                .bind("device_id", deviceId)
                .map(this::map)
                .all();
    }

    @Override
    public Mono<OffsetPage<EventBO>> list(EventFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("filter is required"));
        StringBuilder where = new StringBuilder(" WHERE e.tenant_id=:tenant_id AND e.deleted=0");
        if (present(filter.eventName())) where.append(" AND e.event_name LIKE :event_name");
        if (present(filter.eventCode())) where.append(" AND e.event_code=:event_code");
        if (filter.eventTypeFlag() != null) where.append(" AND e.event_type_flag=:event_type");
        if (filter.eventLevelFlag() != null) where.append(" AND e.event_level_flag=:event_level");
        if (filter.profileId() != null) where.append(" AND e.profile_id=:profile_id");
        if (filter.enableFlag() != null) where.append(" AND e.enable_flag=:enable_flag");
        if (filter.version() != null) where.append(" AND e.version=:version");
        if (filter.deviceId() != null)
            where.append(
                    " AND EXISTS (SELECT 1 FROM dc3_manager.dc3_device d WHERE d.tenant_id=e.tenant_id AND d.id=:device_id AND d.profile_id=e.profile_id AND d.deleted=0)");
        String predicate = where.toString();
        DatabaseClient.GenericExecuteSpec count =
                bind(databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + " e" + predicate), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(
                        databaseClient.sql("SELECT " + qualifiedColumns("e") + " FROM " + TABLE + " e" + predicate
                                + " ORDER BY " + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset"),
                        filter)
                .bind("limit", filter.limit())
                .bind("offset", filter.offset());
        Mono<Long> total = count.map((row, metadata) -> {
                    Number n = row.get("total", Number.class);
                    return n == null ? 0L : n.longValue();
                })
                .one()
                .defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, filter.offset(), filter.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, EventFilter filter) {
        spec = spec.bind("tenant_id", filter.tenantId());
        if (present(filter.eventName())) spec = spec.bind("event_name", "%" + filter.eventName() + "%");
        if (present(filter.eventCode())) spec = spec.bind("event_code", filter.eventCode());
        if (filter.eventTypeFlag() != null)
            spec = spec.bind("event_type", filter.eventTypeFlag().getIndex());
        if (filter.eventLevelFlag() != null)
            spec = spec.bind("event_level", filter.eventLevelFlag().getIndex());
        if (filter.profileId() != null) spec = spec.bind("profile_id", filter.profileId());
        if (filter.enableFlag() != null)
            spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        if (filter.version() != null) spec = spec.bind("version", filter.version());
        if (filter.deviceId() != null) spec = spec.bind("device_id", filter.deviceId());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "e.id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String col =
                    switch (spec.field()) {
                        case "id" -> "e.id";
                        case "eventName" -> "e.event_name";
                        case "eventCode" -> "e.event_code";
                        case "createTime" -> "e.create_time";
                        case "operateTime" -> "e.operate_time";
                        case "version" -> "e.version";
                        default -> throw new IllegalArgumentException("unsupported event sort field: " + spec.field());
                    };
            clauses.add(col + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(v -> v.startsWith("e.id "))) clauses.add("e.id ASC");
        return String.join(", ", clauses);
    }

    private String qualifiedColumns(String alias) {
        return java.util.Arrays.stream(COLUMNS.split(", "))
                .map(c -> alias + "." + c)
                .reduce((a, b) -> a + ", " + b)
                .orElse(COLUMNS);
    }

    private EventBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        EventBO v = new EventBO();
        v.setId(row.get("id", Long.class));
        v.setEventName(row.get("event_name", String.class));
        v.setEventCode(row.get("event_code", String.class));
        v.setProfileId(row.get("profile_id", Long.class));
        v.setTenantId(row.get("tenant_id", Long.class));
        v.setRemark(row.get("remark", String.class));
        v.setSignature(row.get("signature", String.class));
        v.setVersion(row.get("version", Integer.class));
        v.setCreatorId(row.get("creator_id", Long.class));
        v.setCreatorName(row.get("creator_name", String.class));
        v.setCreateTime(time(row.get("create_time")));
        v.setOperatorId(row.get("operator_id", Long.class));
        v.setOperatorName(row.get("operator_name", String.class));
        v.setOperateTime(time(row.get("operate_time")));
        Number type = row.get("event_type_flag", Number.class),
                level = row.get("event_level_flag", Number.class),
                enabled = row.get("enable_flag", Number.class);
        v.setEventTypeFlag(EventTypeFlagEnum.ofIndex(type == null ? null : type.byteValue()));
        v.setEventLevelFlag(EventLevelEnum.ofIndex(level == null ? null : level.byteValue()));
        v.setEnableFlag(EnableFlagEnum.ofIndex(enabled == null ? null : enabled.byteValue()));
        String raw = row.get("event_ext", String.class);
        if (raw != null)
            try {
                JsonExt json = objectMapper.readValue(raw, JsonExt.class);
                EventExt ext = new EventExt();
                ext.setType(json.getType());
                ext.setVersion(json.getVersion());
                ext.setRemark(json.getRemark());
                ext.setContent(
                        json.getContent() == null
                                ? null
                                : JsonUtil.parseObject(json.getContent(), EventExt.Content.class));
                v.setEventExt(ext);
            } catch (Exception e) {
                throw new IllegalStateException("event_ext contains invalid JSON", e);
            }
        return v;
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private Byte index(EventTypeFlagEnum value) {
        return value == null ? EventTypeFlagEnum.INFO.getIndex() : value.getIndex();
    }

    private Byte index(EventLevelEnum value) {
        return value == null ? EventLevelEnum.LOW.getIndex() : value.getIndex();
    }

    private Byte index(EnableFlagEnum value) {
        return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex();
    }

    private String serialize(EventExt value) {
        try {
            JsonExt json = new JsonExt();
            if (value != null) {
                json.setType(value.getType());
                json.setVersion(value.getVersion());
                json.setRemark(value.getRemark());
                json.setContent(value.getContent() == null ? null : JsonUtil.toJsonString(value.getContent()));
            }
            return objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("event_ext is not valid JSON", e);
        }
    }
}
