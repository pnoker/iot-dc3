/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.common.manager.repository.ReactiveDeviceStore;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Default reactive device application service. */
@Service
@RequiredArgsConstructor
public class ReactiveDeviceServiceImpl implements ReactiveDeviceService {

    private final ReactiveDeviceStore deviceStore;
    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public Mono<DeviceBO> add(DeviceBO device) {
        return Mono.defer(() -> {
            validateWrite(device);
            if (device.getDeviceCode() == null || device.getDeviceCode().isBlank()) {
                device.setDeviceCode(CodeUtil.getCode());
            }
            return deviceStore.getByName(device.getTenantId(), device.getDeviceName())
                    .flatMap(existing -> Mono.<DeviceBO>error(new DuplicateException(
                            "Failed to create device: device has been duplicated")))
                    .switchIfEmpty(Mono.defer(() -> deviceStore.insert(device)))
                    .onErrorMap(DataIntegrityViolationException.class,
                            error -> new DuplicateException("Failed to create device: device code is already in use"))
                    .doOnSuccess(saved -> publish(saved, MetadataOperateTypeEnum.ADD));
        });
    }

    @Override
    public Mono<DeviceBO> update(DeviceBO device) {
        return Mono.defer(() -> {
            validateWrite(device);
            if (device.getId() == null || device.getVersion() == null || device.getVersion() < 0) {
                return Mono.error(new RequestException("Device ID and version are required for update"));
            }
            return deviceStore.get(device.getTenantId(), device.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Device does not exist")))
                    .flatMap(current -> {
                        if (device.getDeviceName().equals(current.getDeviceName())) {
                            return Mono.just(current);
                        }
                        return deviceStore.getByName(device.getTenantId(), device.getDeviceName())
                                .flatMap(existing -> Mono.<DeviceBO>error(new DuplicateException(
                                        "Failed to update device: device has been duplicated")))
                                .switchIfEmpty(Mono.just(current));
                    })
                    .flatMap(ignored -> deviceStore.update(device, device.getVersion()))
                    .switchIfEmpty(Mono.error(new ConflictException("Device version conflict")))
                    .doOnSuccess(saved -> publish(saved, MetadataOperateTypeEnum.UPDATE));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) {
            return Mono.error(new RequestException("Tenant ID and device ID are required"));
        }
        return deviceStore.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Device does not exist")))
                .flatMap(device -> deviceStore.delete(tenantId, id, expectedVersion, operatorId, operatorName))
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new ConflictException("Device version conflict")))
                .doOnSuccess(ignored -> metadataEventPublisher.publishEvent(
                        new MetadataEvent(this, tenantId, id, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE)));
    }

    @Override
    public Mono<DeviceBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null) {
            return Mono.error(new RequestException("Tenant ID and device ID are required"));
        }
        return deviceStore.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Device does not exist")));
    }

    @Override
    public Mono<OffsetPage<DeviceBO>> list(DeviceFilter filter) {
        return deviceStore.list(filter);
    }

    @Override
    public Flux<DeviceBO> listByDriverId(Long tenantId, Long driverId) {
        return deviceStore.listByDriverId(tenantId, driverId);
    }

    @Override
    public Flux<DeviceBO> listByProfileId(Long tenantId, Long profileId) {
        return deviceStore.listByProfileId(tenantId, profileId);
    }

    @Override
    public Flux<DeviceBO> listByIds(Long tenantId, List<Long> ids) {
        return deviceStore.listByIds(tenantId, ids);
    }

    private void validateWrite(DeviceBO device) {
        if (device == null || device.getTenantId() == null || device.getTenantId() <= 0
                || device.getDeviceName() == null || device.getDeviceName().isBlank()
                || device.getDriverId() == null || device.getDriverId() <= 0) {
            throw new RequestException("Tenant ID, device name and driver ID are required");
        }
        device.setDeviceName(device.getDeviceName().trim());
    }

    private void publish(DeviceBO device, MetadataOperateTypeEnum operation) {
        if (device != null) {
            metadataEventPublisher.publishEvent(new MetadataEvent(this, device.getTenantId(), device.getId(), MetadataTypeEnum.DEVICE,
                    operation));
        }
    }
}
