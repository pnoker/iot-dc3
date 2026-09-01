package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.TenantMembershipDO;
import reactor.core.publisher.Mono;

/** Non-blocking write port for tenant memberships. */
public interface ReactiveTenantMembershipCommandStore {

    Mono<TenantMembershipDO> insert(TenantMembershipDO membership);

    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
