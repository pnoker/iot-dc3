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

import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.operation.DeviceImportManifest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Business service covering device import schema use cases. */
@Service
@RequiredArgsConstructor
public class DeviceImportSchemaService {

    private final ReactiveDriverService driverService;
    private final ReactiveProfileService profileService;
    private final ReactiveDriverAttributeService driverAttributeService;
    private final ReactivePointAttributeService pointAttributeService;
    private final ReactivePointService pointService;
    private final DeviceImportWorkbookCodec workbookCodec;

    /** Load the device import manifest for the driver/profile pair. */
    public Mono<DeviceImportManifest> load(Long tenantId, Long driverId, Long profileId) {
        if (tenantId == null || driverId == null || profileId == null) {
            return Mono.error(new RequestException("Tenant ID, driver ID and profile ID are required"));
        }
        return Mono.zip(driverService.getById(tenantId, driverId), profileService.getById(tenantId, profileId))
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .then(Mono.zip(
                        driverAttributeService
                                .listByDriverId(tenantId, driverId)
                                .collectList(),
                        pointAttributeService.listByDriverId(tenantId, driverId).collectList(),
                        pointService.listByProfileId(tenantId, profileId).collectList()))
                .map(tuple -> workbookCodec.manifest(driverId, profileId, tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }
}
