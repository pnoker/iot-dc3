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

import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.query.FacadeProfileOffsetQuery;
import io.github.pnoker.common.facade.local.builder.FacadeProfileBuilder;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.common.manager.service.ReactiveProfileService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * In-process ProfileFacade implementation.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
public class ProfileLocalFacade implements ProfileFacade {

    private final ReactiveProfileService reactiveProfileService;

    private final FacadeProfileBuilder facadeProfileBuilder;

    @org.springframework.beans.factory.annotation.Autowired
    public ProfileLocalFacade(
            ReactiveProfileService reactiveProfileService, FacadeProfileBuilder facadeProfileBuilder) {
        this.reactiveProfileService = reactiveProfileService;
        this.facadeProfileBuilder = facadeProfileBuilder;
    }

    @Override
    public Mono<FacadeProfileBO> getByIdReactive(Long tenantId, Long id) {
        return reactiveProfileService.getById(tenantId, id).map(facadeProfileBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeProfileBO> listByIdsReactive(Long tenantId, Collection<Long> ids) {
        List<Long> normalized = ids == null
                ? List.of()
                : ids.stream()
                        .filter(value -> value != null && value > 0)
                        .distinct()
                        .toList();
        return reactiveProfileService.listByIds(tenantId, normalized).map(facadeProfileBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeProfileBO> listByDeviceIdReactive(Long tenantId, Long deviceId) {
        return reactiveProfileService.listByDeviceId(tenantId, deviceId).map(facadeProfileBuilder::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeProfileBO>> listReactive(FacadeProfileOffsetQuery query) {
        ProfileFilter filter = new ProfileFilter(
                query.tenantId(),
                query.profileName(),
                query.profileCode(),
                query.profileShareFlag(),
                query.profileTypeFlag(),
                query.enableFlag(),
                query.groupId(),
                query.labelId(),
                query.version(),
                query.deviceId(),
                query.offset(),
                query.limit(),
                query.sort());
        return reactiveProfileService
                .list(filter)
                .map(page -> OffsetPage.of(
                        page.items().stream()
                                .map(facadeProfileBuilder::toFacadeBO)
                                .toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }
}
