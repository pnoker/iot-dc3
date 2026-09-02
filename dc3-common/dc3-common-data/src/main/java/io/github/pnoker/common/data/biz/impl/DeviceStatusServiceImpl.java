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

import io.github.pnoker.common.data.biz.DeviceStatusService;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Reactive device status service backed by the entity lease projection. */
@Service
@RequiredArgsConstructor
public class DeviceStatusServiceImpl implements DeviceStatusService {

    private final DeviceFacade deviceFacade;
    private final ReactiveEntityStateStore stateStore;

    @Override
    public Mono<Map<String, String>> list(FacadeDeviceOffsetQuery query) {
        return deviceFacade.listReactive(query).flatMap(page -> statuses(query.tenantId(), page.items()));
    }

    @Override
    public Mono<Map<String, String>> listByProfileId(Long tenantId, Long profileId) {
        return deviceFacade
                .listByProfileIdReactive(tenantId, profileId)
                .collectList()
                .flatMap(devices -> statuses(tenantId, devices));
    }

    private Mono<Map<String, String>> statuses(Long tenantId, List<FacadeDeviceBO> devices) {
        List<Long> ids = devices.stream()
                .map(FacadeDeviceBO::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        if (ids.isEmpty()) return Mono.just(Map.of());
        return stateStore
                .listStateFlags(tenantId, EntityTypeEnum.DEVICE, ids)
                .map(flags -> devices.stream()
                        .filter(device -> device.getId() != null)
                        .collect(Collectors.toUnmodifiableMap(
                                device -> String.valueOf(device.getId()),
                                device -> statusCode(flags.get(device.getId())))));
    }

    private String statusCode(Byte state) {
        if (state == null) return EntityStatusEnum.OFFLINE.getCode();
        EntityStatusEnum value = EntityStatusEnum.ofIndex(state);
        return value == null ? EntityStatusEnum.OFFLINE.getCode() : value.getCode();
    }
}
