package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.repository.UserFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped user read service. */
public interface ReactiveUserService {
    Mono<UserBO> getById(Long tenantId, Long id);
    Mono<UserBO> getByUserName(Long tenantId, String userName);
    Mono<UserBO> getByPrincipalId(Long tenantId, Long principalId);
    Mono<OffsetPage<UserBO>> list(UserFilter filter);
}
