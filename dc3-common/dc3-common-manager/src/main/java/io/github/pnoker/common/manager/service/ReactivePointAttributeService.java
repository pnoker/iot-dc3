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

import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.repository.PointAttributeFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive point attribute application service. */
public interface ReactivePointAttributeService {
    Mono<PointAttributeBO> add(PointAttributeBO value);

    Mono<PointAttributeBO> update(PointAttributeBO value);

    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    Mono<PointAttributeBO> getById(Long tenantId, Long id);

    Mono<PointAttributeBO> getByNameAndDriverId(Long tenantId, String name, Long driverId);

    Flux<PointAttributeBO> listByDriverId(Long tenantId, Long driverId);

    Mono<OffsetPage<PointAttributeBO>> list(PointAttributeFilter filter);

    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);

    Mono<List<PointAttributeBO>> saveBatch(List<PointAttributeBO> values);

    Mono<List<PointAttributeBO>> updateBatch(List<PointAttributeBO> values);
}
