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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped drivers. */
public interface ReactiveDriverStore {

    Mono<OffsetPage<DriverBO>> list(DriverFilter filter);

    Mono<DriverBO> get(Long tenantId, Long id);

    Mono<DriverBO> getByNameAndCode(Long tenantId, String driverName, String driverCode);

    Mono<DriverBO> getByServiceName(Long tenantId, String serviceName);

    Mono<DriverBO> getByDeviceId(Long tenantId, Long deviceId);

    Flux<DriverBO> listByIds(Long tenantId, List<Long> ids);

    Flux<DriverBO> listByProfileId(Long tenantId, Long profileId);

    Flux<DriverBO> listByPointId(Long tenantId, Long pointId);

    Mono<DriverBO> insert(DriverBO driver);

    Mono<DriverBO> update(DriverBO driver, int expectedVersion);

    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
}
