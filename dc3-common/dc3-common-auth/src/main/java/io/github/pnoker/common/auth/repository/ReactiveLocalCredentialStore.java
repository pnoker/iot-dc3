package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.LocalCredentialDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Non-blocking persistence port for tenant-scoped local credentials. */
public interface ReactiveLocalCredentialStore {
    Mono<LocalCredentialDO> getById(Long tenantId, Long id);
    Mono<LocalCredentialDO> getByLoginName(Long tenantId, String loginNameNormalized);
    Mono<Boolean> existsByLoginName(Long tenantId, String loginNameNormalized);
    Mono<OffsetPage<LocalCredentialDO>> list(LocalCredentialFilter filter);
}
