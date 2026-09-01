package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.builder.ServiceAccountBuilder;
import io.github.pnoker.common.auth.repository.ReactiveServiceAccountStore;
import io.github.pnoker.common.auth.repository.ServiceAccountFilter;
import io.github.pnoker.common.auth.service.ReactiveServiceAccountService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default non-blocking service-account application service. */
@Service
@RequiredArgsConstructor
public class ReactiveServiceAccountServiceImpl implements ReactiveServiceAccountService {
    private final ReactiveServiceAccountStore store;
    private final ServiceAccountBuilder builder;
    private final ReactiveTenantMembershipService membershipService;

    @Override
    public Mono<ServiceAccountBO> getById(Long tenantId, Long id) {
        if (!valid(tenantId, id)) return Mono.error(new RequestException("Service account ID is required"));
        return store.getById(tenantId, id).map(builder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Service account")));
    }

    @Override
    public Mono<OffsetPage<ServiceAccountBO>> list(ServiceAccountFilter filter) {
        return store.list(filter).map(page -> OffsetPage.of(page.items().stream().map(builder::buildBOByDO).toList(),
                page.offset(), page.limit(), page.total()));
    }

    @Override
    public Mono<ServiceAccountBO> add(ServiceAccountBO account) {
        if (account == null || !valid(account.getTenantId(), account.getOwnerPrincipalId())) {
            return Mono.error(new RequestException("Tenant and owner principal are required"));
        }
        return membershipService.requireTenantMember(account.getTenantId(), account.getOwnerPrincipalId())
                .then(store.insert(account)).map(builder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Service account")))
                .onErrorMap(DataIntegrityViolationException.class, error -> new RequestException("Service account already exists"));
    }

    @Override
    public Mono<ServiceAccountBO> update(Long tenantId, ServiceAccountBO account) {
        if (account == null || !valid(tenantId, account.getId())) return Mono.error(new RequestException("Service account update is invalid"));
        account.setTenantId(tenantId);
        return membershipService.requireTenantMember(tenantId, account.getOwnerPrincipalId())
                .then(store.update(tenantId, account)).map(builder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Service account")))
                .onErrorMap(DataIntegrityViolationException.class, error -> new RequestException("Service account already exists"));
    }

    @Override
    public Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (!valid(tenantId, id)) return Mono.error(new RequestException("Service account ID is required"));
        return store.delete(tenantId, id, operatorId, operatorName)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted) ? Mono.<Void>empty() : Mono.error(new NotFoundException("Service account")));
    }

    private boolean valid(Long tenantId, Long id) { return tenantId != null && tenantId > 0 && id != null && id > 0; }
}
