package io.github.pnoker.common.data.repository;

import reactor.core.publisher.Mono;

/** Reactive tenant-scoped reads used by the alarm rule pipeline. */
public interface ReactiveRuleStateLookup {

    Mono<Boolean> hasFiringState(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId);

    Mono<Long> getFiringAlarmId(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId);
}
