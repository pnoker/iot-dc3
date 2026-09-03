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
package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.repository.CommandAttributeFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive command attribute application service. */
public interface ReactiveCommandAttributeService {
    /** Add one command attribute. */
    Mono<CommandAttributeBO> add(CommandAttributeBO value);

    /** Update one command attribute and emit the updated row. */
    Mono<CommandAttributeBO> update(CommandAttributeBO value);

    /** Delete the command attribute, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Resolve the command attribute by its id. */
    Mono<CommandAttributeBO> getById(Long tenantId, Long id);

    /** Resolve the command attribute by its name and driver id. */
    Mono<CommandAttributeBO> getByNameAndDriverId(Long tenantId, String name, Long driverId);

    /** List command attributes matched by driver id. */
    Flux<CommandAttributeBO> listByDriverId(Long tenantId, Long driverId);

    /** Page command attributes matching the tenant-scoped filters. */
    Mono<OffsetPage<CommandAttributeBO>> list(CommandAttributeFilter filter);

    /** Delete the records matched by ids. */
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);

    /** Save the batch, inserting or updating as needed. */
    Mono<List<CommandAttributeBO>> saveBatch(List<CommandAttributeBO> values);

    /** Update one batch and emit the updated row. */
    Mono<List<CommandAttributeBO>> updateBatch(List<CommandAttributeBO> values);
}
