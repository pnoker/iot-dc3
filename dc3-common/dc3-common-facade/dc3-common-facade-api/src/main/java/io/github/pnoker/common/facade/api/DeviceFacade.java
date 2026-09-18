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
package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Protocol-neutral device facade.
 * <p>
 * Mirrors the RPCs on {@code api.center.manager.DeviceApi} but returns plain BO/Page
 * types so callers never have to touch gRPC or protobuf classes. Two implementations back
 * this interface:
 * <ul>
 * <li>{@code DeviceLocalFacade} — in-process call into {@code DeviceService}, selected
 * when {@code dc3.facade.manager.mode=local}.</li>
 * <li>{@code DeviceGrpcFacade} — gRPC call against Manager Center, selected when
 * {@code dc3.facade.manager.mode=grpc} (default).</li>
 * </ul>
 * <p>
 * Single-record and bulk lookups are tenant-scoped: the tenant id rides on the gRPC query
 * (or the thread-local in local mode) so the manager center enforces tenant isolation.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface DeviceFacade {
    /** Resolve the device by its id. */
    Mono<FacadeDeviceBO> getByIdReactive(Long tenantId, Long id);

    /** List devices matched by ids. */
    Flux<FacadeDeviceBO> listByIdsReactive(Long tenantId, Collection<Long> ids);

    /** Page devices matching the tenant-scoped filters. */
    Mono<OffsetPage<FacadeDeviceBO>> listReactive(FacadeDeviceOffsetQuery query);

    /** List devices matched by profile id. */
    Flux<FacadeDeviceBO> listByProfileIdReactive(Long tenantId, Long profileId);

    /** List devices matched by driver id. */
    Flux<FacadeDeviceBO> listByDriverIdReactive(Long tenantId, Long driverId);

    /** Load the active owner scoped to the tenant by id. */
    Mono<FacadeDeviceOwnerBO> getActiveOwnerReactive(Long tenantId, Long deviceId);
}
