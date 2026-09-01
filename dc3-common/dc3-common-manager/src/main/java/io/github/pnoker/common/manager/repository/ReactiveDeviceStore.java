/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive persistence port for tenant-scoped devices. */
public interface ReactiveDeviceStore {

    Mono<OffsetPage<DeviceBO>> list(DeviceFilter filter);

    Mono<DeviceBO> get(Long tenantId, Long id);

    Mono<DeviceBO> getByName(Long tenantId, String deviceName);

    Mono<DeviceBO> getByCode(Long tenantId, String deviceCode);

    Flux<DeviceBO> listByDriverId(Long tenantId, Long driverId);

    Flux<DeviceBO> listByProfileId(Long tenantId, Long profileId);

    Flux<DeviceBO> listByIds(Long tenantId, List<Long> ids);

    Mono<DeviceBO> insert(DeviceBO device);

    Mono<DeviceBO> update(DeviceBO device, int expectedVersion);

    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
}
