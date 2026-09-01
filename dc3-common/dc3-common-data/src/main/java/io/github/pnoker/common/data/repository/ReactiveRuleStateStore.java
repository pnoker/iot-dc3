package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** Reactive tenant-scoped persistence for rule runtime state. */
public interface ReactiveRuleStateStore {

    Mono<RuleStateDO> get(long tenantId, long stateId);

    Mono<OffsetPage<RuleStateDO>> list(long tenantId, Long ruleId, Byte alarmTargetTypeFlag, Long entityId,
                                       String fingerprint, Byte entityStateFlag, Long alarmId, PageRequest page);

    Mono<Boolean> delete(long tenantId, long stateId);

    Mono<RuleStateDO> find(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId, String fingerprint);

    Mono<RuleStateDO> transition(RuleStateDO state, boolean recovery);

    Mono<Boolean> updateLastNotifyTime(long tenantId, long stateId, LocalDateTime lastNotifyTime);
}
