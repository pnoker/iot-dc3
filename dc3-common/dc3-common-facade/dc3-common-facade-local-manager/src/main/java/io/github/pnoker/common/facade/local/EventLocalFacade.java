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

import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.query.FacadeEventOffsetQuery;
import io.github.pnoker.common.facade.local.builder.FacadeEventBuilder;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import io.github.pnoker.common.manager.repository.EventFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * In-process EventFacade implementation.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class EventLocalFacade implements EventFacade {

    private final ReactiveEventService reactiveEventService;

    private final FacadeEventBuilder facadeEventBuilder;

    @Override
    public Mono<FacadeEventBO> getById(Long tenantId, Long id) {
        return reactiveEventService.getById(tenantId, id)
                .map(facadeEventBuilder::toFacadeBO)
                .onErrorResume(NotFoundException.class, ignored -> Mono.empty());
    }

    @Override
    public Flux<FacadeEventBO> listByIds(Long tenantId, Collection<Long> ids) {
        return reactiveEventService.listByIds(tenantId, ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList()).map(facadeEventBuilder::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeEventBO>> list(FacadeEventOffsetQuery query) {
        return reactiveEventService.list(new EventFilter(query.tenantId(), query.eventName(), query.eventCode(), query.eventTypeFlag(), query.eventLevelFlag(), query.profileId(), query.enableFlag(), query.version(), query.deviceId(), query.offset(), query.limit(), query.sort()))
                .map(page -> OffsetPage.of(page.items().stream().map(facadeEventBuilder::toFacadeBO).toList(), page.offset(), page.limit(), page.total()));
    }

}
