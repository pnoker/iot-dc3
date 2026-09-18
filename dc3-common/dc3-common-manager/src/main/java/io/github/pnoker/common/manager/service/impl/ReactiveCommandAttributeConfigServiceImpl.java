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
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.CommandAttributeConfigFilter;
import io.github.pnoker.common.manager.repository.ReactiveCommandAttributeConfigStore;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeService;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive application service for per-device command attribute configuration. */
@Service
@RequiredArgsConstructor
public class ReactiveCommandAttributeConfigServiceImpl implements ReactiveCommandAttributeConfigService {
    private final ReactiveCommandAttributeConfigStore store;
    private final ReactiveCommandAttributeService attributeService;
    private final ReactiveDeviceService deviceService;
    private final ReactiveCommandService commandService;
    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public Mono<CommandAttributeConfigBO> add(CommandAttributeConfigBO value) {
        return Mono.defer(() -> validate(value, false)
                .then(Mono.defer(() -> store.getByAttributeDeviceCommand(
                        value.getTenantId(), value.getAttributeId(), value.getDeviceId(), value.getCommandId())))
                .flatMap(existing -> Mono.<CommandAttributeConfigBO>error(
                        new DuplicateException("Command attribute config has been duplicated")))
                .switchIfEmpty(Mono.defer(() -> store.insert(normalize(value, false))))
                .onErrorMap(
                        DataIntegrityViolationException.class,
                        error -> new DuplicateException("Command attribute config has been duplicated"))
                .doOnNext(saved -> publish(saved.getTenantId(), saved.getDeviceId())));
    }

    @Override
    public Mono<CommandAttributeConfigBO> update(CommandAttributeConfigBO value) {
        return Mono.defer(() -> validate(value, true)
                .then(Mono.defer(() -> store.get(value.getTenantId(), value.getId())))
                .switchIfEmpty(Mono.error(new NotFoundException("Command attribute config does not exist")))
                .flatMap(current -> store.getByAttributeDeviceCommand(
                                value.getTenantId(), value.getAttributeId(), value.getDeviceId(), value.getCommandId())
                        .filter(existing -> !java.util.Objects.equals(existing.getId(), value.getId()))
                        .flatMap(existing -> Mono.<CommandAttributeConfigBO>error(
                                new DuplicateException("Command attribute config has been duplicated")))
                        .switchIfEmpty(Mono.defer(() -> store.update(normalize(value, true), value.getVersion()))))
                .switchIfEmpty(Mono.error(new ConflictException("Command attribute config version conflict")))
                .doOnNext(saved -> publish(saved.getTenantId(), saved.getDeviceId())));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        return store.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Command attribute config does not exist")))
                .flatMap(value -> store.delete(tenantId, id, expectedVersion, operatorId, operatorName)
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(new ConflictException("Command attribute config version conflict")))
                        .doOnNext(ignored -> publish(value.getTenantId(), value.getDeviceId())));
    }

    @Override
    public Mono<CommandAttributeConfigBO> getById(Long tenantId, Long id) {
        return store.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Command attribute config does not exist")));
    }

    @Override
    public Mono<CommandAttributeConfigBO> getByAttributeIdAndDeviceIdAndCommandId(
            Long tenantId, Long attributeId, Long deviceId, Long commandId) {
        return store.getByAttributeDeviceCommand(tenantId, attributeId, deviceId, commandId)
                .switchIfEmpty(Mono.error(new NotFoundException("Command attribute config does not exist")));
    }

    @Override
    public Flux<CommandAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId) {
        return store.listByDeviceId(tenantId, deviceId);
    }

    @Override
    public Flux<CommandAttributeConfigBO> listByDeviceIdAndCommandId(Long tenantId, Long deviceId, Long commandId) {
        return store.listByDeviceIdAndCommandId(tenantId, deviceId, commandId);
    }

    @Override
    public Mono<OffsetPage<CommandAttributeConfigBO>> list(CommandAttributeConfigFilter filter) {
        return store.list(filter);
    }

    private Mono<Void> validate(CommandAttributeConfigBO value, boolean update) {
        if (value == null
                || value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getAttributeId() == null
                || value.getDeviceId() == null
                || value.getCommandId() == null
                || (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0)))
            return Mono.error(new RequestException("tenantId, attributeId, deviceId and commandId are required"));
        return Mono.defer(() -> Mono.zip(
                        attributeService.getById(value.getTenantId(), value.getAttributeId()),
                        deviceService.getById(value.getTenantId(), value.getDeviceId()),
                        commandService.getById(value.getTenantId(), value.getCommandId())))
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .flatMap(tuple -> {
                    CommandAttributeBO attribute = tuple.getT1();
                    DeviceBO device = tuple.getT2();
                    CommandBO command = tuple.getT3();
                    if (!java.util.Objects.equals(attribute.getDriverId(), device.getDriverId())
                            || !java.util.Objects.equals(device.getProfileId(), command.getProfileId()))
                        return Mono.error(new NotFoundException("Resource does not exist"));
                    return Mono.empty();
                });
    }

    private CommandAttributeConfigBO normalize(CommandAttributeConfigBO value, boolean update) {
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
