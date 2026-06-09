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
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.data.biz.DriverStateService;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.entity.dto.DriverTimeoutCheckDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.TimeoutSourceTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Business service implementation for driver heartbeat and state processing.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverStateServiceImpl implements DriverStateService {

    private static final int STATUS_TIMEOUT_SECONDS = 45;

    private final DriverAlarmService driverAlarmService;

    private final EntityStateMapper entityStateMapper;

    private final RabbitTemplate rabbitTemplate;

    private static boolean isFlip(byte prevIndex, String currentCode) {
        return online(prevIndex) != online(currentCode);
    }

    private static boolean online(byte index) {
        return index == EntityStatusEnum.ONLINE.getIndex() || index == EntityStatusEnum.MAINTAIN.getIndex();
    }

    private static boolean online(String code) {
        return EntityStatusEnum.ONLINE.getCode().equals(code) || EntityStatusEnum.MAINTAIN.getCode().equals(code);
    }

    @Override
    public void heartbeat(DriverStateDTO entityDTO) {
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDriverId())
                || Objects.isNull(entityDTO.getTenantId()) || Objects.isNull(entityDTO.getStatus())) {
            return;
        }

        EntityStatusEnum statusEnum = EntityStatusEnum.ofCode(entityDTO.getStatus());
        if (Objects.isNull(statusEnum)) {
            statusEnum = EntityStatusEnum.OFFLINE;
        }
        String current = statusEnum.getCode();
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(STATUS_TIMEOUT_SECONDS);
        EntityStateDO stateDO = entityStateMapper.upsertEntityState(
                IdWorker.getId(),
                entityDTO.getTenantId(),
                EntityTypeEnum.DRIVER.getIndex(),
                entityDTO.getDriverId(),
                0L,
                statusEnum.getIndex(),
                EntityStatusEnum.OFFLINE.getIndex(),
                expireTime,
                STATUS_TIMEOUT_SECONDS,
                TimeoutSourceTypeEnum.SYSTEM.getIndex(),
                "driver-heartbeat",
                entityDTO.getStateDescription());
        if (Objects.isNull(stateDO)) {
            return;
        }

        // Publish timeout check message with current lease version
        DriverTimeoutCheckDTO checkDTO = DriverTimeoutCheckDTO.builder()
                .driverId(entityDTO.getDriverId())
                .leaseVersion(stateDO.getLeaseVersion())
                .tenantId(entityDTO.getTenantId())
                .build();
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_STATE_TIMEOUT_DELAY,
                RabbitConstant.ROUTING_DRIVER_TIMEOUT_DELAY, checkDTO);

        byte lastIndex = stateDO.getLastStateFlag();
        if (isFlip(lastIndex, current)) {
            String message = String.format("Driver status changed: %s -> %s",
                    EntityStatusEnum.ofIndex(lastIndex) != null ? EntityStatusEnum.ofIndex(lastIndex).getCode() : DataConstant.STATUS_UNKNOWN,
                    current);
            DriverAlarmDTO alarm = DriverAlarmDTO.builder()
                    .tenantId(entityDTO.getTenantId())
                    .driverId(entityDTO.getDriverId())
                    .status(current)
                    .statusName(EntityStatusEnum.ofCode(current) != null ? EntityStatusEnum.ofCode(current).name() : current)
                    .message(message)
                    .build();
            driverAlarmService.alarm(alarm);
        }
    }

}
