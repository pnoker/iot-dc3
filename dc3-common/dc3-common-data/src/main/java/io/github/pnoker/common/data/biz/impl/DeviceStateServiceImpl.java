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
import io.github.pnoker.common.data.biz.DeviceAlarmService;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.biz.DeviceStateService;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Business service implementation for device heartbeat and state processing.
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStateServiceImpl implements DeviceStateService {

    private final LocalCacheService localCacheService;

    private final DeviceAlarmService deviceAlarmService;

    private final EntityStateManager entityStateManager;

    private static boolean isFlip(String prev, String current) {
        return online(prev) != online(current);
    }

    private static boolean online(String code) {
        return DeviceStatusEnum.ONLINE.getCode().equals(code) || DeviceStatusEnum.MAINTAIN.getCode().equals(code);
    }

    @Override
    public void heartbeat(DeviceStateDTO entityDTO) {
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDeviceId()) || Objects.isNull(entityDTO.getStatus())) {
            return;
        }

        String statusKey = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + entityDTO.getDeviceId();
        String prev = localCacheService.getKey(statusKey);
        String current = entityDTO.getStatus();

        // Persist state lease to database (source of truth)
        EntityStateDO stateDO = entityStateManager.lambdaQuery()
                .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DEVICE.getIndex())
                .eq(EntityStateDO::getEntityId, entityDTO.getDeviceId())
                .one();
        long ttlSeconds = entityDTO.getTimeUnit().toSeconds(entityDTO.getTimeOut());
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(ttlSeconds);
        if (Objects.isNull(stateDO)) {
            stateDO = new EntityStateDO();
            stateDO.setEntityTypeFlag(EntityTypeFlagEnum.DEVICE.getIndex());
            stateDO.setEntityId(entityDTO.getDeviceId());
            stateDO.setDriverId(Objects.nonNull(entityDTO.getDriverId()) ? entityDTO.getDriverId() : 0L);
            stateDO.setTenantId(entityDTO.getTenantId());
            stateDO.setLeaseVersion(1L);
        } else {
            stateDO.setLeaseVersion(stateDO.getLeaseVersion() + 1L);
        }
        DeviceStatusEnum statusEnum = DeviceStatusEnum.ofCode(current);
        stateDO.setStateFlag((byte) (Objects.nonNull(statusEnum) ? statusEnum.getIndex() : 0));
        stateDO.setExpireTime(expireTime);
        stateDO.setTtlSeconds((int) ttlSeconds);
        entityStateManager.saveOrUpdate(stateDO);

        localCacheService.setKey(statusKey, current, entityDTO.getTimeOut(), entityDTO.getTimeUnit());

        if (Objects.nonNull(prev) && !Objects.equals(prev, current) && isFlip(prev, current)) {
            String message = String.format("Device status changed: %s -> %s", prev, current);
            DeviceAlarmDTO alarm = DeviceAlarmDTO.builder()
                    .driverId(entityDTO.getDriverId())
                    .tenantId(entityDTO.getTenantId())
                    .deviceId(entityDTO.getDeviceId())
                    .status(current)
                    .statusName(DeviceStatusEnum.ofCode(current) != null ? DeviceStatusEnum.ofCode(current).name() : current)
                    .message(message)
                    .build();
            deviceAlarmService.alarm(alarm);
        }
    }

}
