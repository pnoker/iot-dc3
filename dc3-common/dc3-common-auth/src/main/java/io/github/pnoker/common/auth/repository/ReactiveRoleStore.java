package io.github.pnoker.common.auth.repository;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface ReactiveRoleStore {
 Mono<RoleDO> getById(Long tenantId, Long id);
 Mono<OffsetPage<RoleDO>> list(RoleFilter filter);
 Flux<RoleDO> listTree(RoleFilter filter);
 Mono<RoleDO> insert(RoleBO role);
 Mono<RoleDO> update(Long tenantId, RoleBO role);
 Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
