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
package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.DriverAttributeFilter;
import io.github.pnoker.common.manager.repository.ReactiveDriverAttributeStore;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default reactive driver attribute application service. */
@Service
@RequiredArgsConstructor
public class ReactiveDriverAttributeServiceImpl implements ReactiveDriverAttributeService {
    private final ReactiveDriverAttributeStore store;
    private final ReactiveDriverService driverService;
    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public Mono<DriverAttributeBO> add(DriverAttributeBO value) {
        return Mono.defer(() -> {
            validate(value, false);
            return ensureDriver(value)
                    .then(store.getByCodeAndDriver(value.getTenantId(), value.getAttributeCode(), value.getDriverId()))
                    .flatMap(existing -> Mono.<DriverAttributeBO>error(
                            new DuplicateException("Command attribute has been duplicated")))
                    .switchIfEmpty(Mono.defer(() -> store.insert(normalize(value, false))))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Command attribute has been duplicated"))
                    .doOnSuccess(saved -> publish(saved.getTenantId(), saved.getDriverId()));
        });
    }

    @Override
    public Mono<DriverAttributeBO> update(DriverAttributeBO value) {
        return Mono.defer(() -> {
            validate(value, true);
            return store.get(value.getTenantId(), value.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Command attribute does not exist")))
                    .flatMap(current -> ensureDriver(value)
                            .then(store.getByCodeAndDriver(
                                    value.getTenantId(), value.getAttributeCode(), value.getDriverId()))
                            .filter(existing -> !existing.getId().equals(value.getId()))
                            .flatMap(existing -> Mono.<DriverAttributeBO>error(
                                    new DuplicateException("Command attribute has been duplicated")))
                            .switchIfEmpty(Mono.defer(() -> store.update(normalize(value, true), value.getVersion()))))
                    .switchIfEmpty(Mono.error(new ConflictException("Driver attribute version conflict")))
                    .doOnSuccess(saved -> publish(saved.getTenantId(), saved.getDriverId()));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        return store.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Command attribute does not exist")))
                .flatMap(value -> store.delete(tenantId, id, expectedVersion, operatorId, operatorName)
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(new ConflictException("Driver attribute version conflict")))
                        .doOnSuccess(ignored -> publish(value.getTenantId(), value.getDriverId())));
    }

    @Override
    public Mono<DriverAttributeBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and attribute ID are required"));
        return store.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Command attribute does not exist")));
    }

    @Override
    public Mono<DriverAttributeBO> getByNameAndDriverId(Long tenantId, String name, Long driverId) {
        if (tenantId == null || driverId == null || name == null || name.isBlank()) return Mono.empty();
        return store.getByCodeAndDriver(tenantId, name, driverId);
    }

    @Override
    public Flux<DriverAttributeBO> listByDriverId(Long tenantId, Long driverId) {
        return store.listByDriverId(tenantId, driverId);
    }

    @Override
    public Mono<OffsetPage<DriverAttributeBO>> list(DriverAttributeFilter filter) {
        return store.list(filter);
    }

    @Override
    public Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName) {
        return store.deleteByIds(tenantId, ids, operatorId, operatorName);
    }

    @Override
    public Mono<List<DriverAttributeBO>> saveBatch(List<DriverAttributeBO> values) {
        if (values == null || values.isEmpty()) return Mono.just(List.of());
        return Flux.fromIterable(values).concatMap(this::add).collectList();
    }

    @Override
    public Mono<List<DriverAttributeBO>> updateBatch(List<DriverAttributeBO> values) {
        if (values == null || values.isEmpty()) return Mono.just(List.of());
        return Flux.fromIterable(values).concatMap(this::update).collectList();
    }

    private Mono<Void> ensureDriver(DriverAttributeBO value) {
        return driverService.getById(value.getTenantId(), value.getDriverId()).then();
    }

    private void validate(DriverAttributeBO value, boolean update) {
        if (value == null
                || value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getDriverId() == null
                || value.getDriverId() <= 0
                || value.getAttributeName() == null
                || value.getAttributeName().isBlank()
                || value.getAttributeCode() == null
                || value.getAttributeCode().isBlank()) {
            throw new RequestException("Tenant ID, driver ID, attribute name and attribute code are required");
        }
        if (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0)) {
            throw new RequestException("Attribute ID and version are required for update");
        }
        value.setAttributeName(value.getAttributeName().trim());
        value.setAttributeCode(value.getAttributeCode().trim());
    }

    private DriverAttributeBO normalize(DriverAttributeBO value, boolean update) {
        if (value.getAttributeTypeFlag() == null)
            value.setAttributeTypeFlag(io.github.pnoker.common.enums.AttributeTypeEnum.STRING);
        if (value.getEnableFlag() == null) value.setEnableFlag(io.github.pnoker.common.enums.EnableFlagEnum.ENABLE);
        if (!update && value.getVersion() == null) value.setVersion(0);
        return value;
    }

    private void publish(Long tenantId, Long driverId) {
        if (tenantId != null && driverId != null)
            metadataEventPublisher.publishEvent(new MetadataEvent(
                    this, tenantId, driverId, MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.UPDATE));
    }
}
