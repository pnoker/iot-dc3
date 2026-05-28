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
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.entity.query.DriverQuery;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Business service implementation for driver status operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverStatusServiceImpl implements DriverStatusService {

    private final DriverFacade driverFacade;

    private final DeviceFacade deviceFacade;

    private final EntityStateManager entityStateManager;


    @Override
    public Map<Long, String> getStatusByPage(DriverQuery pageQuery) {
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

        FacadePage<FacadeDriverBO> page = driverFacade.listByPage(facadeQuery);
        if (page.getRecords().isEmpty()) {
            return Map.of();
        }
        return getStatusMap(page.getRecords());
    }

    @Override
    public Long getDeviceOnlineByDriverId(Long tenantId, Long driverId) {
        List<String> list = getDeviceStatuses(tenantId, driverId);
        if (Objects.isNull(list)) {
            return 0L;
        }
        return list.stream().filter(e -> e.equals(EntityStatusEnum.ONLINE.getCode())).count();
    }

    @Override
    public Long getDeviceOfflineByDriverId(Long tenantId, Long driverId) {
        List<String> list = getDeviceStatuses(tenantId, driverId);
        if (Objects.isNull(list)) {
            return 0L;
        }
        return list.stream().filter(e -> e.equals(EntityStatusEnum.OFFLINE.getCode())).count();
    }

    /**
     * Load the status of every device attached to the given driver. Returns {@code null}
     * when the driver has no devices (preserves legacy "0" signal).
     */
    private List<String> getDeviceStatuses(Long tenantId, Long driverId) {
        FacadeDriverBO driver = driverFacade.getById(tenantId, driverId);
        if (Objects.isNull(driver)) {
            return null;
        }

        List<FacadeDeviceBO> devices = deviceFacade.listByDriverId(tenantId, driverId);
        if (devices.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> list = new ArrayList<>(devices.size());
        devices.forEach(device -> {
            EntityStateDO state = entityStateManager.lambdaQuery()
                    .eq(EntityStateDO::getTenantId, tenantId)
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DEVICE.getIndex())
                    .eq(EntityStateDO::getEntityId, device.getId())
                    .one();
            String status;
            if (Objects.isNull(state) || state.getExpireTime().isBefore(now)) {
                status = EntityStatusEnum.OFFLINE.getCode();
            } else {
                EntityStatusEnum e = EntityStatusEnum.ofIndex(state.getStateFlag());
                status = Objects.nonNull(e) ? e.getCode() : EntityStatusEnum.OFFLINE.getCode();
            }
            list.add(status);
        });
        return list;
    }

    private Map<Long, String> getStatusMap(List<FacadeDriverBO> drivers) {
        Map<Long, String> statusMap = new HashMap<>(16);
        LocalDateTime now = LocalDateTime.now();
        drivers.forEach(driver -> {
            EntityStateDO state = entityStateManager.lambdaQuery()
                    .eq(EntityStateDO::getTenantId, driver.getTenantId())
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DRIVER.getIndex())
                    .eq(EntityStateDO::getEntityId, driver.getId())
                    .one();
            String status;
            if (Objects.isNull(state) || state.getExpireTime().isBefore(now)) {
                status = EntityStatusEnum.OFFLINE.getCode();
            } else {
                EntityStatusEnum e = EntityStatusEnum.ofIndex(state.getStateFlag());
                status = Objects.nonNull(e) ? e.getCode() : EntityStatusEnum.OFFLINE.getCode();
            }
            statusMap.put(driver.getId(), status);
        });
        return statusMap;
    }

}
