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

import io.github.pnoker.common.data.biz.DeviceAlarmService;
import io.github.pnoker.common.data.biz.DeviceStateService;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.enums.TimeoutSourceFlagEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Business service implementation for device heartbeat and state processing.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStateServiceImpl implements DeviceStateService {

    private final DeviceAlarmService deviceAlarmService;

    private final EntityStateManager entityStateManager;

    private static boolean isFlip(byte prevIndex, String currentCode) {
        return online(prevIndex) != online(currentCode);
    }

    private static boolean online(byte index) {
        return index == DeviceStatusEnum.ONLINE.getIndex() || index == DeviceStatusEnum.MAINTAIN.getIndex();
    }

    private static boolean online(String code) {
        return DeviceStatusEnum.ONLINE.getCode().equals(code) || DeviceStatusEnum.MAINTAIN.getCode().equals(code);
    }

    @Override
    public void heartbeat(DeviceStateDTO entityDTO) {
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDeviceId()) || Objects.isNull(entityDTO.getStatus())) {
            return;
        }

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
            stateDO.setParentEntityId(Objects.nonNull(entityDTO.getDriverId()) ? entityDTO.getDriverId() : 0L);
            stateDO.setTenantId(entityDTO.getTenantId());
            stateDO.setLeaseVersion(1L);
            stateDO.setLastStateFlag((byte) DeviceStatusEnum.OFFLINE.getIndex());
            stateDO.setLastHeartbeatTime(LocalDateTime.now());
            stateDO.setLastAlarmId(0L);
            stateDO.setTimeoutSourceFlag((byte) TimeoutSourceFlagEnum.DRIVER.getIndex());
            stateDO.setStateExt(JsonExt.builder().type("device-heartbeat").content("").version(1).build());
        } else {
            stateDO.setLeaseVersion(stateDO.getLeaseVersion() + 1L);
            stateDO.setLastStateFlag(stateDO.getStateFlag());
            stateDO.setLastHeartbeatTime(LocalDateTime.now());
        }
        DeviceStatusEnum statusEnum = DeviceStatusEnum.ofCode(current);
        stateDO.setStateFlag((byte) (Objects.nonNull(statusEnum) ? statusEnum.getIndex() : 0));
        stateDO.setExpireTime(expireTime);
        stateDO.setTimeoutSeconds((int) ttlSeconds);
        entityStateManager.saveOrUpdate(stateDO);

        byte lastIndex = stateDO.getLastStateFlag();
        if (isFlip(lastIndex, current)) {
            String message = String.format("Device status changed: %s -> %s",
                    DeviceStatusEnum.ofIndex(lastIndex) != null ? DeviceStatusEnum.ofIndex(lastIndex).getCode() : "unknown",
                    current);
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
