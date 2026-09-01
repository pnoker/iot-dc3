package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.repository.RoleResourceBindFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive application service for role-resource assignments. */
public interface ReactiveRoleResourceBindService {
    Mono<RoleResourceBindBO> add(RoleResourceBindBO binding, Long tenantId);
    Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName);
    Mono<OffsetPage<RoleResourceBindBO>> list(RoleResourceBindFilter filter);
    Flux<ResourceBO> listResourcesByRole(Long tenantId, Long roleId);
    Flux<ResourceBO> listResourcesByPrincipal(Long tenantId, Long principalId);
    Flux<RoleBO> listRolesByResource(Long tenantId, Long resourceId);
}
