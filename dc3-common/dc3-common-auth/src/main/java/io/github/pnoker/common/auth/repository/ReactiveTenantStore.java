package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Persistence port for the global tenant catalog. */
public interface ReactiveTenantStore {

    Mono<TenantDO> getById(Long id);

    Mono<TenantDO> getEnabledByCode(String code);

    Mono<TenantDO> getByNameAndCode(String tenantName, String tenantCode);

    Mono<OffsetPage<TenantDO>> list(TenantFilter filter);

    Mono<TenantDO> insert(TenantDO tenant);

    Mono<TenantDO> update(TenantDO tenant);

    Mono<Boolean> delete(Long id, Long operatorId, String operatorName);
}
