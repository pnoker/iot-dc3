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

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.data.biz.DriverStatusService;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.entity.query.DriverQuery;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DriverService Impl
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverStatusServiceImpl implements DriverStatusService {

    @Resource
    private DriverFacade driverFacade;

    @Resource
    private DeviceFacade deviceFacade;

    @Resource
    private LocalCacheService localCacheService;

    @Override
    public Map<Long, String> selectByPage(DriverQuery pageQuery) {
        FacadeDriverQuery facadeQuery = FacadeDriverQuery.builder()
                .page(pageQuery.getPage())
                .driverName(pageQuery.getDriverName())
                .driverCode(pageQuery.getDriverCode())
                .serviceName(pageQuery.getServiceName())
                .serviceHost(pageQuery.getServiceHost())
                .tenantId(pageQuery.getTenantId())
                .driverTypeFlag(pageQuery.getDriverTypeFlag())
                .enableFlag(pageQuery.getEnableFlag())
                .build();

        FacadePage<FacadeDriverBO> page = driverFacade.selectByPage(facadeQuery);
        if (page.getRecords().isEmpty()) {
            return Map.of();
        }
        return getStatusMap(page.getRecords());
    }

    @Override
    public String getDeviceOnlineByDriverId(Long driverId) {
        List<String> list = getDeviceStatuses(driverId);
        if (list == null) {
            return String.valueOf(0L);
        }
        long count = list.stream().filter(e -> e.equals(DeviceStatusEnum.ONLINE.getCode())).count();
        return String.valueOf(count);
    }

    @Override
    public String getDeviceOfflineByDriverId(Long driverId) {
        List<String> list = getDeviceStatuses(driverId);
        if (list == null) {
            return String.valueOf(0L);
        }
        long count = list.stream().filter(e -> e.equals(DeviceStatusEnum.OFFLINE.getCode())).count();
        return String.valueOf(count);
    }

    /**
     * Load the status of every device attached to the given driver. Returns {@code null}
     * when the driver has no devices (preserves legacy "0" signal).
     */
    private List<String> getDeviceStatuses(Long driverId) {
        List<FacadeDeviceBO> devices = deviceFacade.selectByDriverId(driverId);
        if (devices.isEmpty()) {
            return null;
        }

        Set<Long> deviceIds = devices.stream().map(FacadeDeviceBO::getId).collect(Collectors.toSet());
        List<String> list = new ArrayList<>(deviceIds.size());
        deviceIds.forEach(id -> {
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + id;
            String status = localCacheService.getKey(key);
            status = Objects.nonNull(status) ? status : DeviceStatusEnum.OFFLINE.getCode();
            list.add(status);
        });
        return list;
    }

    private Map<Long, String> getStatusMap(List<FacadeDriverBO> drivers) {
        Map<Long, String> statusMap = new HashMap<>(16);
        Set<Long> driverIds = drivers.stream().map(FacadeDriverBO::getId).collect(Collectors.toSet());
        driverIds.forEach(id -> {
            String key = PrefixConstant.DRIVER_STATUS_KEY_PREFIX + id;
            String status = localCacheService.getKey(key);
            status = Objects.nonNull(status) ? status : DriverStatusEnum.OFFLINE.getCode();
            statusMap.put(id, status);
        });
        return statusMap;
    }

}
