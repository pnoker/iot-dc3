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

import io.github.pnoker.common.manager.entity.operation.DeviceImportJob;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Reactive persistence port for device import job records. */

public interface ReactiveDeviceImportJobStore {

    /** Insert one device import job and emit the stored row. */
    Mono<Void> insert(DeviceImportJob job);

    /** Claim the import job for the worker until the given instant. */
    Mono<DeviceImportJob> claim(UUID operationId, String workerId, Instant now, Instant claimedUntil);

    /** Renew the worker's claim on the import job. */
    Mono<Boolean> renew(UUID operationId, String workerId, Instant claimedUntil);

    /** List operation ids whose import jobs can be recovered. */
    Flux<UUID> listRecoverable(Instant now);

    /** Delete the device import job. */
    Mono<Void> delete(UUID operationId, Long tenantId);
}
