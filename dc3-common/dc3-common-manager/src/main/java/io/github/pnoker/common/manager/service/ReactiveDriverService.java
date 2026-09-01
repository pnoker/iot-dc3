/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 */
package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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
