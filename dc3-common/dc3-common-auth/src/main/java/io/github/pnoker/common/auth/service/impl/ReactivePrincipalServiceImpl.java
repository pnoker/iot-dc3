/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default non-blocking principal application service. */
@Service
@RequiredArgsConstructor
public class ReactivePrincipalServiceImpl implements ReactivePrincipalService {

    private final ReactivePrincipalStore principalStore;
    private final PrincipalBuilder principalBuilder;

    @Override
    public Mono<PrincipalBO> getById(Long tenantId, Long id) {
        if (tenantId == null || tenantId <= 0 || id == null || id <= 0)
            return Mono.error(new RequestException("Principal ID is required"));
        return principalStore
                .getById(tenantId, id)
                .map(principalBuilder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Principal")));
    }

    @Override
    public Mono<OffsetPage<PrincipalBO>> list(Long tenantId, PrincipalFilter filter) {
        return principalStore
                .list(tenantId, filter)
                .map(page -> OffsetPage.of(
                        page.items().stream().map(principalBuilder::buildBOByDO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }

    @Override
    public Flux<PrincipalBO> listByIds(Long tenantId, Collection<Long> ids) {
        return principalStore.listByIds(tenantId, ids).map(principalBuilder::buildBOByDO);
    }

    @Override
    public Mono<PrincipalBO> setEnableFlag(
            Long tenantId, Long id, EnableFlagEnum target, Long operatorId, String operatorName) {
        if (tenantId == null || tenantId <= 0 || id == null || id <= 0 || target == null)
            return Mono.error(new RequestException("Principal update is invalid"));
        return principalStore
                .updateEnableFlag(tenantId, id, target.getIndex(), operatorId, operatorName)
                .map(principalBuilder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("Principal")));
    }

    @Override
    public Mono<Boolean> touchLastLogin(Long id) {
        return principalStore.touchLastLogin(id);
    }
}
