package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.PrincipalBO;
import io.github.pnoker.common.auth.repository.PrincipalFilter;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Reactive application service for the global principal catalog. */
public interface ReactivePrincipalService {

    Mono<PrincipalBO> getById(Long tenantId, Long id);

    Mono<OffsetPage<PrincipalBO>> list(Long tenantId, PrincipalFilter filter);

    Flux<PrincipalBO> listByIds(Long tenantId, Collection<Long> ids);

    Mono<PrincipalBO> setEnableFlag(Long tenantId, Long id, EnableFlagEnum target, Long operatorId, String operatorName);

    Mono<Boolean> touchLastLogin(Long id);
}
