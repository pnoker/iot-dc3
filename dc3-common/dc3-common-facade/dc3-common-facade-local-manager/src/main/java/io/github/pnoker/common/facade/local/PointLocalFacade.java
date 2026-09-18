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

import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.query.FacadePointOffsetQuery;
import io.github.pnoker.common.facade.local.builder.FacadePointBuilder;
import io.github.pnoker.common.manager.repository.PointFilter;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * In-process PointFacade implementation. Canonical reactive methods route directly to
 * the manager R2DBC service without using tenant thread-locals.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
public class PointLocalFacade implements PointFacade {

    private final ReactivePointService reactivePointService;

    private final FacadePointBuilder facadePointBuilder;

    /** Constructor used by the Spring local facade. */
    @Autowired
    public PointLocalFacade(ReactivePointService reactivePointService, FacadePointBuilder facadePointBuilder) {
        this.reactivePointService = reactivePointService;
        this.facadePointBuilder = facadePointBuilder;
    }

    /** Constructor retained for focused unit tests of the synchronous legacy methods. */
    @Override
    public Mono<FacadePointBO> getByIdReactive(Long tenantId, Long id) {
        if (reactivePointService == null) {
            return Mono.error(new IllegalStateException("Reactive point service is not configured"));
        }
        return reactivePointService.getById(tenantId, id).map(facadePointBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadePointBO> listByIdsReactive(Long tenantId, Collection<Long> ids) {
        if (reactivePointService == null || ids == null || ids.isEmpty()) {
            return Flux.empty();
        }
        List<Long> pointIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        return reactivePointService.listByIds(tenantId, pointIds).map(facadePointBuilder::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadePointBO>> listReactive(FacadePointOffsetQuery query) {
        if (reactivePointService == null) {
            return Mono.error(new IllegalStateException("Reactive point service is not configured"));
        }
        PointFilter filter = new PointFilter(
                query.tenantId(),
                query.pointName(),
                query.pointCode(),
                query.pointTypeFlag(),
                query.rwFlag(),
                query.profileId(),
                query.enableFlag(),
                query.groupId(),
                query.labelId(),
                query.version(),
                query.deviceId(),
                query.offset(),
                query.limit(),
                query.sort());
        return reactivePointService
                .list(filter)
                .map(page -> OffsetPage.of(
                        page.items().stream()
                                .map(facadePointBuilder::toFacadeBO)
                                .toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }
}
