package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.TenantMembershipBuilder;
import io.github.pnoker.common.auth.repository.ReactiveTenantMembershipStore;
import io.github.pnoker.common.auth.repository.TenantMembershipFilter;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default non-blocking tenant-membership service. */
@Service
@RequiredArgsConstructor
public class ReactiveTenantMembershipServiceImpl implements ReactiveTenantMembershipService {

    private final ReactiveTenantMembershipStore store;
    private final TenantMembershipBuilder builder;

    @Override
    public Mono<TenantMembershipBO> getById(Long tenantId, Long id) {
        return requireIds(tenantId, id)
                .then(Mono.defer(() -> store.getById(tenantId, id)))
                .map(builder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Tenant membership")));
    }

    @Override
    public Mono<TenantMembershipBO> getByTenantAndPrincipal(Long tenantId, Long principalId) {
        return requireIds(tenantId, principalId)
                .then(Mono.defer(() -> store.getByTenantAndPrincipal(tenantId, principalId)))
                .map(builder::buildBOByDO);
    }

    @Override
    public Flux<Long> listPrincipalIds(Long tenantId) {
        if (tenantId == null || tenantId <= 0) return Flux.error(new RequestException("Tenant ID is required"));
        return store.listPrincipalIds(tenantId);
    }

    @Override
    public Mono<OffsetPage<TenantMembershipBO>> list(TenantMembershipFilter filter) {
        return store.list(filter).map(page -> OffsetPage.of(page.items().stream().map(builder::buildBOByDO).toList(),
                page.offset(), page.limit(), page.total()));
    }

    @Override
    public Mono<Boolean> isTenantMember(Long tenantId, Long principalId) {
        if (tenantId == null || tenantId <= 0 || principalId == null || principalId <= 0) return Mono.just(false);
        return store.getByTenantAndPrincipal(tenantId, principalId).hasElement();
    }

    @Override
    public Mono<TenantMembershipBO> requireTenantMember(Long tenantId, Long principalId) {
        return getByTenantAndPrincipal(tenantId, principalId)
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")));
    }

    private Mono<Void> requireIds(Long tenantId, Long id) {
        if (tenantId == null || tenantId <= 0 || id == null || id <= 0) {
            return Mono.error(new RequestException("Tenant membership IDs are required"));
        }
        return Mono.empty();
    }
}
