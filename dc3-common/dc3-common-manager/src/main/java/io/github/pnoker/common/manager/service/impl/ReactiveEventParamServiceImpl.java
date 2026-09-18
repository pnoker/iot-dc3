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
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.EventParamFilter;
import io.github.pnoker.common.manager.repository.ReactiveEventParamStore;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactiveEventParamService;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default reactive event parameter application service. */
@Service
@RequiredArgsConstructor
public class ReactiveEventParamServiceImpl implements ReactiveEventParamService {
    private final ReactiveEventParamStore store;
    private final ReactiveEventService eventService;
    private final ReactiveDeviceService deviceService;
    private final ReactiveDriverService driverService;
    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public Mono<EventParamBO> add(EventParamBO value) {
        return Mono.defer(() -> {
            validate(value, false);
            return parent(value.getTenantId(), value.getEventId())
                    .then(store.existsByNameOrCode(
                            value.getTenantId(), value.getEventId(), value.getParamName(), value.getParamCode(), null))
                    .flatMap(duplicate -> duplicate
                            ? Mono.<EventParamBO>error(new DuplicateException("Event param has been duplicated"))
                            : store.insert(normalize(value, false)))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Event param has been duplicated"))
                    .flatMap(saved -> publishUpdate(saved.getTenantId(), saved.getEventId())
                            .thenReturn(saved));
        });
    }

    @Override
    public Mono<EventParamBO> update(EventParamBO value) {
        return Mono.defer(() -> {
            validate(value, true);
            return store.get(value.getTenantId(), value.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Event param does not exist")))
                    .flatMap(current -> {
                        ensureStableIdentity(current, value);
                        return parent(value.getTenantId(), value.getEventId())
                                .then(store.existsByNameOrCode(
                                        value.getTenantId(),
                                        value.getEventId(),
                                        value.getParamName(),
                                        value.getParamCode(),
                                        value.getId()))
                                .flatMap(duplicate -> duplicate
                                        ? Mono.<EventParamBO>error(
                                                new DuplicateException("Event param has been duplicated"))
                                        : store.update(normalize(value, true), value.getVersion()))
                                .switchIfEmpty(Mono.error(new ConflictException("Event param version conflict")))
                                .flatMap(saved -> publishUpdate(saved.getTenantId(), current.getEventId())
                                        .thenReturn(saved));
                    });
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        return store.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Event param does not exist")))
                .flatMap(value -> store.delete(tenantId, id, expectedVersion, operatorId, operatorName)
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(new ConflictException("Event param version conflict")))
                        .flatMap(ignored ->
                                publishUpdate(tenantId, value.getEventId()).thenReturn(true)));
    }

    @Override
    public Mono<EventParamBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and event param ID are required"));
        return store.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Event param does not exist")));
    }

    @Override
    public Flux<EventParamBO> listByEventId(Long tenantId, Long eventId) {
        return store.listByEventId(tenantId, eventId);
    }

    @Override
    public Flux<EventParamBO> listByIds(Long tenantId, Collection<Long> ids) {
        return store.listByIds(tenantId, ids);
    }

    @Override
    public Mono<OffsetPage<EventParamBO>> list(EventParamFilter filter) {
        return store.list(filter);
    }

    private Mono<Void> parent(Long tenantId, Long eventId) {
        return eventService
                .getById(tenantId, eventId)
                .switchIfEmpty(Mono.error(new NotFoundException("Event does not exist")))
                .then();
    }

    private Mono<Void> publishUpdate(Long tenantId, Long eventId) {
        return eventService
                .getById(tenantId, eventId)
                .flatMapMany(event -> {
                    return deviceService
                            .listByProfileId(tenantId, event.getProfileId())
                            .flatMap(device -> driverService
                                    .getByDeviceId(tenantId, device.getId())
                                    .map(driver -> new DeviceTargets(device.getId(), target(driver.getServiceName())))
                                    .onErrorResume(
                                            NotFoundException.class,
                                            error -> Mono.just(new DeviceTargets(device.getId(), Set.of()))))
                            .collectList()
                            .doOnNext(devices -> {
                                Set<String> targets = devices.stream()
                                        .flatMap(device -> device.targets().stream())
                                        .collect(Collectors.toSet());
                                metadataEventPublisher.publishEvent(new MetadataEvent(
                                        this,
                                        tenantId,
                                        eventId,
                                        MetadataTypeEnum.EVENT,
                                        MetadataOperateTypeEnum.UPDATE,
                                        targets));
                                devices.forEach(device -> metadataEventPublisher.publishEvent(new MetadataEvent(
                                        this,
                                        tenantId,
                                        device.deviceId(),
                                        MetadataTypeEnum.DEVICE,
                                        MetadataOperateTypeEnum.UPDATE,
                                        device.targets())));
                            });
                })
                .then();
    }

    private void validate(EventParamBO value, boolean update) {
        if (value == null
                || value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getEventId() == null
                || value.getEventId() <= 0
                || value.getParamName() == null
                || value.getParamName().isBlank()
                || value.getParamCode() == null
                || value.getParamCode().isBlank())
            throw new RequestException("Tenant ID, event ID, parameter name and parameter code are required");
        if (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0))
            throw new RequestException("Event param ID and version are required for update");
        value.setParamName(value.getParamName().trim());
        value.setParamCode(value.getParamCode().trim());
    }

    private EventParamBO normalize(EventParamBO value, boolean update) {
        if (value.getParamTypeFlag() == null) value.setParamTypeFlag(PointTypeEnum.STRING);
        if (value.getEnableFlag() == null) value.setEnableFlag(EnableFlagEnum.ENABLE);
        if (!update && value.getVersion() == null) value.setVersion(0);
        return value;
    }

    private void ensureStableIdentity(EventParamBO current, EventParamBO requested) {
        if (!java.util.Objects.equals(current.getEventId(), requested.getEventId()))
            throw new RequestException("Event param cannot be moved to another event");
        if (!java.util.Objects.equals(current.getParamCode(), requested.getParamCode()))
            throw new RequestException("Event param code cannot be changed");
    }

    private Set<String> target(String serviceName) {
        return serviceName == null || serviceName.isBlank() ? Set.of() : Set.of(serviceName);
    }

    private record DeviceTargets(Long deviceId, Set<String> targets) {}
}
