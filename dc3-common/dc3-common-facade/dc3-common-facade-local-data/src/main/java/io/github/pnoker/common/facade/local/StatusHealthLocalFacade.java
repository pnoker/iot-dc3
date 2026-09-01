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

package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.data.biz.SystemHealthService;
import io.github.pnoker.common.data.entity.vo.dashboard.SystemHealthVO;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverDeviceStatusSummaryBO;
import io.github.pnoker.common.facade.entity.bo.FacadeSystemHealthBO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StatusHealthLocalFacade implements StatusHealthFacade {
    private final DeviceFacade deviceFacade;
    private final DriverFacade driverFacade;
    private final SystemHealthService systemHealthService;
    private final ReactiveEntityStateStore entityStateStore;

    @Override
    public Mono<Map<Long, String>> listDeviceStatusesByIdsReactive(Long tenantId, Collection<Long> deviceIds) {
        return deviceFacade.listByIdsReactive(tenantId, deviceIds).map(FacadeDeviceBO::getId).collectList().flatMap(ids -> stateFlags(tenantId, EntityTypeEnum.DEVICE, ids).map(flags -> statusCodes(ids, flags)));
    }
    @Override
    public Mono<Map<Long, String>> listDeviceStatusesByProfileIdReactive(Long tenantId, Long profileId) {
        return deviceFacade.listByProfileIdReactive(tenantId, profileId).map(FacadeDeviceBO::getId).collectList().flatMap(ids -> stateFlags(tenantId, EntityTypeEnum.DEVICE, ids).map(flags -> statusCodes(ids, flags)));
    }
    @Override
    public Mono<Map<Long, String>> listDriverStatusesByIdsReactive(Long tenantId, Collection<Long> driverIds) {
        return driverFacade.listByIdsReactive(tenantId, driverIds).map(driver -> driver.getId()).collectList().flatMap(ids -> stateFlags(tenantId, EntityTypeEnum.DRIVER, ids).map(flags -> statusCodes(ids, flags)));
    }
    @Override
    public Mono<FacadeDriverDeviceStatusSummaryBO> getDriverDeviceStatusSummaryReactive(Long tenantId, Long driverId) {
        return driverFacade.getByIdReactive(tenantId, driverId).flatMap(driver -> deviceFacade.listByDriverIdReactive(tenantId, driverId).map(FacadeDeviceBO::getId).collectList().flatMap(ids -> stateFlags(tenantId, EntityTypeEnum.DEVICE, ids).map(flags -> {
            int online = (int) ids.stream().filter(id -> EntityStatusEnum.ONLINE.getIndex().equals(flags.get(id))).count();
            return new FacadeDriverDeviceStatusSummaryBO(driverId, ids.size(), online, ids.size() - online);
        })));
    }
    @Override
    public Mono<FacadeSystemHealthBO> systemHealthReactive(Long tenantId) {
        return systemHealthService.snapshot(tenantId).map(this::toFacadeHealth);
    }
    private Mono<Map<Long, Byte>> stateFlags(Long tenantId, EntityTypeEnum type, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Mono.just(Map.of());
        return entityStateStore.listStateFlags(tenantId, type, ids);
    }
    private Map<Long, String> statusCodes(Collection<Long> ids, Map<Long, Byte> flags) {
        Map<Long, String> result = new LinkedHashMap<>();
        ids.forEach(id -> { EntityStatusEnum status = EntityStatusEnum.ofIndex(flags.get(id)); result.put(id, status == null ? EntityStatusEnum.OFFLINE.getCode() : status.getCode()); });
        return Map.copyOf(result);
    }
    private FacadeSystemHealthBO toFacadeHealth(SystemHealthVO source) {
        if (source == null) return null;
        FacadeSystemHealthBO target = new FacadeSystemHealthBO(); target.setCenter(source.getCenter()); target.setInfra(source.getInfra()); target.setDrivers(summary(source.getDrivers())); target.setDevices(summary(source.getDevices())); return target;
    }
    private FacadeSystemHealthBO.FleetSummary summary(SystemHealthVO.FleetSummary source) { return source == null ? new FacadeSystemHealthBO.FleetSummary() : new FacadeSystemHealthBO.FleetSummary(source.getTotal(), source.getOnline()); }
}
