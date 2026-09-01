package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.GroupBO;
import io.github.pnoker.common.manager.repository.GroupFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive application service for groups. */
public interface ReactiveGroupService {
    Mono<GroupBO> add(GroupBO group);
    Mono<GroupBO> update(GroupBO group);
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
    Mono<GroupBO> getById(Long tenantId, Long id);
    Mono<OffsetPage<GroupBO>> list(GroupFilter filter);
}
