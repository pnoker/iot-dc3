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
package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.manager.biz.ReactiveDriverRegisterService;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcCommandAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcEventAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactiveEventAttributeService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/** Atomic, tenant-scoped reactive driver registration and attribute reconciliation. */
@Service
@RequiredArgsConstructor
public class ReactiveDriverRegisterServiceImpl implements ReactiveDriverRegisterService {

    private final GrpcDriverBuilder grpcDriverBuilder;
    private final GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;
    private final GrpcPointAttributeBuilder grpcPointAttributeBuilder;
    private final GrpcCommandAttributeBuilder grpcCommandAttributeBuilder;
    private final GrpcEventAttributeBuilder grpcEventAttributeBuilder;
    private final ReactiveDriverService driverService;
    private final ReactiveDriverAttributeService driverAttributeService;
    private final ReactivePointAttributeService pointAttributeService;
    private final ReactiveCommandAttributeService commandAttributeService;
    private final ReactiveEventAttributeService eventAttributeService;
    private final TenantFacade tenantFacade;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<Registration> register(GrpcDriverRegisterDTO request) {
        if (request == null || !request.hasDriver() || request.getTenant().isBlank()) {
            return Mono.error(new ServiceException("Driver registration requires tenant and driver"));
        }
        return tenantFacade
                .getByCode(request.getTenant())
                .switchIfEmpty(Mono.error(new ServiceException("Tenant information is invalid")))
                .flatMap(tenant -> {
                    DriverBO incoming = grpcDriverBuilder.buildBOByGrpcDTO(request.getDriver());
                    if (incoming == null) {
                        return Mono.error(new ServiceException("Driver information is invalid"));
                    }
                    incoming.setTenantId(tenant.getId());
                    return transactionalOperator.transactional(reconcileDriver(incoming, request));
                });
    }

    private Mono<Registration> reconcileDriver(DriverBO incoming, GrpcDriverRegisterDTO request) {
        return driverService
                .getByServiceName(incoming.getTenantId(), incoming.getServiceName())
                .onErrorResume(NotFoundException.class, ignored -> Mono.empty())
                .flatMap(existing -> {
                    incoming.setId(existing.getId());
                    if (incoming.getVersion() == null) incoming.setVersion(existing.getVersion());
                    return driverService.update(incoming);
                })
                .switchIfEmpty(driverService.add(incoming))
                .flatMap(driver -> reconcileDriverAttributes(request, driver)
                        .flatMap(driverAttributes -> reconcilePointAttributes(request, driver)
                                .flatMap(pointAttributes -> reconcileCommandAttributes(request, driver)
                                        .flatMap(commandAttributes -> reconcileEventAttributes(request, driver)
                                                .map(eventAttributes -> new Registration(
                                                        driver,
                                                        driverAttributes,
                                                        pointAttributes,
                                                        commandAttributes,
                                                        eventAttributes))))));
    }

    private Mono<List<DriverAttributeBO>> reconcileDriverAttributes(GrpcDriverRegisterDTO request, DriverBO driver) {
        Map<String, DriverAttributeBO> incoming = request.getDriverAttributesList().stream()
                .collect(Collectors.toMap(
                        value -> value.getAttributeCode(), grpcDriverAttributeBuilder::buildBOByGrpcDTO));
        return driverAttributeService
                .listByDriverId(driver.getTenantId(), driver.getId())
                .collectList()
                .flatMap(existing -> reconcile(
                        incoming,
                        existing,
                        driver,
                        DriverAttributeBO::getAttributeCode,
                        attribute -> {
                            attribute.setDriverId(driver.getId());
                            attribute.setTenantId(driver.getTenantId());
                        },
                        driverAttributeService::saveBatch,
                        driverAttributeService::updateBatch,
                        ids -> driverAttributeService.deleteByIds(driver.getTenantId(), ids, null, null),
                        () -> driverAttributeService
                                .listByDriverId(driver.getTenantId(), driver.getId())
                                .collectList()));
    }

    private Mono<List<PointAttributeBO>> reconcilePointAttributes(GrpcDriverRegisterDTO request, DriverBO driver) {
        Map<String, PointAttributeBO> incoming = request.getPointAttributesList().stream()
                .collect(Collectors.toMap(
                        value -> value.getAttributeCode(), grpcPointAttributeBuilder::buildBOByGrpcDTO));
        return pointAttributeService
                .listByDriverId(driver.getTenantId(), driver.getId())
                .collectList()
                .flatMap(existing -> reconcile(
                        incoming,
                        existing,
                        driver,
                        PointAttributeBO::getAttributeCode,
                        attribute -> {
                            attribute.setDriverId(driver.getId());
                            attribute.setTenantId(driver.getTenantId());
                        },
                        pointAttributeService::saveBatch,
                        pointAttributeService::updateBatch,
                        ids -> pointAttributeService.deleteByIds(driver.getTenantId(), ids, null, null),
                        () -> pointAttributeService
                                .listByDriverId(driver.getTenantId(), driver.getId())
                                .collectList()));
    }

    private Mono<List<CommandAttributeBO>> reconcileCommandAttributes(GrpcDriverRegisterDTO request, DriverBO driver) {
        Map<String, CommandAttributeBO> incoming = request.getCommandAttributesList().stream()
                .collect(Collectors.toMap(
                        value -> value.getAttributeCode(), grpcCommandAttributeBuilder::buildBOByGrpcDTO));
        return commandAttributeService
                .listByDriverId(driver.getTenantId(), driver.getId())
                .collectList()
                .flatMap(existing -> reconcile(
                        incoming,
                        existing,
                        driver,
                        CommandAttributeBO::getAttributeCode,
                        attribute -> {
                            attribute.setDriverId(driver.getId());
                            attribute.setTenantId(driver.getTenantId());
                        },
                        commandAttributeService::saveBatch,
                        commandAttributeService::updateBatch,
                        ids -> commandAttributeService.deleteByIds(driver.getTenantId(), ids, null, null),
                        () -> commandAttributeService
                                .listByDriverId(driver.getTenantId(), driver.getId())
                                .collectList()));
    }

    private Mono<List<EventAttributeBO>> reconcileEventAttributes(GrpcDriverRegisterDTO request, DriverBO driver) {
        Map<String, EventAttributeBO> incoming = request.getEventAttributesList().stream()
                .collect(Collectors.toMap(
                        value -> value.getAttributeCode(), grpcEventAttributeBuilder::buildBOByGrpcDTO));
        return eventAttributeService
                .listByDriverId(driver.getTenantId(), driver.getId())
                .collectList()
                .flatMap(existing -> reconcile(
                        incoming,
                        existing,
                        driver,
                        EventAttributeBO::getAttributeCode,
                        attribute -> {
                            attribute.setDriverId(driver.getId());
                            attribute.setTenantId(driver.getTenantId());
                        },
                        eventAttributeService::saveBatch,
                        eventAttributeService::updateBatch,
                        ids -> eventAttributeService.deleteByIds(driver.getTenantId(), ids, null, null),
                        () -> eventAttributeService
                                .listByDriverId(driver.getTenantId(), driver.getId())
                                .collectList()));
    }

    private <T> Mono<List<T>> reconcile(
            Map<String, T> incoming,
            List<T> existing,
            DriverBO driver,
            Function<T, String> key,
            java.util.function.Consumer<T> normalize,
            Function<List<T>, Mono<List<T>>> save,
            Function<List<T>, Mono<List<T>>> update,
            Function<Set<Long>, Mono<Boolean>> delete,
            Supplier<Mono<List<T>>> reload) {
        Map<String, T> current = existing.stream()
                .filter(value -> Objects.equals(driver.getTenantId(), tenantId(value)))
                .collect(Collectors.toMap(key, Function.identity(), (left, right) -> left));
        List<T> inserts = new ArrayList<>();
        List<T> updates = new ArrayList<>();
        incoming.forEach((code, value) -> {
            normalize.accept(value);
            T old = current.get(code);
            if (old != null) {
                setIdAndVersion(value, old);
                updates.add(value);
            } else {
                inserts.add(value);
            }
        });
        Set<Long> removed = current.entrySet().stream()
                .filter(entry -> !incoming.containsKey(entry.getKey()))
                .map(entry -> id(entry.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        return save.apply(inserts)
                .then(update.apply(updates))
                .then(delete.apply(removed))
                .then(reload.get());
    }

    private Long tenantId(Object value) {
        return value instanceof io.github.pnoker.common.entity.common.TenantOwned owned ? owned.getTenantId() : null;
    }

    private Long id(Object value) {
        return value instanceof io.github.pnoker.common.entity.base.BaseBO base ? base.getId() : null;
    }

    private void setIdAndVersion(Object value, Object old) {
        if (value instanceof io.github.pnoker.common.entity.base.BaseBO target
                && old instanceof io.github.pnoker.common.entity.base.BaseBO source) {
            target.setId(source.getId());
        }
        if (value instanceof DriverAttributeBO target
                && old instanceof DriverAttributeBO source
                && target.getVersion() == null) target.setVersion(source.getVersion());
        if (value instanceof PointAttributeBO target
                && old instanceof PointAttributeBO source
                && target.getVersion() == null) target.setVersion(source.getVersion());
        if (value instanceof CommandAttributeBO target
                && old instanceof CommandAttributeBO source
                && target.getVersion() == null) target.setVersion(source.getVersion());
        if (value instanceof EventAttributeBO target
                && old instanceof EventAttributeBO source
                && target.getVersion() == null) target.setVersion(source.getVersion());
    }
}
