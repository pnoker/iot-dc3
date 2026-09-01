package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.UserDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Persistence port for users; every tenant-scoped operation carries tenantId explicitly. */
public interface ReactiveUserStore {

    Mono<UserDO> getById(Long tenantId, Long id);

    Mono<UserDO> getByUserName(Long tenantId, String userName);

    Mono<UserDO> getByPrincipalId(Long tenantId, Long principalId);

    Mono<OffsetPage<UserDO>> list(UserFilter filter);
}
