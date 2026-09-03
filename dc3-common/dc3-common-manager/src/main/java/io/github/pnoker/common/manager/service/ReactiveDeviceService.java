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

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive device application service. */
public interface ReactiveDeviceService {

    /** Add one device. */
    Mono<DeviceBO> add(DeviceBO device);

    /** Update one device and emit the updated row. */
    Mono<DeviceBO> update(DeviceBO device);

    /** Delete the device, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Resolve the device by its id. */
    Mono<DeviceBO> getById(Long tenantId, Long id);

    /** Page devices matching the tenant-scoped filters. */
    Mono<OffsetPage<DeviceBO>> list(DeviceFilter filter);

    /** List devices matched by driver id. */
    Flux<DeviceBO> listByDriverId(Long tenantId, Long driverId);

    /** List devices matched by profile id. */
    Flux<DeviceBO> listByProfileId(Long tenantId, Long profileId);

    /** List devices matched by ids. */
    Flux<DeviceBO> listByIds(Long tenantId, List<Long> ids);
}
