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

import io.github.pnoker.common.data.repository.ReactiveEntityAlarmStore;

import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for {@code dc3_data.dc3_entity_alarm}. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcEntityAlarmStore implements ReactiveEntityAlarmStore {

    private static final String TABLE = "dc3_data.dc3_entity_alarm";
    private static final String COLUMNS = "id, alarm_target_type_flag, entity_id, driver_id, device_id, point_id,"
            + " rule_id, rule_state_id, dedupe_key, alarm_type_flag, alarm_source_flag, alarm_level_flag, alarm_ext,"
            + " expired_time, confirm_flag, tenant_id, create_time, operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcDialect dialect;

    @Override
    public Mono<EntityAlarmDO> insert(EntityAlarmDO alarm) {
        if (alarm == null || !validId(alarm.getTenantId())) {
            return Mono.error(new IllegalArgumentException("tenantId is required"));
        }
        if (alarm.getId() == null) {
            alarm.setId(stableId(alarm));
        }
        LocalDateTime now = utcNow();
        if (alarm.getCreateTime() == null) {
            alarm.setCreateTime(now);
        }
        if (alarm.getOperateTime() == null) {
            alarm.setOperateTime(now);
        }
        String insert = "INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES "
                + "(:id,:alarm_target_type_flag,:entity_id,:driver_id,:device_id,:point_id,:rule_id,:rule_state_id,:dedupe_key,"
                + ":alarm_type_flag,:alarm_source_flag,:alarm_level_flag," + dialect.jsonWriteExpression(":alarm_ext")
                + ",:expired_time,:confirm_flag,:tenant_id,:create_time,:operate_time)";
        String sql = dialect.name().equalsIgnoreCase("postgres")
                ? insert + " ON CONFLICT (tenant_id,dedupe_key) DO NOTHING"
                : insert + " ON DUPLICATE KEY UPDATE id=id";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("id", alarm.getId())
                .bind("alarm_target_type_flag", value(alarm.getAlarmTargetTypeFlag()))
                .bind("entity_id", value(alarm.getEntityId()))
                .bind("driver_id", value(alarm.getDriverId()))
                .bind("device_id", value(alarm.getDeviceId()))
                .bind("point_id", value(alarm.getPointId()))
                .bind("rule_id", value(alarm.getRuleId()))
                .bind("rule_state_id", value(alarm.getRuleStateId()))
                .bind("alarm_type_flag", value(alarm.getAlarmTypeFlag()))
                .bind("alarm_source_flag", value(alarm.getAlarmSourceFlag()))
                .bind("alarm_level_flag", value(alarm.getAlarmLevelFlag()))
                .bind("expired_time", value(alarm.getExpiredTime()))
                .bind("confirm_flag", value(alarm.getConfirmFlag()))
                .bind("tenant_id", alarm.getTenantId())
                .bind("create_time", alarm.getCreateTime())
                .bind("operate_time", alarm.getOperateTime());
        spec = bindNullable(spec, "dedupe_key", alarm.getDedupeKey(), String.class);
        spec = spec.bind("alarm_ext", alarm.getAlarmExt() == null ? "{}" : JsonUtil.toJsonString(alarm.getAlarmExt()));
        return transactionalOperator
                .transactional(spec.fetch().rowsUpdated())
                .flatMap(rows ->
                        alarm.getDedupeKey() == null || alarm.getDedupeKey().isBlank()
                                ? (rows == 1
                                        ? Mono.just(alarm)
                                        : Mono.error(new IllegalStateException(
                                                "entity alarm insert affected " + rows + " rows")))
                                : findByDedupe(alarm.getTenantId(), alarm.getDedupeKey())
                                        .switchIfEmpty(Mono.error(
                                                new IllegalStateException("entity alarm dedupe row is missing"))));
    }

    @Override
    public Mono<List<EntityAlarmDO>> insertBatch(List<EntityAlarmDO> alarms) {
        if (alarms == null || alarms.isEmpty()) {
            return Mono.just(List.of());
        }
        return transactionalOperator.transactional(
                Flux.fromIterable(alarms).concatMap(this::insert).collectList());
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long alarmId) {
        if (!validId(tenantId) || !validId(alarmId)) {
            return Mono.just(false);
        }
        return databaseClient
                .sql("DELETE FROM " + TABLE + " WHERE tenant_id=:tenant_id AND id=:id")
                .bind("tenant_id", tenantId)
                .bind("id", alarmId)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    @Override
    public Mono<Boolean> updateConfirm(Long tenantId, Long alarmId, byte confirmFlag) {
        if (!validId(tenantId) || !validId(alarmId) || (confirmFlag != 0 && confirmFlag != 1)) {
            return Mono.just(false);
        }
        return databaseClient
                .sql("UPDATE " + TABLE + " SET confirm_flag=:confirm_flag,operate_time=CURRENT_TIMESTAMP"
                        + " WHERE tenant_id=:tenant_id AND id=:id")
                .bind("confirm_flag", confirmFlag)
                .bind("tenant_id", tenantId)
                .bind("id", alarmId)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    private EntityAlarmDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        EntityAlarmDO value = new EntityAlarmDO();
        value.setId(row.get("id", Long.class));
        value.setAlarmTargetTypeFlag(number(row.get("alarm_target_type_flag")));
        value.setEntityId(row.get("entity_id", Long.class));
        value.setDriverId(row.get("driver_id", Long.class));
        value.setDeviceId(row.get("device_id", Long.class));
        value.setPointId(row.get("point_id", Long.class));
        value.setRuleId(row.get("rule_id", Long.class));
        value.setRuleStateId(row.get("rule_state_id", Long.class));
        value.setDedupeKey(row.get("dedupe_key", String.class));
        value.setAlarmTypeFlag(number(row.get("alarm_type_flag")));
        value.setAlarmSourceFlag(number(row.get("alarm_source_flag")));
        value.setAlarmLevelFlag(number(row.get("alarm_level_flag")));
        String rawExt = row.get("alarm_ext", String.class);
        if (rawExt != null) {
            try {
                value.setAlarmExt(JsonUtil.parseObject(rawExt, io.github.pnoker.common.entity.ext.JsonExt.class));
            } catch (RuntimeException exception) {
                throw new IllegalStateException("alarm_ext contains invalid JSON", exception);
            }
        }
        value.setExpiredTime(numberLong(row.get("expired_time")));
        value.setConfirmFlag(number(row.get("confirm_flag")));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperateTime(time(row.get("operate_time")));
        return value;
    }

    private Byte number(Object value) {
        return value instanceof Number number ? number.byteValue() : null;
    }

    private Long numberLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }

    private int value(Number value) {
        return value == null ? 0 : value.intValue();
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private boolean validId(Long value) {
        return value != null && value > 0;
    }

    private long stableId(EntityAlarmDO alarm) {
        if (alarm.getDedupeKey() == null || alarm.getDedupeKey().isBlank()) {
            return UuidV7.nextLong();
        }
        UUID stable = UUID.nameUUIDFromBytes(
                (alarm.getTenantId() + ":" + alarm.getDedupeKey()).getBytes(StandardCharsets.UTF_8));
        long id = (stable.getMostSignificantBits() ^ Long.rotateLeft(stable.getLeastSignificantBits(), 29))
                & Long.MAX_VALUE;
        return id == 0 ? 1 : id;
    }

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private Mono<EntityAlarmDO> findByDedupe(Long tenantId, String dedupeKey) {
        if (!validId(tenantId) || dedupeKey == null || dedupeKey.isBlank()) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND dedupe_key=:dedupe_key LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("dedupe_key", dedupeKey)
                .map(this::map)
                .one();
    }
}
