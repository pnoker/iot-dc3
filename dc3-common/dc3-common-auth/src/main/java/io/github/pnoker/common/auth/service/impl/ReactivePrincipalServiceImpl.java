package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.PrincipalBO;
import io.github.pnoker.common.auth.entity.builder.PrincipalBuilder;
import io.github.pnoker.common.auth.repository.PrincipalFilter;
import io.github.pnoker.common.auth.repository.ReactivePrincipalStore;
import io.github.pnoker.common.auth.service.ReactivePrincipalService;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Default non-blocking principal application service. */
@Service
@RequiredArgsConstructor
public class ReactivePrincipalServiceImpl implements ReactivePrincipalService {

    private final ReactivePrincipalStore principalStore;
    private final PrincipalBuilder principalBuilder;

    @Override
    public Mono<PrincipalBO> getById(Long tenantId, Long id) {
        if (tenantId == null || tenantId <= 0 || id == null || id <= 0) return Mono.error(new RequestException("Principal ID is required"));
        return principalStore.getById(tenantId, id).map(principalBuilder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Principal")));
    }

    @Override
    public Mono<OffsetPage<PrincipalBO>> list(Long tenantId, PrincipalFilter filter) {
        return principalStore.list(tenantId, filter).map(page -> OffsetPage.of(
                page.items().stream().map(principalBuilder::buildBOByDO).toList(),
                page.offset(), page.limit(), page.total()));
    }

    @Override
    public Flux<PrincipalBO> listByIds(Long tenantId, Collection<Long> ids) {
        return principalStore.listByIds(tenantId, ids).map(principalBuilder::buildBOByDO);
    }

    @Override
    public Mono<PrincipalBO> setEnableFlag(Long tenantId, Long id, EnableFlagEnum target, Long operatorId, String operatorName) {
        if (tenantId == null || tenantId <= 0 || id == null || id <= 0 || target == null) return Mono.error(new RequestException("Principal update is invalid"));
        return principalStore.updateEnableFlag(tenantId, id, target.getIndex(), operatorId, operatorName)
                .map(principalBuilder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Principal")));
    }

    @Override
    public Mono<Boolean> touchLastLogin(Long id) {
        return principalStore.touchLastLogin(id);
    }
}
