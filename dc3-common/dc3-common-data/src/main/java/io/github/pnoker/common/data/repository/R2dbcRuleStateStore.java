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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for {@code dc3_data.dc3_rule_state}. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcRuleStateStore implements ReactiveRuleStateStore {

    private static final String TABLE = "dc3_data.dc3_rule_state";
    private static final String COLUMNS = "id,rule_id,alarm_target_type_flag,entity_id,fingerprint,entity_state_flag,"
            + "first_trigger_time,last_trigger_time,last_recover_time,last_notify_time,trigger_count,alarm_id,"
            + "entity_state_ext,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<RuleStateDO> get(long tenantId, long stateId) {
        if (!valid(tenantId) || !valid(stateId)) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE tenant_id=:tenant_id AND id=:id LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", stateId)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<RuleStateDO>> list(
            long tenantId,
            Long ruleId,
            Byte alarmTargetTypeFlag,
            Long entityId,
            String fingerprint,
            Byte entityStateFlag,
            Long alarmId,
            PageRequest page) {
        if (!valid(tenantId)) return Mono.just(OffsetPage.of(List.of(), page.offset(), page.limit(), 0));
        StringBuilder where = new StringBuilder(" WHERE tenant_id=:tenant_id");
        List<String> binds = new ArrayList<>();
        if (ruleId != null) {
            where.append(" AND rule_id=:rule_id");
            binds.add("rule_id");
        }
        if (alarmTargetTypeFlag != null) {
            where.append(" AND alarm_target_type_flag=:target_type");
            binds.add("target_type");
        }
        if (entityId != null) {
            where.append(" AND entity_id=:entity_id");
            binds.add("entity_id");
        }
        if (fingerprint != null && !fingerprint.isBlank()) {
            where.append(" AND fingerprint LIKE :fingerprint");
            binds.add("fingerprint");
        }
        if (entityStateFlag != null) {
            where.append(" AND entity_state_flag=:state_flag");
            binds.add("state_flag");
        }
        if (alarmId != null) {
            where.append(" AND alarm_id=:alarm_id");
            binds.add("alarm_id");
        }
        String condition = where.toString();
        DatabaseClient.GenericExecuteSpec countSpec =
                databaseClient.sql("SELECT COUNT(*) FROM " + TABLE + condition).bind("tenant_id", tenantId);
        DatabaseClient.GenericExecuteSpec rowsSpec = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + condition + " ORDER BY " + orderBy(page.sort())
                        + " LIMIT :limit OFFSET :offset")
                .bind("tenant_id", tenantId)
                .bind("limit", page.limit())
                .bind("offset", page.offset());
        for (String bind : binds) {
            Object value =
                    switch (bind) {
                        case "rule_id" -> ruleId;
                        case "target_type" -> alarmTargetTypeFlag;
                        case "entity_id" -> entityId;
                        case "fingerprint" -> "%" + fingerprint + "%";
                        case "state_flag" -> entityStateFlag;
                        case "alarm_id" -> alarmId;
                        default -> null;
                    };
            countSpec = countSpec.bind(bind, value);
            rowsSpec = rowsSpec.bind(bind, value);
        }
        Mono<Long> total =
                countSpec.map((row, metadata) -> row.get(0, Long.class)).one().defaultIfEmpty(0L);
        DatabaseClient.GenericExecuteSpec itemRows = rowsSpec;
        return total.flatMap(totalCount -> itemRows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, page.offset(), page.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<Boolean> delete(long tenantId, long stateId) {
        if (!valid(tenantId) || !valid(stateId)) return Mono.just(false);
        return databaseClient
                .sql("DELETE FROM " + TABLE + " WHERE tenant_id=:tenant_id AND id=:id")
                .bind("tenant_id", tenantId)
                .bind("id", stateId)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    @Override
    public Mono<RuleStateDO> find(
            long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId, String fingerprint) {
        if (!valid(tenantId) || !valid(ruleId) || !valid(entityId) || fingerprint == null) {
            return Mono.empty();
        }
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND rule_id=:rule_id AND alarm_target_type_flag=:target_type"
                        + " AND entity_id=:entity_id AND fingerprint=:fingerprint LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("rule_id", ruleId)
                .bind("target_type", alarmTargetTypeFlag)
                .bind("entity_id", entityId)
                .bind("fingerprint", fingerprint)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<RuleStateDO> transition(RuleStateDO state, boolean recovery) {
        if (state == null
                || !valid(state.getTenantId())
                || !valid(state.getRuleId())
                || !valid(state.getEntityId())
                || state.getFingerprint() == null) {
            return Mono.error(new IllegalArgumentException("tenant, rule, entity and fingerprint are required"));
        }
        if (state.getId() == null) {
            state.setId(UuidV7.nextLong());
        }
        LocalDateTime now = utcNow();
        if (state.getCreateTime() == null) state.setCreateTime(now);
        if (state.getOperateTime() == null) state.setOperateTime(now);
        if (state.getTriggerCount() == null) state.setTriggerCount(0L);
        if (state.getAlarmId() == null) state.setAlarmId(0L);
        String update = "UPDATE " + TABLE + " SET entity_state_flag=:state_flag, last_trigger_time=:last_trigger_time,"
                + " last_recover_time=:last_recover_time, alarm_id=:alarm_id, entity_state_ext="
                + dialect.jsonWriteExpression(":state_ext") + ", trigger_count=trigger_count+:trigger_delta"
                + " WHERE tenant_id=:tenant_id AND rule_id=:rule_id AND alarm_target_type_flag=:target_type"
                + " AND entity_id=:entity_id AND fingerprint=:fingerprint";
        DatabaseClient.GenericExecuteSpec updateSpec = databaseClient
                .sql(update)
                .bind("state_flag", value(state.getEntityStateFlag()))
                .bind("alarm_id", state.getAlarmId())
                .bind(
                        "state_ext",
                        state.getEntityStateExt() == null ? "{}" : JsonUtil.toJsonString(state.getEntityStateExt()))
                .bind("trigger_delta", recovery ? 0L : 1L)
                .bind("tenant_id", state.getTenantId())
                .bind("rule_id", state.getRuleId())
                .bind("target_type", value(state.getAlarmTargetTypeFlag()))
                .bind("entity_id", state.getEntityId())
                .bind("fingerprint", state.getFingerprint());
        updateSpec = bindNullable(updateSpec, "last_trigger_time", state.getLastTriggerTime(), LocalDateTime.class);
        updateSpec = bindNullable(updateSpec, "last_recover_time", state.getLastRecoverTime(), LocalDateTime.class);
        return transactionalOperator
                .transactional(updateSpec.fetch().rowsUpdated())
                .flatMap(rows -> rows == 1
                        ? find(
                                state.getTenantId(),
                                state.getRuleId(),
                                value(state.getAlarmTargetTypeFlag()),
                                state.getEntityId(),
                                state.getFingerprint())
                        : insert(state, recovery))
                .onErrorResume(
                        DataIntegrityViolationException.class,
                        error -> find(
                                        state.getTenantId(),
                                        state.getRuleId(),
                                        value(state.getAlarmTargetTypeFlag()),
                                        state.getEntityId(),
                                        state.getFingerprint())
                                .flatMap(existing -> transition(existing, recovery)));
    }

    private Mono<RuleStateDO> insert(RuleStateDO state, boolean recovery) {
        state.setTriggerCount(recovery ? 0L : 1L);
        String sql = "INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES "
                + "(:id,:rule_id,:target_type,:entity_id,:fingerprint,:state_flag,:first_trigger_time,:last_trigger_time,"
                + ":last_recover_time,:last_notify_time,:trigger_count,:alarm_id,"
                + dialect.jsonWriteExpression(":state_ext")
                + ",:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time)";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("id", state.getId())
                .bind("rule_id", state.getRuleId())
                .bind("target_type", value(state.getAlarmTargetTypeFlag()))
                .bind("entity_id", state.getEntityId())
                .bind("fingerprint", state.getFingerprint())
                .bind("state_flag", value(state.getEntityStateFlag()))
                .bind("trigger_count", state.getTriggerCount())
                .bind("alarm_id", state.getAlarmId())
                .bind("tenant_id", state.getTenantId())
                .bind("remark", text(state.getRemark()))
                .bind("creator_id", longValue(state.getCreatorId()))
                .bind("creator_name", text(state.getCreatorName()))
                .bind("create_time", state.getCreateTime())
                .bind("operator_id", longValue(state.getOperatorId()))
                .bind("operator_name", text(state.getOperatorName()))
                .bind("operate_time", state.getOperateTime());
        spec = bindNullable(spec, "first_trigger_time", state.getFirstTriggerTime(), LocalDateTime.class);
        spec = bindNullable(spec, "last_trigger_time", state.getLastTriggerTime(), LocalDateTime.class);
        spec = bindNullable(spec, "last_recover_time", state.getLastRecoverTime(), LocalDateTime.class);
        spec = bindNullable(spec, "last_notify_time", state.getLastNotifyTime(), LocalDateTime.class);
        spec = bindNullable(
                spec,
                "state_ext",
                state.getEntityStateExt() == null ? "{}" : JsonUtil.toJsonString(state.getEntityStateExt()),
                String.class);
        return transactionalOperator
                .transactional(spec.fetch().rowsUpdated())
                .flatMap(rows -> rows == 1
                        ? Mono.just(state)
                        : Mono.error(new IllegalStateException("rule state insert affected " + rows + " rows")));
    }

    @Override
    public Mono<Boolean> updateLastNotifyTime(long tenantId, long stateId, LocalDateTime lastNotifyTime) {
        if (!valid(tenantId) || !valid(stateId) || lastNotifyTime == null) return Mono.just(false);
        return databaseClient
                .sql("UPDATE " + TABLE + " SET last_notify_time=:last_notify_time"
                        + " WHERE tenant_id=:tenant_id AND id=:id")
                .bind("last_notify_time", lastNotifyTime)
                .bind("tenant_id", tenantId)
                .bind("id", stateId)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    private RuleStateDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        RuleStateDO value = new RuleStateDO();
        value.setId(row.get("id", Long.class));
        value.setRuleId(row.get("rule_id", Long.class));
        value.setAlarmTargetTypeFlag(number(row.get("alarm_target_type_flag", Number.class)));
        value.setEntityId(row.get("entity_id", Long.class));
        value.setFingerprint(row.get("fingerprint", String.class));
        value.setEntityStateFlag(number(row.get("entity_state_flag", Number.class)));
        value.setFirstTriggerTime(time(row.get("first_trigger_time")));
        value.setLastTriggerTime(time(row.get("last_trigger_time")));
        value.setLastRecoverTime(time(row.get("last_recover_time")));
        value.setLastNotifyTime(time(row.get("last_notify_time")));
        value.setTriggerCount(row.get("trigger_count", Long.class));
        value.setAlarmId(row.get("alarm_id", Long.class));
        value.setEntityStateExt(json(row.get("entity_state_ext", String.class)));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setRemark(row.get("remark", String.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        return value;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "last_trigger_time DESC, id DESC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (spec.field()) {
                        case "ruleId" -> "rule_id";
                        case "entityId" -> "entity_id";
                        case "entityStateFlag" -> "entity_state_flag";
                        case "triggerCount" -> "trigger_count";
                        case "lastTriggerTime" -> "last_trigger_time";
                        case "lastNotifyTime" -> "last_notify_time";
                        case "createTime" -> "create_time";
                        case "id" -> "id";
                        default -> throw new IllegalArgumentException("unsupported sort field: " + spec.field());
                    };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id DESC");
        return String.join(", ", clauses);
    }

    private JsonExt json(String raw) {
        if (raw == null) return null;
        try {
            return JsonUtil.parseObject(raw, JsonExt.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LocalDateTime time(Object raw) {
        if (raw instanceof LocalDateTime value) return value;
        if (raw instanceof OffsetDateTime value)
            return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return raw instanceof java.time.Instant value ? LocalDateTime.ofInstant(value, ZoneOffset.UTC) : null;
    }

    private Byte number(Number value) {
        return value == null ? null : value.byteValue();
    }

    private Byte value(Byte value) {
        return value == null ? (byte) 0 : value;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private Long longValue(Long value) {
        return value == null ? 0L : value;
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }

    private boolean valid(long value) {
        return value > 0;
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}
