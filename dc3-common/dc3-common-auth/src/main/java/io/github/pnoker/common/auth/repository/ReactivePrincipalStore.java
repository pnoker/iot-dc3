package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Persistence port for the platform principal catalog. */
public interface ReactivePrincipalStore {

    Mono<PrincipalDO> getById(Long tenantId, Long id);

    Mono<OffsetPage<PrincipalDO>> list(Long tenantId, PrincipalFilter filter);

    Flux<PrincipalDO> listByIds(Long tenantId, Collection<Long> ids);

    Mono<PrincipalDO> updateEnableFlag(Long tenantId, Long id, byte enableFlag, Long operatorId, String operatorName);

    Mono<Boolean> touchLastLogin(Long id);
}
