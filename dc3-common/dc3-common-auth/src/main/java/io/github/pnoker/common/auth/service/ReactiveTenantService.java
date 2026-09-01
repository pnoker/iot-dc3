package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.repository.TenantFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive application service for the global tenant catalog. */
public interface ReactiveTenantService {

    Mono<TenantBO> getById(Long id);

    Mono<TenantBO> getByCode(String code);

    Mono<OffsetPage<TenantBO>> list(TenantFilter filter);

    Mono<TenantBO> add(TenantBO tenant);

    Mono<TenantBO> update(TenantBO tenant);

    Mono<Boolean> delete(Long id, Long operatorId, String operatorName);
}
