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

package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.local.builder.FacadeDriverBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * In-process DriverFacade implementation.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
public class DriverLocalFacade implements DriverFacade {


    private final FacadeDriverBuilder facadeDriverBuilder;

    private final ReactiveDriverService reactiveDriverService;

    @org.springframework.beans.factory.annotation.Autowired
    public DriverLocalFacade(FacadeDriverBuilder facadeDriverBuilder, ReactiveDriverService reactiveDriverService) {
        this.facadeDriverBuilder = facadeDriverBuilder;
        this.reactiveDriverService = reactiveDriverService;
    }


    @Override public Mono<FacadeDriverBO> getByIdReactive(Long tenantId, Long id) {
        if (reactiveDriverService == null) return Mono.error(new IllegalStateException("ReactiveDriverService is not configured"));
        return reactiveDriverService.getById(tenantId, id).map(facadeDriverBuilder::toFacadeBO);
    }
    @Override public Flux<FacadeDriverBO> listByIdsReactive(Long tenantId, Collection<Long> ids) {
        if (reactiveDriverService == null) return Flux.error(new IllegalStateException("ReactiveDriverService is not configured"));
        return reactiveDriverService.listByIds(tenantId, ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList()).map(facadeDriverBuilder::toFacadeBO);
    }
    @Override public Mono<OffsetPage<FacadeDriverBO>> listReactive(io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery query) {
        if (reactiveDriverService == null) return Mono.error(new IllegalStateException("ReactiveDriverService is not configured"));
        return reactiveDriverService.list(new DriverFilter(query.tenantId(), query.driverName(), query.driverCode(), query.serviceName(), query.serviceHost(), query.driverTypeFlag(), query.enableFlag(), query.version(), query.groupId(), query.labelId(), query.offset(), query.limit(), query.sort())).map(page -> OffsetPage.of(page.items().stream().map(facadeDriverBuilder::toFacadeBO).toList(), page.offset(), page.limit(), page.total()));
    }


}
