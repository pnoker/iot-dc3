package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped persistence port for alarm rules. */
public interface ReactiveRuleStore {

    Flux<RuleBO> listEnabledCandidates(long tenantId, AlarmTargetTypeEnum targetType, long entityId);

    Mono<RuleDO> get(long tenantId, long id);

    Mono<OffsetPage<RuleDO>> list(long tenantId, String ruleName, String ruleCode, Long entityId,
                                  AlarmTargetTypeEnum targetType, io.github.pnoker.common.enums.EnableFlagEnum enableFlag,
                                  PageRequest pageRequest);

    Mono<RuleDO> insert(RuleDO rule);

    Mono<RuleDO> update(RuleDO rule);

    Mono<Boolean> softDelete(long tenantId, long id);

    Mono<Boolean> hasChildren(long tenantId, long id);

    Mono<Boolean> existsActiveCode(long tenantId, String ruleCode, Long excludedId);
}
