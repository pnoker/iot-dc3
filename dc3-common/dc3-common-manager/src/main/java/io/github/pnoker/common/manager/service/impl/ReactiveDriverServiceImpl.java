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
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.common.manager.repository.ReactiveDriverStore;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default reactive driver application service. */
@Service
@RequiredArgsConstructor
public class ReactiveDriverServiceImpl implements ReactiveDriverService {

    private final ReactiveDriverStore driverStore;
    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public Mono<DriverBO> add(DriverBO driver) {
        return Mono.defer(() -> {
            validateWrite(driver, false);
            if (driver.getDriverCode() == null || driver.getDriverCode().isBlank())
                driver.setDriverCode(CodeUtil.getCode());
            return driverStore
                    .getByNameAndCode(driver.getTenantId(), driver.getDriverName(), driver.getDriverCode())
                    .flatMap(existing -> Mono.<DriverBO>error(
                            new DuplicateException("Failed to create driver: driver has been duplicated")))
                    .switchIfEmpty(Mono.defer(() -> driverStore.insert(driver)))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Failed to create driver: driver code is already in use"))
                    .doOnSuccess(saved -> publish(saved, MetadataOperateTypeEnum.ADD));
        });
    }

    @Override
    public Mono<DriverBO> update(DriverBO driver) {
        return Mono.defer(() -> {
            validateWrite(driver, true);
            return driverStore
                    .get(driver.getTenantId(), driver.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Driver does not exist")))
                    .flatMap(current -> {
                        if (current.getDriverCode() != null
                                && !current.getDriverCode().equals(driver.getDriverCode())) {
                            return Mono.error(new RequestException("Driver code cannot be changed"));
                        }
                        return driverStore
                                .getByNameAndCode(driver.getTenantId(), driver.getDriverName(), driver.getDriverCode())
                                .filter(existing -> !existing.getId().equals(driver.getId()))
                                .flatMap(existing -> Mono.<DriverBO>error(
                                        new DuplicateException("Failed to update driver: driver has been duplicated")))
                                .switchIfEmpty(Mono.defer(() -> driverStore.update(driver, driver.getVersion())));
                    })
                    .switchIfEmpty(Mono.error(new ConflictException("Driver version conflict")))
                    .doOnSuccess(saved -> publish(saved, MetadataOperateTypeEnum.UPDATE));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and driver ID are required"));
        return driverStore
                .get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Driver does not exist")))
                .flatMap(driver -> driverStore.delete(tenantId, id, expectedVersion, operatorId, operatorName))
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new ConflictException("Driver version conflict")))
                .doOnSuccess(ignored -> metadataEventPublisher.publishEvent(new MetadataEvent(
                        this, tenantId, id, MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.DELETE)));
    }

    @Override
    public Mono<DriverBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and driver ID are required"));
        return driverStore.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Driver does not exist")));
    }

    @Override
    public Mono<DriverBO> getByServiceName(Long tenantId, String serviceName) {
        if (tenantId == null || serviceName == null || serviceName.isBlank())
            return Mono.error(new RequestException("Tenant ID and service name are required"));
        return driverStore
                .getByServiceName(tenantId, serviceName)
                .switchIfEmpty(Mono.error(new NotFoundException("Driver does not exist")));
    }

    @Override
    public Mono<DriverBO> getByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null)
            return Mono.error(new RequestException("Tenant ID and device ID are required"));
        return driverStore
                .getByDeviceId(tenantId, deviceId)
                .switchIfEmpty(Mono.error(new NotFoundException("Driver does not exist")));
    }

    @Override
    public Mono<OffsetPage<DriverBO>> list(DriverFilter filter) {
        return driverStore.list(filter);
    }

    @Override
    public Flux<DriverBO> listByIds(Long tenantId, List<Long> ids) {
        return driverStore.listByIds(tenantId, ids);
    }

    @Override
    public Flux<DriverBO> listByProfileId(Long tenantId, Long profileId) {
        return driverStore.listByProfileId(tenantId, profileId);
    }

    @Override
    public Flux<DriverBO> listByPointId(Long tenantId, Long pointId) {
        return driverStore.listByPointId(tenantId, pointId);
    }

    private void validateWrite(DriverBO driver, boolean update) {
        if (driver == null
                || driver.getTenantId() == null
                || driver.getTenantId() <= 0
                || driver.getDriverName() == null
                || driver.getDriverName().isBlank()
                || driver.getServiceName() == null
                || driver.getServiceName().isBlank()
                || driver.getServiceHost() == null
                || driver.getServiceHost().isBlank()) {
            throw new RequestException("Tenant ID, driver name, service name and service host are required");
        }
        if (update && (driver.getId() == null || driver.getVersion() == null || driver.getVersion() < 0)) {
            throw new RequestException("Driver ID and version are required for update");
        }
        if (update && (driver.getDriverCode() == null || driver.getDriverCode().isBlank())) {
            throw new RequestException("Driver code is required for update");
        }
        driver.setDriverName(driver.getDriverName().trim());
        driver.setServiceName(driver.getServiceName().trim());
        driver.setServiceHost(driver.getServiceHost().trim());
    }

    private void publish(DriverBO driver, MetadataOperateTypeEnum operation) {
        if (driver != null)
            metadataEventPublisher.publishEvent(
                    new MetadataEvent(this, driver.getTenantId(), driver.getId(), MetadataTypeEnum.DRIVER, operation));
    }
}
