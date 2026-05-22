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

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.github.pnoker.common.data.biz.DeviceAlarmService;
import io.github.pnoker.common.data.biz.DeviceStateService;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
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

    private final EntityStateMapper entityStateMapper;

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
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDeviceId())
                || Objects.isNull(entityDTO.getDriverId()) || Objects.isNull(entityDTO.getTenantId())
                || Objects.isNull(entityDTO.getStatus()) || Objects.isNull(entityDTO.getTimeoutUnit())
                || entityDTO.getTimeout() <= 0) {
            return;
        }

        long ttlSeconds = entityDTO.getTimeoutUnit().toSeconds(entityDTO.getTimeout());
        if (ttlSeconds <= 0 || ttlSeconds > Integer.MAX_VALUE) {
            return;
        }

        DeviceStatusEnum statusEnum = DeviceStatusEnum.ofCode(entityDTO.getStatus());
        if (Objects.isNull(statusEnum)) {
            statusEnum = DeviceStatusEnum.OFFLINE;
        }
        String current = statusEnum.getCode();
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(ttlSeconds);
        EntityStateDO stateDO = entityStateMapper.upsertEntityState(
                IdWorker.getId(),
                entityDTO.getTenantId(),
                EntityTypeFlagEnum.DEVICE.getIndex(),
                entityDTO.getDeviceId(),
                entityDTO.getDriverId(),
                (byte) statusEnum.getIndex(),
                (byte) DeviceStatusEnum.OFFLINE.getIndex(),
                expireTime,
                (int) ttlSeconds,
                (byte) TimeoutSourceFlagEnum.DRIVER.getIndex(),
                "device-heartbeat");
        if (Objects.isNull(stateDO)) {
            return;
        }

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
