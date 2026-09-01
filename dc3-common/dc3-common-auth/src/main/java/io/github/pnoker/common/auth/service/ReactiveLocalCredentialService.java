package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.repository.LocalCredentialFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Non-blocking local credential queries and authentication primitives. */
public interface ReactiveLocalCredentialService {
    Mono<LocalCredentialBO> getById(Long tenantId, Long id);
    Mono<LocalCredentialBO> getByLoginName(Long tenantId, String loginName);
    Mono<OffsetPage<LocalCredentialBO>> list(LocalCredentialFilter filter);
    Mono<Boolean> isLoginNameAvailable(Long tenantId, String loginName);
    Mono<Boolean> verifyPassword(LocalCredentialBO credential, String rawPassword);
}
