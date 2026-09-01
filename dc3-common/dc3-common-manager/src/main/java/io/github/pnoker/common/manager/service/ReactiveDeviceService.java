/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive device application service. */
public interface ReactiveDeviceService {

    Mono<DeviceBO> add(DeviceBO device);

    Mono<DeviceBO> update(DeviceBO device);

    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    Mono<DeviceBO> getById(Long tenantId, Long id);

    Mono<OffsetPage<DeviceBO>> list(DeviceFilter filter);

    Flux<DeviceBO> listByDriverId(Long tenantId, Long driverId);

    Flux<DeviceBO> listByProfileId(Long tenantId, Long profileId);

    Flux<DeviceBO> listByIds(Long tenantId, List<Long> ids);
}
