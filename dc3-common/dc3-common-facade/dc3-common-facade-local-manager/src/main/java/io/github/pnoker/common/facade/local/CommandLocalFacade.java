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

import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.query.FacadeCommandOffsetQuery;
import io.github.pnoker.common.facade.local.builder.FacadeCommandBuilder;
import io.github.pnoker.common.manager.repository.CommandFilter;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * In-process CommandFacade implementation.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class CommandLocalFacade implements CommandFacade {

    private final ReactiveCommandService reactiveCommandService;

    private final FacadeCommandBuilder facadeCommandBuilder;

    @Override
    public Mono<FacadeCommandBO> getById(Long tenantId, Long id) {
        return reactiveCommandService
                .getById(tenantId, id)
                .map(facadeCommandBuilder::toFacadeBO)
                .onErrorResume(NotFoundException.class, ignored -> Mono.empty());
    }

    @Override
    public Flux<FacadeCommandBO> listByIds(Long tenantId, Collection<Long> ids) {
        return reactiveCommandService
                .listByIds(
                        tenantId,
                        ids == null
                                ? List.of()
                                : ids.stream()
                                        .filter(Objects::nonNull)
                                        .distinct()
                                        .toList())
                .map(facadeCommandBuilder::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeCommandBO>> list(FacadeCommandOffsetQuery query) {
        return reactiveCommandService
                .list(new CommandFilter(
                        query.tenantId(),
                        query.commandName(),
                        query.commandCode(),
                        query.commandTypeFlag(),
                        query.callTypeFlag(),
                        query.profileId(),
                        query.enableFlag(),
                        query.version(),
                        query.deviceId(),
                        query.offset(),
                        query.limit(),
                        query.sort()))
                .map(page -> OffsetPage.of(
                        page.items().stream()
                                .map(facadeCommandBuilder::toFacadeBO)
                                .toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }
}
