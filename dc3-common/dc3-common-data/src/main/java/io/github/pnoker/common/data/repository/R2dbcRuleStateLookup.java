package io.github.pnoker.common.data.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for rule-state reads on the hot alarm path. */
@Repository
@ConditionalOnClass(DatabaseClient.class)
@RequiredArgsConstructor
public class R2dbcRuleStateLookup implements ReactiveRuleStateLookup {

    private static final String TABLE = "dc3_data.dc3_rule_state";
    private static final int FIRING = 1;

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Boolean> hasFiringState(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId) {
        if (!valid(tenantId) || !valid(ruleId) || !valid(entityId)) {
            return Mono.just(false);
        }
        return databaseClient.sql("SELECT 1 FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND rule_id=:rule_id AND alarm_target_type_flag=:target_type"
                        + " AND entity_id=:entity_id AND entity_state_flag=:firing LIMIT 1")
                .bind("tenant_id", tenantId).bind("rule_id", ruleId).bind("target_type", alarmTargetTypeFlag)
                .bind("entity_id", entityId).bind("firing", FIRING)
                .map((row, metadata) -> true).one().defaultIfEmpty(false);
    }

    @Override
    public Mono<Long> getFiringAlarmId(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId) {
        if (!valid(tenantId) || !valid(ruleId) || !valid(entityId)) {
            return Mono.empty();
        }
        return databaseClient.sql("SELECT alarm_id FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND rule_id=:rule_id AND alarm_target_type_flag=:target_type"
                        + " AND entity_id=:entity_id AND entity_state_flag=:firing AND alarm_id > 0"
                        + " ORDER BY CASE WHEN last_trigger_time IS NULL THEN 1 ELSE 0 END ASC,"
                        + " last_trigger_time DESC, id DESC LIMIT 1")
                .bind("tenant_id", tenantId).bind("rule_id", ruleId).bind("target_type", alarmTargetTypeFlag)
                .bind("entity_id", entityId).bind("firing", FIRING)
                .map((row, metadata) -> row.get("alarm_id", Long.class)).one();
    }

    private boolean valid(long value) {
        return value > 0;
    }
}
