package io.github.pnoker.common.data.service;

import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.data.entity.query.RuleStateQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive rule runtime state service. */
public interface RuleStateService {

    Mono<RuleStateBO> getById(Long tenantId, Long id);

    Mono<OffsetPage<RuleStateBO>> list(Long tenantId, RuleStateQuery query);

    Mono<Boolean> delete(Long tenantId, Long id);
}
