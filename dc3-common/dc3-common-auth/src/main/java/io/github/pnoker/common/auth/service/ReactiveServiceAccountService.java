package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.repository.ServiceAccountFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive application service for tenant-scoped service-account aggregates. */
public interface ReactiveServiceAccountService {
    Mono<ServiceAccountBO> getById(Long tenantId, Long id);
    Mono<OffsetPage<ServiceAccountBO>> list(ServiceAccountFilter filter);
    Mono<ServiceAccountBO> add(ServiceAccountBO account);
    Mono<ServiceAccountBO> update(Long tenantId, ServiceAccountBO account);
    Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
