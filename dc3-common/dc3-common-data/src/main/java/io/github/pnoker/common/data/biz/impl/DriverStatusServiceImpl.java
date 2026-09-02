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
package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.biz.DriverStatusService;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Reactive driver status service backed by the entity lease projection. */
@Service
@RequiredArgsConstructor
public class DriverStatusServiceImpl implements DriverStatusService {

    private final DriverFacade driverFacade;
    private final DeviceFacade deviceFacade;
    private final ReactiveEntityStateStore stateStore;

    @Override
    public Mono<Map<String, String>> list(FacadeDriverOffsetQuery query) {
        return driverFacade
                .listReactive(query)
                .flatMap(page -> statuses(
                        query.tenantId(),
                        page.items().stream()
                                .map(driver -> driver.getId())
                                .filter(id -> id != null)
                                .toList(),
                        EntityTypeEnum.DRIVER));
    }

    @Override
    public Mono<Long> countOnlineDevices(Long tenantId, Long driverId) {
        return driverFacade
                .getByIdReactive(tenantId, driverId)
                .switchIfEmpty(Mono.error(new NotFoundException("Driver does not exist")))
                .flatMap(ignored ->
                        deviceFacade.listByDriverIdReactive(tenantId, driverId).collectList())
                .flatMap(devices -> {
                    List<Long> ids = devices.stream()
                            .map(FacadeDeviceBO::getId)
                            .filter(id -> id != null)
                            .toList();
                    if (ids.isEmpty()) return Mono.just(0L);
                    return stateStore
                            .listStateFlags(tenantId, EntityTypeEnum.DEVICE, ids)
                            .map(flags -> ids.stream()
                                    .filter(id -> online(flags.get(id)))
                                    .count());
                });
    }

    @Override
    public Mono<Long> countOfflineDevices(Long tenantId, Long driverId) {
        return driverFacade
                .getByIdReactive(tenantId, driverId)
                .switchIfEmpty(Mono.error(new NotFoundException("Driver does not exist")))
                .flatMap(ignored ->
                        deviceFacade.listByDriverIdReactive(tenantId, driverId).collectList())
                .flatMap(devices -> {
                    List<Long> ids = devices.stream()
                            .map(FacadeDeviceBO::getId)
                            .filter(id -> id != null)
                            .toList();
                    if (ids.isEmpty()) return Mono.just(0L);
                    return stateStore
                            .listStateFlags(tenantId, EntityTypeEnum.DEVICE, ids)
                            .map(flags -> ids.stream()
                                    .filter(id -> !online(flags.get(id)))
                                    .count());
                });
    }

    private Mono<Map<String, String>> statuses(Long tenantId, List<Long> ids, EntityTypeEnum type) {
        if (ids.isEmpty()) return Mono.just(Map.of());
        return stateStore
                .listStateFlags(tenantId, type, ids)
                .map(flags -> ids.stream()
                        .collect(Collectors.toUnmodifiableMap(String::valueOf, id -> statusCode(flags.get(id)))));
    }

    private boolean online(Byte state) {
        if (state == null) return false;
        return state == EntityStatusEnum.ONLINE.getIndex() || state == EntityStatusEnum.MAINTAIN.getIndex();
    }

    private String statusCode(Byte state) {
        if (state == null) return EntityStatusEnum.OFFLINE.getCode();
        EntityStatusEnum value = EntityStatusEnum.ofIndex(state);
        return value == null ? EntityStatusEnum.OFFLINE.getCode() : value.getCode();
    }
}
