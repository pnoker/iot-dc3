package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.model.RoleResourceBindDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Non-blocking persistence port for role-resource bindings. */
public interface ReactiveRoleResourceBindStore {
    Mono<OffsetPage<RoleResourceBindDO>> list(RoleResourceBindFilter filter);
    Mono<RoleResourceBindDO> insert(RoleResourceBindBO binding);
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
    Mono<Boolean> exists(Long tenantId, Long roleId, Long resourceId);
    Flux<Long> listResourceIds(Long tenantId, Long roleId);
    Flux<Long> listResourceIdsByPrincipal(Long tenantId, Long principalId);
    Flux<Long> listRoleIdsByResource(Long tenantId, Long resourceId);
}
