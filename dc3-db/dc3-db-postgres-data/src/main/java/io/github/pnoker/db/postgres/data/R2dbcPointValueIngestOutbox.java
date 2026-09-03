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

import io.github.pnoker.common.data.repository.ReactivePointValueIngestOutbox;

import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC adapter for the durable point-value ingest receipt. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcPointValueIngestOutbox implements ReactivePointValueIngestOutbox {

    private static final String COLUMNS = "tenant_id,message_id,schema_version,driver_node,sequence,fencing_token,"
            + "device_id,point_id,raw_value,cal_value,num_value,driver_id,create_time,operate_time";

    private final DatabaseClient databaseClient;
    private final R2dbcDialect dialect;
    private final TransactionalOperator transactionalOperator;

    private String table() {
        return dialect.operationTable().replace("dc3_operation", "dc3_point_value_ingest_outbox");
    }

    @Override
    public Mono<List<PointValueDO>> enqueue(List<PointValueDO> values, String owner) {
        List<PointValueDO> rows = values == null
                ? List.of()
                : values.stream().filter(Objects::nonNull).toList();
        if (rows.isEmpty() || owner == null || owner.isBlank()) return Mono.just(List.of());
        return transactionalOperator.transactional(Flux.fromIterable(rows)
                .concatMap(value ->
                        insert(value, owner).filter(Boolean::booleanValue).map(ignored -> value))
                .collectList());
    }

    @Override
    public Flux<PointValueDO> findPersisted(List<PointValueDO> values) {
        if (values == null || values.isEmpty()) return Flux.empty();
        return Flux.fromIterable(values)
                .concatMap(value -> databaseClient
                        .sql("SELECT " + COLUMNS + " FROM " + table()
                                + " WHERE tenant_id=:tenant_id AND message_id=:message_id AND status='PERSISTED'")
                        .bind("tenant_id", value.getTenantId())
                        .bind("message_id", value.getMessageId())
                        .map(this::map)
                        .one());
    }

    @Override
    public Mono<Integer> markPersisted(PointValueDO value, String owner) {
        if (value == null || value.getTenantId() == null || value.getMessageId() == null) return Mono.just(0);
        if (owner == null || owner.isBlank()) return Mono.just(0);
        return databaseClient
                .sql(
                        "UPDATE " + table() + " SET status='PERSISTED',claimed_at=CURRENT_TIMESTAMP,last_error=NULL "
                                + "WHERE tenant_id=:tenant_id AND message_id=:message_id AND status='CLAIMED' AND claimed_by=:owner")
                .bind("tenant_id", value.getTenantId())
                .bind("message_id", value.getMessageId())
                .bind("owner", owner)
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    private Mono<Boolean> insert(PointValueDO value, String owner) {
        require(value);
        String sql = "INSERT INTO " + table() + " (" + COLUMNS
                + ",status,attempts,available_at,claimed_at,claimed_by) VALUES "
                + "(:tenant_id,:message_id,:schema_version,:driver_node,:sequence,:fencing_token,:device_id,:point_id,"
                + ":raw_value,:cal_value,:num_value,:driver_id,:create_time,:operate_time,'CLAIMED',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,:claimed_by) "
                + (postgres()
                        ? "ON CONFLICT (tenant_id,message_id) DO NOTHING"
                        : "ON DUPLICATE KEY UPDATE message_id=message_id");
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("tenant_id", value.getTenantId())
                .bind("message_id", value.getMessageId())
                .bind("schema_version", value.getSchemaVersion())
                .bind("driver_node", value.getDriverNode())
                .bind("sequence", value.getSequence())
                .bind("fencing_token", value.getFencingToken())
                .bind("device_id", value.getDeviceId())
                .bind("point_id", value.getPointId())
                .bind("raw_value", value.getRawValue())
                .bind("cal_value", value.getCalValue())
                .bind("driver_id", value.getDriverId());
        spec = bindTime(spec, "create_time", value.getCreateTime());
        spec = bindTime(spec, "operate_time", value.getOperateTime());
        spec = value.getNumValue() == null
                ? spec.bindNull("num_value", Double.class)
                : spec.bind("num_value", value.getNumValue());
        spec = spec.bind("claimed_by", owner);
        return spec.fetch()
                .rowsUpdated()
                .then(databaseClient
                        .sql("SELECT status,claimed_by FROM " + table()
                                + " WHERE tenant_id=:tenant_id AND message_id=:message_id")
                        .bind("tenant_id", value.getTenantId())
                        .bind("message_id", value.getMessageId())
                        .map((row, metadata) -> "CLAIMED".equals(row.get("status", String.class))
                                && owner.equals(row.get("claimed_by", String.class)))
                        .one()
                        .defaultIfEmpty(false));
    }

    @Override
    public Flux<PointValueDO> claim(String owner, int limit) {
        if (owner == null || owner.isBlank() || limit < 1) return Flux.empty();
        int bounded = Math.min(limit, 500);
        String expired = postgres()
                ? "UPDATE " + table() + " SET status='PENDING',claimed_at=NULL,claimed_by=NULL "
                        + "WHERE status='CLAIMED' AND claimed_at < CURRENT_TIMESTAMP - INTERVAL '30 seconds'"
                : "UPDATE " + table() + " SET status='PENDING',claimed_at=NULL,claimed_by=NULL "
                        + "WHERE status='CLAIMED' AND claimed_at < CURRENT_TIMESTAMP - INTERVAL 30 SECOND";
        String persistedLeaseExpired =
                postgres() ? "CURRENT_TIMESTAMP - INTERVAL '30 seconds'" : "CURRENT_TIMESTAMP - INTERVAL 30 SECOND";
        String select = "SELECT " + COLUMNS + " FROM " + table()
                + " WHERE (status='PENDING' AND available_at<=CURRENT_TIMESTAMP)"
                + " OR (status='PERSISTED' AND (claimed_at IS NULL OR claimed_at < " + persistedLeaseExpired + "))"
                + " ORDER BY available_at,tenant_id,message_id LIMIT :limit FOR UPDATE SKIP LOCKED";
        Mono<List<PointValueDO>> claimed = databaseClient
                .sql(expired)
                .fetch()
                .rowsUpdated()
                .then(databaseClient
                        .sql(select)
                        .bind("limit", bounded)
                        .map(this::map)
                        .all()
                        .concatMap(value -> databaseClient
                                .sql(
                                        "UPDATE " + table()
                                                + " SET status='CLAIMED',attempts=attempts+1,claimed_at=CURRENT_TIMESTAMP,claimed_by=:owner "
                                                + "WHERE tenant_id=:tenant_id AND message_id=:message_id AND status IN ('PENDING','PERSISTED')")
                                .bind("owner", owner)
                                .bind("tenant_id", value.getTenantId())
                                .bind("message_id", value.getMessageId())
                                .fetch()
                                .rowsUpdated()
                                .filter(rows -> rows == 1)
                                .thenReturn(value))
                        .collectList());
        return transactionalOperator.transactional(claimed).flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Integer> markProcessed(PointValueDO value) {
        if (value == null || value.getTenantId() == null || value.getMessageId() == null) return Mono.just(0);
        return databaseClient
                .sql("UPDATE " + table()
                        + " SET status='PROCESSED',processed_at=CURRENT_TIMESTAMP,claimed_at=NULL,claimed_by=NULL,last_error=NULL "
                        + "WHERE tenant_id=:tenant_id AND message_id=:message_id AND status='PERSISTED'")
                .bind("tenant_id", value.getTenantId())
                .bind("message_id", value.getMessageId())
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Mono<Integer> markFailed(PointValueDO value, String owner, String error) {
        if (value == null || value.getTenantId() == null || value.getMessageId() == null) return Mono.just(0);
        if (owner == null || owner.isBlank()) return Mono.just(0);
        String backoff = postgres()
                ? "CURRENT_TIMESTAMP + (LEAST(300, POWER(2, attempts)) * INTERVAL '1 second')"
                : "DATE_ADD(CURRENT_TIMESTAMP, INTERVAL LEAST(300, POW(2, attempts)) SECOND)";
        String sql = "UPDATE " + table() + " SET status=CASE WHEN attempts>=10 THEN 'FAILED' ELSE 'PENDING' END, "
                + "available_at=" + backoff + ",claimed_at=NULL,claimed_by=NULL,last_error=:last_error "
                + "WHERE tenant_id=:tenant_id AND message_id=:message_id AND status IN ('CLAIMED','PERSISTED') AND claimed_by=:owner";
        return databaseClient
                .sql(sql)
                .bind("tenant_id", value.getTenantId())
                .bind("message_id", value.getMessageId())
                .bind("owner", owner)
                .bind("last_error", Objects.requireNonNullElse(error, "unknown error"))
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    private PointValueDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        PointValueDO value = new PointValueDO();
        value.setTenantId(number(row, "tenant_id", Long.class));
        value.setMessageId(row.get("message_id", String.class));
        value.setSchemaVersion(number(row, "schema_version", Integer.class));
        value.setDriverNode(row.get("driver_node", String.class));
        value.setSequence(number(row, "sequence", Long.class));
        value.setFencingToken(number(row, "fencing_token", Long.class));
        value.setDeviceId(number(row, "device_id", Long.class));
        value.setPointId(number(row, "point_id", Long.class));
        value.setRawValue(row.get("raw_value", String.class));
        value.setCalValue(row.get("cal_value", String.class));
        value.setNumValue(number(row, "num_value", Double.class));
        value.setDriverId(number(row, "driver_id", Long.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperateTime(time(row.get("operate_time")));
        return value;
    }

    private boolean postgres() {
        return "postgres".equalsIgnoreCase(dialect.name());
    }

    private void require(PointValueDO value) {
        if (value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getMessageId() == null
                || value.getMessageId().isBlank()) {
            throw new IllegalArgumentException("tenantId and messageId are required");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T number(io.r2dbc.spi.Row row, String name, Class<T> type) {
        Object raw = row.get(name);
        if (raw == null) return null;
        if (type.isInstance(raw)) return (T) raw;
        if (raw instanceof Number n) {
            if (type == Long.class) return (T) Long.valueOf(n.longValue());
            if (type == Integer.class) return (T) Integer.valueOf(n.intValue());
            if (type == Double.class) return (T) Double.valueOf(n.doubleValue());
        }
        throw new IllegalStateException("Column " + name + " is not numeric: " + raw.getClass());
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (value instanceof java.time.Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return null;
    }

    private DatabaseClient.GenericExecuteSpec bindTime(
            DatabaseClient.GenericExecuteSpec spec, String name, LocalDateTime value) {
        return postgres() ? spec.bind(name, value.atOffset(ZoneOffset.UTC)) : spec.bind(name, value);
    }
}
