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
import io.github.pnoker.common.data.biz.DeviceStatusService;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.entity.query.DeviceQuery;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import java.time.LocalDateTime;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business service implementation for device status operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStatusServiceImpl implements DeviceStatusService {

    private final DeviceFacade deviceFacade;

    private final EntityStateManager entityStateManager;

    private final LocalCacheService localCacheService;

    @Override
    public Map<Long, String> getStatusByPage(DeviceQuery pageQuery) {
        FacadeDeviceQuery facadeQuery = FacadeDeviceQuery.builder()
                .page(pageQuery.getPage())
                .deviceName(pageQuery.getDeviceName())
                .deviceCode(pageQuery.getDeviceCode())
                .driverId(pageQuery.getDriverId())
                .profileId(pageQuery.getProfileId())
                .tenantId(pageQuery.getTenantId())
                .enableFlag(pageQuery.getEnableFlag())
                .build();

        FacadePage<FacadeDeviceBO> page = deviceFacade.listByPage(facadeQuery);
        if (page.getRecords().isEmpty()) {
            return Map.of();
        }

        return getStatusMap(page.getRecords());
    }

    @Override
    public Map<Long, String> listByProfileId(Long tenantId, Long profileId) {
        List<FacadeDeviceBO> devices = deviceFacade.listByProfileId(tenantId, profileId);
        if (devices.isEmpty()) {
            return Map.of();
        }
        return getStatusMap(devices);
    }

    /**
     * Get a map of device statuses keyed by device id.
     */
    private Map<Long, String> getStatusMap(List<FacadeDeviceBO> devices) {
        Map<Long, String> statusMap = new HashMap<>(16);
        LocalDateTime now = LocalDateTime.now();
        devices.forEach(device -> {
            EntityStateDO state = entityStateManager.lambdaQuery()
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DEVICE.getIndex())
                    .eq(EntityStateDO::getEntityId, device.getId())
                    .one();
            String status;
            if (Objects.isNull(state) || state.getExpireTime().isBefore(now)) {
                status = DeviceStatusEnum.OFFLINE.getCode();
            } else {
                DeviceStatusEnum e = DeviceStatusEnum.ofIndex(state.getStateFlag());
                status = Objects.nonNull(e) ? e.getCode() : DeviceStatusEnum.OFFLINE.getCode();
            }
            statusMap.put(device.getId(), status);
        });
        return statusMap;
    }

}
