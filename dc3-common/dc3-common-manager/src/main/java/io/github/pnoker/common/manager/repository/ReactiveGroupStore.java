package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.GroupBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped groups. */
public interface ReactiveGroupStore {
    Mono<OffsetPage<GroupBO>> list(GroupFilter filter);
    Mono<GroupBO> get(Long tenantId, Long id);
    Mono<GroupBO> getByName(Long tenantId, byte type, Long parentId, String name);
    Mono<Boolean> hasChildren(Long tenantId, Long id);
    Mono<Boolean> hasActiveBindings(Long tenantId, Long id);
    Mono<GroupBO> insert(GroupBO group);
    Mono<GroupBO> update(GroupBO group);
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
