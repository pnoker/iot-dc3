package io.github.pnoker.common.auth.service;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleTreeBO;
import io.github.pnoker.common.auth.repository.RoleFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface ReactiveRoleService {
 Mono<RoleBO> getById(Long tenantId, Long id);
 Mono<OffsetPage<RoleBO>> list(RoleFilter filter);
 Flux<RoleTreeBO> listTree(RoleFilter filter);
 Mono<RoleBO> add(RoleBO role);
 Mono<RoleBO> update(Long tenantId, RoleBO role);
 Mono<Void> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
