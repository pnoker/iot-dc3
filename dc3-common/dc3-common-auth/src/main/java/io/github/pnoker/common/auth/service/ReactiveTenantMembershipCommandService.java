package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import reactor.core.publisher.Mono;

/** Non-blocking tenant membership commands. */
public interface ReactiveTenantMembershipCommandService {

    Mono<TenantMembershipBO> add(TenantMembershipBO membership);

    Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
