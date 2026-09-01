package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.TenantMembershipBuilder;
import io.github.pnoker.common.auth.repository.ReactiveTenantMembershipCommandStore;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipCommandService;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default non-blocking tenant membership command service. */
@Service
@RequiredArgsConstructor
public class ReactiveTenantMembershipCommandServiceImpl implements ReactiveTenantMembershipCommandService {

    private final ReactiveTenantMembershipCommandStore store;
    private final TenantMembershipBuilder builder;

    @Override
    public Mono<TenantMembershipBO> add(TenantMembershipBO membership) {
        return Mono.defer(() -> {
            if (membership == null || membership.getTenantId() == null || membership.getTenantId() <= 0
                    || membership.getPrincipalId() == null || membership.getPrincipalId() <= 0) {
                return Mono.error(new RequestException("Tenant and principal are required"));
            }
            return store.insert(builder.buildDOByBO(membership))
                    .map(builder::buildBOByDO)
                    .switchIfEmpty(Mono.error(new RequestException("Failed to create tenant membership")))
                    .onErrorMap(DataIntegrityViolationException.class,
                            error -> new DuplicateException("Tenant membership already exists"));
        });
    }

    @Override
    public Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName) {
        if (tenantId == null || tenantId <= 0 || id == null || id <= 0) {
            return Mono.error(new RequestException("Tenant membership IDs are required"));
        }
        return store.delete(tenantId, id, operatorId, operatorName)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.<Void>empty()
                        : Mono.error(new NotFoundException("Tenant membership does not exist")));
    }
}
