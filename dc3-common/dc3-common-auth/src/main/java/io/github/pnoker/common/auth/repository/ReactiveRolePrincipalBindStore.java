package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.model.RolePrincipalBindDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Non-blocking persistence port for role-principal bindings. */
public interface ReactiveRolePrincipalBindStore {
    Mono<RolePrincipalBindDO> getById(Long tenantId, Long id);
    Mono<OffsetPage<RolePrincipalBindDO>> list(RolePrincipalBindFilter filter);
    Mono<RolePrincipalBindDO> insert(RolePrincipalBindBO binding);
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
    Mono<Boolean> exists(Long tenantId, Long roleId, Long principalId, Long excludedId);
    Flux<Long> listRoleIds(Long tenantId, Long principalId);
    Flux<Long> listPrincipalIds(Long tenantId, Long roleId, String principalType);
}
