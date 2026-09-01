package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.repository.TenantMembershipFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive tenant-membership security boundary. */
public interface ReactiveTenantMembershipService {
    Mono<TenantMembershipBO> getById(Long tenantId, Long id);
    Mono<TenantMembershipBO> getByTenantAndPrincipal(Long tenantId, Long principalId);
    Flux<Long> listPrincipalIds(Long tenantId);
    Mono<OffsetPage<TenantMembershipBO>> list(TenantMembershipFilter filter);
    Mono<Boolean> isTenantMember(Long tenantId, Long principalId);
    Mono<TenantMembershipBO> requireTenantMember(Long tenantId, Long principalId);
}
