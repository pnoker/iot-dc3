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

import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive application service for driver metadata. */
public interface ReactiveDriverService {

    Mono<DriverBO> add(DriverBO driver);

    Mono<DriverBO> update(DriverBO driver);

    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    Mono<DriverBO> getById(Long tenantId, Long id);

    Mono<DriverBO> getByServiceName(Long tenantId, String serviceName);

    Mono<DriverBO> getByDeviceId(Long tenantId, Long deviceId);

    Mono<OffsetPage<DriverBO>> list(DriverFilter filter);

    Flux<DriverBO> listByIds(Long tenantId, List<Long> ids);

    Flux<DriverBO> listByProfileId(Long tenantId, Long profileId);

    Flux<DriverBO> listByPointId(Long tenantId, Long pointId);
}
