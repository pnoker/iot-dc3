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

    /** Page drivers matching the tenant-scoped filters. */
    Mono<OffsetPage<DriverBO>> list(DriverFilter filter);

    /** Load the driver scoped to the tenant by id. */
    Mono<DriverBO> get(Long tenantId, Long id);

    /** Resolve the driver by its name and code. */
    Mono<DriverBO> getByNameAndCode(Long tenantId, String driverName, String driverCode);

    /** Resolve the driver by its service name. */
    Mono<DriverBO> getByServiceName(Long tenantId, String serviceName);

    /** Resolve the driver by its device id. */
    Mono<DriverBO> getByDeviceId(Long tenantId, Long deviceId);

    /** List drivers matched by ids. */
    Flux<DriverBO> listByIds(Long tenantId, List<Long> ids);

    /** List drivers matched by profile id. */
    Flux<DriverBO> listByProfileId(Long tenantId, Long profileId);

    /** List drivers matched by point id. */
    Flux<DriverBO> listByPointId(Long tenantId, Long pointId);

    /** Insert one driver and emit the stored row. */
    Mono<DriverBO> insert(DriverBO driver);

    /** Update one driver and emit the updated row. */
    Mono<DriverBO> update(DriverBO driver, int expectedVersion);

    /** Delete the driver, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
}
