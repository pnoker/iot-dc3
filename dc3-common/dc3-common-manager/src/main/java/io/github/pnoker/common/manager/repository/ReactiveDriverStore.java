/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 */
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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
