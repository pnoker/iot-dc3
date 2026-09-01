package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.TenantMembershipDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Non-blocking persistence port for tenant memberships. */
public interface ReactiveTenantMembershipStore {
    Mono<TenantMembershipDO> getById(Long tenantId, Long id);
    Mono<TenantMembershipDO> getByTenantAndPrincipal(Long tenantId, Long principalId);
    Flux<Long> listPrincipalIds(Long tenantId);
    Mono<OffsetPage<TenantMembershipDO>> list(TenantMembershipFilter filter);
}
