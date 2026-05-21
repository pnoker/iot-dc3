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

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.data.biz.DriverStateService;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.entity.dto.DriverTimeoutCheckDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.enums.TimeoutSourceFlagEnum;

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

    private final EntityStateManager entityStateManager;

    private final RabbitTemplate rabbitTemplate;

    private static boolean isFlip(byte prevIndex, String currentCode) {
        return online(prevIndex) != online(currentCode);
    }

    private static boolean online(byte index) {
        return index == DriverStatusEnum.ONLINE.getIndex() || index == DriverStatusEnum.MAINTAIN.getIndex();
    }

    private static boolean online(String code) {
        return DriverStatusEnum.ONLINE.getCode().equals(code) || DriverStatusEnum.MAINTAIN.getCode().equals(code);
    }

    @Override
    public void heartbeat(DriverStateDTO entityDTO) {
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDriverId()) || Objects.isNull(entityDTO.getStatus())) {
            return;
        }

        String current = entityDTO.getStatus();

        // Persist state lease to database (source of truth)
        EntityStateDO stateDO = entityStateManager.lambdaQuery()
                .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DRIVER.getIndex())
                .eq(EntityStateDO::getEntityId, entityDTO.getDriverId())
                .one();
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(STATUS_TIMEOUT_SECONDS);
        if (Objects.isNull(stateDO)) {
            stateDO = new EntityStateDO();
            stateDO.setEntityTypeFlag(EntityTypeFlagEnum.DRIVER.getIndex());
            stateDO.setEntityId(entityDTO.getDriverId());
            stateDO.setParentEntityId(entityDTO.getDriverId());
            stateDO.setTenantId(entityDTO.getTenantId());
            stateDO.setLeaseVersion(1L);
            stateDO.setLastStateFlag((byte) DriverStatusEnum.OFFLINE.getIndex());
            stateDO.setLastHeartbeatTime(LocalDateTime.now());
            stateDO.setLastAlarmId(0L);
            stateDO.setTimeoutSourceFlag((byte) TimeoutSourceFlagEnum.SYSTEM.getIndex());
            stateDO.setStateExt(JsonExt.builder().type("driver-heartbeat").content("").version(1).build());
        } else {
            stateDO.setLeaseVersion(stateDO.getLeaseVersion() + 1L);
            stateDO.setLastStateFlag(stateDO.getStateFlag());
            stateDO.setLastHeartbeatTime(LocalDateTime.now());
        }
        DriverStatusEnum statusEnum = DriverStatusEnum.ofCode(current);
        stateDO.setStateFlag((byte) (Objects.nonNull(statusEnum) ? statusEnum.getIndex() : 0));
        stateDO.setExpireTime(expireTime);
        stateDO.setTimeoutSeconds(STATUS_TIMEOUT_SECONDS);
        entityStateManager.saveOrUpdate(stateDO);

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
                    DriverStatusEnum.ofIndex(lastIndex) != null ? DriverStatusEnum.ofIndex(lastIndex).getCode() : "unknown",
                    current);
            DriverAlarmDTO alarm = DriverAlarmDTO.builder()
                    .tenantId(entityDTO.getTenantId())
                    .driverId(entityDTO.getDriverId())
                    .status(current)
                    .statusName(DriverStatusEnum.ofCode(current) != null ? DriverStatusEnum.ofCode(current).name() : current)
                    .message(message)
                    .build();
            driverAlarmService.alarm(alarm);
        }
    }

}
