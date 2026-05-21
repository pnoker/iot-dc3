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

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.data.biz.SystemHealthService;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.entity.vo.dashboard.SystemHealthVO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverDeviceStatusSummaryBO;
import io.github.pnoker.common.facade.entity.bo.FacadeSystemHealthBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-process StatusHealthFacade implementation.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusHealthLocalFacade implements StatusHealthFacade {

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final LocalCacheService localCacheService;

    private final SystemHealthService systemHealthService;

    @Override
    public Map<Long, String> selectDeviceStatusesByIds(Long tenantId, Collection<Long> deviceIds) {
        List<FacadeDeviceBO> devices = deviceFacade.listByIds(tenantId, deviceIds);
        Map<Long, String> result = new LinkedHashMap<>();
        devices.forEach(device -> result.put(device.getId(), deviceStatus(device.getId())));
        return result;
    }

    @Override
    public Map<Long, String> selectDeviceStatusesByProfileId(Long tenantId, Long profileId) {
        List<FacadeDeviceBO> devices = deviceFacade.listByProfileId(tenantId, profileId);
        Map<Long, String> result = new LinkedHashMap<>();
        devices.forEach(device -> result.put(device.getId(), deviceStatus(device.getId())));
        return result;
    }

    @Override
    public Map<Long, String> selectDriverStatusesByIds(Long tenantId, Collection<Long> driverIds) {
        List<FacadeDriverBO> drivers = driverFacade.listByIds(tenantId, driverIds);
        Map<Long, String> result = new LinkedHashMap<>();
        drivers.forEach(driver -> result.put(driver.getId(), driverStatus(driver.getId())));
        return result;
    }

    @Override
    public FacadeDriverDeviceStatusSummaryBO getDriverDeviceStatusSummary(Long tenantId, Long driverId) {
        if (Objects.isNull(driverFacade.getById(tenantId, driverId))) {
            return null;
        }
        List<FacadeDeviceBO> devices = deviceFacade.listByDriverId(tenantId, driverId);
        long online = devices.stream()
                .filter(device -> Objects.equals(DeviceStatusEnum.ONLINE.getCode(), deviceStatus(device.getId())))
                .count();
        int offline = (int) Math.max(0, devices.size() - online);
        return new FacadeDriverDeviceStatusSummaryBO(driverId, devices.size(), (int) online, offline);
    }

    @Override
    public FacadeSystemHealthBO systemHealth(Long tenantId) {
        SystemHealthVO source = systemHealthService.snapshot(tenantId);
        if (Objects.isNull(source)) {
            return null;
        }
        FacadeSystemHealthBO target = new FacadeSystemHealthBO();
        target.setCenter(source.getCenter());
        target.setInfra(source.getInfra());
        target.setDrivers(toFacadeSummary(source.getDrivers()));
        target.setDevices(toFacadeSummary(source.getDevices()));
        return target;
    }

    private FacadeSystemHealthBO.FleetSummary toFacadeSummary(SystemHealthVO.FleetSummary source) {
        if (Objects.isNull(source)) {
            return new FacadeSystemHealthBO.FleetSummary();
        }
        return new FacadeSystemHealthBO.FleetSummary(source.getTotal(), source.getOnline());
    }

    private String deviceStatus(Long deviceId) {
        String status = localCacheService.getKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + deviceId);
        return Objects.nonNull(status) ? status : DeviceStatusEnum.OFFLINE.getCode();
    }

    private String driverStatus(Long driverId) {
        String status = localCacheService.getKey(PrefixConstant.DRIVER_STATUS_KEY_PREFIX + driverId);
        return Objects.nonNull(status) ? status : DriverStatusEnum.OFFLINE.getCode();
    }

}
