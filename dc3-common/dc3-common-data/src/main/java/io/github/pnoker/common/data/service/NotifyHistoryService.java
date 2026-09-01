package io.github.pnoker.common.data.service;

import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.query.NotifyHistoryQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive notification delivery history service. */
public interface NotifyHistoryService {
    Mono<NotifyHistoryBO> getById(Long tenantId, Long id);
    Mono<OffsetPage<NotifyHistoryBO>> list(Long tenantId, NotifyHistoryQuery query);
    Mono<Boolean> delete(Long tenantId, Long id);
}
