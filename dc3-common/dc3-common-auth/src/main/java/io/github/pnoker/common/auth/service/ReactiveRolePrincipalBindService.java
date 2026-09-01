package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.repository.RolePrincipalBindFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive application service for role-principal assignments. */
public interface ReactiveRolePrincipalBindService {
    Mono<RolePrincipalBindBO> add(RolePrincipalBindBO binding);
    Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName);
    Mono<OffsetPage<RolePrincipalBindBO>> list(RolePrincipalBindFilter filter);
    Flux<RoleBO> listRolesByPrincipal(Long tenantId, Long principalId);
    Flux<UserBO> listUsersByRole(Long tenantId, Long roleId);
}
