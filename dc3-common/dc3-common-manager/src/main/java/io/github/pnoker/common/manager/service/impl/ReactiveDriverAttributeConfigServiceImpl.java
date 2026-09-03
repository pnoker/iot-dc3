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
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.DriverAttributeConfigFilter;
import io.github.pnoker.common.manager.repository.ReactiveDriverAttributeConfigStore;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default driver attribute config service implementation. */
@Service
@RequiredArgsConstructor
public class ReactiveDriverAttributeConfigServiceImpl implements ReactiveDriverAttributeConfigService {
    private final ReactiveDriverAttributeConfigStore store;
    private final ReactiveDriverAttributeService attributeService;
    private final ReactiveDeviceService deviceService;
    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public Mono<DriverAttributeConfigBO> add(DriverAttributeConfigBO value) {
        return validate(value, false)
                .then(Mono.defer(() -> store.getByAttributeAndDevice(
                        value.getTenantId(), value.getAttributeId(), value.getDeviceId())))
                .flatMap(existing -> Mono.<DriverAttributeConfigBO>error(
                        new DuplicateException("Driver attribute config has been duplicated")))
                .switchIfEmpty(Mono.defer(() -> store.insert(normalize(value, false))))
                .onErrorMap(
                        DataIntegrityViolationException.class,
                        error -> new DuplicateException("Driver attribute config has been duplicated"))
                .doOnNext(saved -> publish(saved.getTenantId(), saved.getDeviceId()));
    }

    @Override
    public Mono<DriverAttributeConfigBO> update(DriverAttributeConfigBO value) {
        return validate(value, true)
                .then(Mono.defer(() -> store.get(value.getTenantId(), value.getId())))
                .switchIfEmpty(Mono.error(new NotFoundException("Driver attribute config does not exist")))
                .flatMap(current -> store.getByAttributeAndDevice(
                                value.getTenantId(), value.getAttributeId(), value.getDeviceId())
                        .filter(existing -> !java.util.Objects.equals(existing.getId(), value.getId()))
                        .flatMap(existing -> Mono.<DriverAttributeConfigBO>error(
                                new DuplicateException("Driver attribute config has been duplicated")))
                        .switchIfEmpty(Mono.defer(() -> store.update(normalize(value, true), value.getVersion()))))
                .switchIfEmpty(Mono.error(new ConflictException("Driver attribute config version conflict")))
                .doOnNext(saved -> publish(saved.getTenantId(), saved.getDeviceId()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        return store.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Driver attribute config does not exist")))
                .flatMap(value -> store.delete(tenantId, id, expectedVersion, operatorId, operatorName)
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(new ConflictException("Driver attribute config version conflict")))
                        .doOnNext(ignored -> publish(value.getTenantId(), value.getDeviceId())));
    }

    @Override
    public Mono<DriverAttributeConfigBO> getById(Long tenantId, Long id) {
        return store.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Driver attribute config does not exist")));
    }

    @Override
    public Mono<DriverAttributeConfigBO> getByAttributeIdAndDeviceId(Long tenantId, Long attributeId, Long deviceId) {
        return store.getByAttributeAndDevice(tenantId, attributeId, deviceId)
                .switchIfEmpty(Mono.error(new NotFoundException("Driver attribute config does not exist")));
    }

    @Override
    public Flux<DriverAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId) {
        return store.listByDeviceId(tenantId, deviceId);
    }

    @Override
    public Mono<OffsetPage<DriverAttributeConfigBO>> list(DriverAttributeConfigFilter filter) {
        return store.list(filter);
    }

    private Mono<Void> validate(DriverAttributeConfigBO value, boolean update) {
        if (value == null
                || value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getAttributeId() == null
                || value.getDeviceId() == null
                || (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0)))
            return Mono.error(new RequestException("tenantId, attributeId and deviceId are required"));
        return Mono.defer(() -> Mono.zip(
                        attributeService.getById(value.getTenantId(), value.getAttributeId()),
                        deviceService.getById(value.getTenantId(), value.getDeviceId())))
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .flatMap(tuple -> {
                    DriverAttributeBO attribute = tuple.getT1();
                    DeviceBO device = tuple.getT2();
                    return java.util.Objects.equals(attribute.getDriverId(), device.getDriverId())
                            ? Mono.empty()
                            : Mono.error(new NotFoundException("Resource does not exist"));
                });
    }

    private DriverAttributeConfigBO normalize(DriverAttributeConfigBO value, boolean update) {
        if (value.getEnableFlag() == null) value.setEnableFlag(io.github.pnoker.common.enums.EnableFlagEnum.ENABLE);
        if (!update && value.getVersion() == null) value.setVersion(0);
        return value;
    }

    private void publish(Long tenantId, Long deviceId) {
        if (tenantId != null && deviceId != null)
            metadataEventPublisher.publishEvent(new MetadataEvent(
                    this, tenantId, deviceId, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE));
    }
}
