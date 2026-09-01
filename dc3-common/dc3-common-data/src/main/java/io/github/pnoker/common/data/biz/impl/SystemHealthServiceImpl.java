/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.SystemHealthService;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.data.entity.vo.dashboard.SystemHealthVO;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reactive health aggregation for platform dependencies and tenant fleet status. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemHealthServiceImpl implements SystemHealthService {
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);
    private static final String CENTER_AUTH = "auth";
    private static final String CENTER_DATA = "data";
    private static final String CENTER_MANAGER = "manager";
    private static final String INFRA_DATABASE = "database";
    private static final String INFRA_MQ = "mq";
    private static final String INFRA_GATEWAY = "gateway";

    private final DatabaseClient databaseClient;
    private final ConnectionFactory rabbitConnectionFactory;
    private final TenantFacade tenantFacade;
    private final DriverFacade driverFacade;
    private final DeviceFacade deviceFacade;
    private final ReactiveEntityStateStore entityStateStore;
    private final ExecutorService connectorExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final reactor.core.scheduler.Scheduler connectorScheduler = Schedulers.fromExecutorService(connectorExecutor);

    @Override
    public Mono<SystemHealthVO> snapshot(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            return Mono.error(new IllegalArgumentException("tenantId must be positive"));
        }
        return Mono.zip(probeCenter(tenantId), probeInfra(), fleetDrivers(tenantId), fleetDevices(tenantId))
                .map(tuple -> {
                    SystemHealthVO health = new SystemHealthVO();
                    health.setCenter(tuple.getT1());
                    health.setInfra(tuple.getT2());
                    health.setDrivers(tuple.getT3());
                    health.setDevices(tuple.getT4());
                    return health;
                });
    }

    private Mono<Map<String, String>> probeCenter(Long tenantId) {
        Mono<String> auth = probe(() -> tenantFacade.getByCode(DefaultFlagEnum.DEFAULT.getCode()).map(value -> value != null));
        Mono<String> manager = probe(() -> driverFacade.listReactive(new FacadeDriverOffsetQuery(
                tenantId, null, null, null, null, null, null, null, null, null, 0, 1, List.of())).map(page -> page.total() >= 0));
        return Mono.zip(auth, manager).map(tuple -> {
            Map<String, String> result = new LinkedHashMap<>();
            result.put(CENTER_AUTH, tuple.getT1());
            result.put(CENTER_DATA, DataConstant.Health.STATUS_UP);
            result.put(CENTER_MANAGER, tuple.getT2());
            return Map.copyOf(result);
        });
    }

    private Mono<Map<String, String>> probeInfra() {
        Mono<String> database = probe(() -> databaseClient.sql("SELECT 1 AS healthy")
                .map((row, metadata) -> row.get("healthy", Number.class))
                .one()
                .map(value -> value != null && value.intValue() == 1));
        Mono<String> mq = probe(() -> {
            if (rabbitConnectionFactory == null) {
                return Mono.just(false);
            }
            return Mono.fromCallable(() -> {
                try (var connection = rabbitConnectionFactory.createConnection()) {
                    return connection != null && connection.isOpen();
                }
            }).subscribeOn(connectorScheduler);
        });
        return Mono.zip(database, mq).map(tuple -> {
            Map<String, String> result = new LinkedHashMap<>();
            result.put(INFRA_DATABASE, tuple.getT1());
            result.put(INFRA_MQ, tuple.getT2());
            result.put(INFRA_GATEWAY, DataConstant.Health.STATUS_UP);
            return Map.copyOf(result);
        });
    }

    private Mono<SystemHealthVO.FleetSummary> fleetDrivers(Long tenantId) {
        FacadeDriverOffsetQuery query = new FacadeDriverOffsetQuery(
                tenantId, null, null, null, null, null, null, null, null, null, 0, 1, List.of());
        return driverFacade.listReactive(query)
                .flatMap(page -> fleetSummary(tenantId, EntityTypeEnum.DRIVER, page.total()))
                .defaultIfEmpty(new SystemHealthVO.FleetSummary())
                .timeout(PROBE_TIMEOUT)
                .onErrorResume(error -> degraded(error, "driver"));
    }

    private Mono<SystemHealthVO.FleetSummary> fleetDevices(Long tenantId) {
        FacadeDeviceOffsetQuery query = new FacadeDeviceOffsetQuery(
                tenantId, null, null, null, null, null, null, null, null, 0, 1, List.of());
        return deviceFacade.listReactive(query)
                .flatMap(page -> fleetSummary(tenantId, EntityTypeEnum.DEVICE, page.total()))
                .defaultIfEmpty(new SystemHealthVO.FleetSummary())
                .timeout(PROBE_TIMEOUT)
                .onErrorResume(error -> degraded(error, "device"));
    }

    private Mono<SystemHealthVO.FleetSummary> fleetSummary(Long tenantId, EntityTypeEnum type, long total) {
        return entityStateStore.countOnline(tenantId, type).map(online -> {
            SystemHealthVO.FleetSummary summary = new SystemHealthVO.FleetSummary();
            summary.setTotal((int) Math.min(Integer.MAX_VALUE, total));
            summary.setOnline((int) Math.min(Integer.MAX_VALUE, online));
            return summary;
        }).defaultIfEmpty(new SystemHealthVO.FleetSummary());
    }

    private Mono<SystemHealthVO.FleetSummary> degraded(Throwable error, String component) {
        log.debug("{} health probe degraded", component, error);
        SystemHealthVO.FleetSummary summary = new SystemHealthVO.FleetSummary();
        return Mono.just(summary);
    }

    private Mono<String> probe(java.util.function.Supplier<Mono<Boolean>> check) {
        return Mono.defer(check)
                .defaultIfEmpty(false)
                .timeout(PROBE_TIMEOUT)
                .map(up -> Boolean.TRUE.equals(up) ? DataConstant.Health.STATUS_UP : DataConstant.Health.STATUS_DOWN)
                .onErrorReturn(DataConstant.Health.STATUS_DOWN);
    }

    private Mono<String> probe(java.util.function.BooleanSupplier check) {
        return Mono.defer(() -> Mono.just(check.getAsBoolean()))
                .timeout(PROBE_TIMEOUT)
                .map(up -> up ? DataConstant.Health.STATUS_UP : DataConstant.Health.STATUS_DOWN)
                .onErrorReturn(DataConstant.Health.STATUS_DOWN);
    }

    @PreDestroy
    void shutdownConnectorExecutor() {
        connectorScheduler.dispose();
        connectorExecutor.shutdownNow();
    }
}
