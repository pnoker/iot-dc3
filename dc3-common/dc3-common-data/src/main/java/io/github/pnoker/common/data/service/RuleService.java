package io.github.pnoker.common.data.service;

import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.query.RuleQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive business service for tenant-scoped alarm rules. */
public interface RuleService {

    Mono<RuleBO> add(RuleBO entityBO);

    Mono<Boolean> delete(Long tenantId, Long id);

    Mono<RuleBO> update(RuleBO entityBO);

    Mono<RuleBO> getById(Long tenantId, Long id);

    Mono<OffsetPage<RuleBO>> list(Long tenantId, RuleQuery entityQuery);
}
