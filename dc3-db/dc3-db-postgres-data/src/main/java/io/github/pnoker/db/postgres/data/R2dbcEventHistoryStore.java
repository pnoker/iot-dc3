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
package io.github.pnoker.db.postgres.data;

import io.github.pnoker.common.data.repository.ReactiveEventHistoryStore;

import io.github.pnoker.common.data.entity.model.EventHistoryDO;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
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
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for {@code dc3_data.dc3_event_history}. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcEventHistoryStore implements ReactiveEventHistoryStore {

    private static final String TABLE = "dc3_data.dc3_event_history";
    private static final String COLUMNS =
            "id,record_id,tenant_id,device_id,event_id,event_code,event_type_flag,event_level_flag,"
                    + "param_values,config_snapshot,message,occur_time,receive_time,acknowledge_flag,acknowledge_time,"
                    + "acknowledge_user_id,schema_version,create_time,operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<EventHistoryDO> insert(EventHistoryDO event) {
        if (event == null
                || event.getTenantId() == null
                || event.getTenantId() <= 0
                || event.getRecordId() == null
                || event.getRecordId().isBlank()) {
            return Mono.error(new IllegalArgumentException("tenantId and recordId are required"));
        }
        if (event.getId() == null) event.setId(UuidV7.nextLong());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (event.getReceiveTime() == null) event.setReceiveTime(now);
        if (event.getCreateTime() == null) event.setCreateTime(now);
        if (event.getOperateTime() == null) event.setOperateTime(now);
        String sql = "INSERT INTO " + TABLE + " (" + COLUMNS
                + ") VALUES (:id,:record_id,:tenant_id,:device_id,:event_id,:event_code,:event_type_flag,:event_level_flag,"
                + dialect.jsonWriteExpression(":param_values") + "," + dialect.jsonWriteExpression(":config_snapshot")
                + ",:message,:occur_time,:receive_time,:acknowledge_flag,:acknowledge_time,:acknowledge_user_id,:schema_version,:create_time,:operate_time)";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("id", event.getId())
                .bind("record_id", event.getRecordId())
                .bind("tenant_id", event.getTenantId())
                .bind("device_id", event.getDeviceId())
                .bind("event_id", event.getEventId())
                .bind("event_code", event.getEventCode())
                .bind("event_type_flag", value(event.getEventTypeFlag()))
                .bind("event_level_flag", value(event.getEventLevelFlag()))
                .bind("message", event.getMessage())
                .bind("occur_time", event.getOccurTime())
                .bind("receive_time", event.getReceiveTime())
                .bind("acknowledge_flag", value(event.getAcknowledgeFlag()))
                .bind("schema_version", event.getSchemaVersion() == null ? (short) 1 : event.getSchemaVersion())
                .bind("create_time", event.getCreateTime())
                .bind("operate_time", event.getOperateTime());
        spec = bindNullable(spec, "param_values", event.getParamValues(), String.class);
        spec = bindNullable(spec, "config_snapshot", event.getConfigSnapshot(), String.class);
        spec = bindNullable(spec, "acknowledge_time", event.getAcknowledgeTime(), LocalDateTime.class);
        spec = bindNullable(spec, "acknowledge_user_id", event.getAcknowledgeUserId(), Long.class);
        return transactionalOperator
                .transactional(spec.fetch().rowsUpdated())
                .flatMap(rows -> rows == 1
                        ? Mono.just(event)
                        : Mono.error(new IllegalStateException("event history insert affected " + rows + " rows")));
    }

    @Override
    public Mono<EventHistoryDO> findByRecordId(Long tenantId, String recordId) {
        if (tenantId == null || tenantId <= 0 || recordId == null || recordId.isBlank()) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND record_id=:record_id LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("record_id", recordId)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<EventHistoryDO>> list(
            Long tenantId,
            Long deviceId,
            Long eventId,
            String eventCode,
            Byte eventTypeFlag,
            long offset,
            int limit,
            List<SortSpec> sort) {
        if (tenantId == null || tenantId <= 0) return Mono.error(new IllegalArgumentException("tenantId is required"));
        if (offset < 0 || limit < 1 || limit > PageRequest.MAX_LIMIT)
            return Mono.error(new IllegalArgumentException("invalid page bounds"));
        StringBuilder predicate = new StringBuilder(" WHERE tenant_id=:tenant_id");
        DatabaseClient.GenericExecuteSpec base = databaseClient.sql("").bind("tenant_id", tenantId);
        if (deviceId != null) {
            predicate.append(" AND device_id=:device_id");
            base = base.bind("device_id", deviceId);
        }
        if (eventId != null) {
            predicate.append(" AND event_id=:event_id");
            base = base.bind("event_id", eventId);
        }
        if (eventCode != null && !eventCode.isBlank()) {
            predicate.append(" AND event_code=:event_code");
            base = base.bind("event_code", eventCode);
        }
        if (eventTypeFlag != null) {
            predicate.append(" AND event_type_flag=:event_type_flag");
            base = base.bind("event_type_flag", eventTypeFlag);
        }
        String order = orderBy(sort);
        Mono<Long> total = databaseClient
                .sql("SELECT COUNT(*) AS total FROM " + TABLE + predicate)
                .bind("tenant_id", tenantId)
                .map((row, metadata) -> row.get("total", Number.class).longValue())
                .one();
        String query = "SELECT " + COLUMNS + " FROM " + TABLE + predicate + " ORDER BY " + order
                + " LIMIT :limit OFFSET :offset";
        DatabaseClient.GenericExecuteSpec rowsSpec = databaseClient
                .sql(query)
                .bind("tenant_id", tenantId)
                .bind("limit", limit)
                .bind("offset", offset);
        if (deviceId != null) rowsSpec = rowsSpec.bind("device_id", deviceId);
        if (eventId != null) rowsSpec = rowsSpec.bind("event_id", eventId);
        if (eventCode != null && !eventCode.isBlank()) rowsSpec = rowsSpec.bind("event_code", eventCode);
        if (eventTypeFlag != null) rowsSpec = rowsSpec.bind("event_type_flag", eventTypeFlag);
        DatabaseClient.GenericExecuteSpec itemRows = rowsSpec;
        return total.flatMap(totalCount -> itemRows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, offset, limit, totalCount)))
                .as(pageTransaction::transactional);
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "occur_time DESC,id DESC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (spec.field()) {
                        case "occurTime" -> "occur_time";
                        case "receiveTime" -> "receive_time";
                        case "createTime" -> "create_time";
                        case "eventTypeFlag" -> "event_type_flag";
                        default -> throw new IllegalArgumentException("unsupported sort field: " + spec.field());
                    };
            clauses.add(column + " " + spec.direction().name());
        }
        clauses.add("id DESC");
        return String.join(",", clauses);
    }

    private EventHistoryDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        EventHistoryDO value = new EventHistoryDO();
        value.setId(row.get("id", Long.class));
        value.setRecordId(row.get("record_id", String.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setDeviceId(row.get("device_id", Long.class));
        value.setEventId(row.get("event_id", Long.class));
        value.setEventCode(row.get("event_code", String.class));
        value.setEventTypeFlag(number(row.get("event_type_flag")));
        value.setEventLevelFlag(number(row.get("event_level_flag")));
        value.setParamValues(text(row.get("param_values", String.class)));
        value.setConfigSnapshot(text(row.get("config_snapshot", String.class)));
        value.setMessage(row.get("message", String.class));
        value.setOccurTime(time(row.get("occur_time")));
        value.setReceiveTime(time(row.get("receive_time")));
        value.setAcknowledgeFlag(number(row.get("acknowledge_flag")));
        value.setAcknowledgeTime(time(row.get("acknowledge_time")));
        value.setAcknowledgeUserId(row.get("acknowledge_user_id", Long.class));
        value.setSchemaVersion(row.get("schema_version", Short.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperateTime(time(row.get("operate_time")));
        return value;
    }

    private Byte number(Object value) {
        return value instanceof Number number ? number.byteValue() : null;
    }

    private String text(String value) {
        return value;
    }

    private int value(Byte value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}
