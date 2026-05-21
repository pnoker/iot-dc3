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
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.AlarmSourceFlagEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import java.time.LocalDateTime;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Turns expired online-status cache keys into OFFLINE alarm rows in
 * {@code dc3_entity_alarm}. Without this a driver or device that stops sending
 * heartbeats would silently drop off the dashboard.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineExpiryListener {

    private final LocalCacheService localCacheService;

    private final DriverFacade driverFacade;

    private final DeviceFacade deviceFacade;

    private final EntityAlarmManager entityAlarmManager;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    private final EntityStateManager entityStateManager;

    private static Long parseIdSuffix(String key, String prefix) {
        try {
            return Long.parseLong(key.substring(prefix.length()));
        } catch (Exception e) {
            log.debug("Unexpected status key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    @PostConstruct
    void register() {
        localCacheService.onExpire(this::onExpire);
    }

    private void onExpire(String key, Object lastValue) {
        if (Objects.isNull(key))
            return;
        final String lastStatus = lastValue instanceof String s ? s : null;
        CompletableFuture.runAsync(() -> {
            try {
                if (key.startsWith(PrefixConstant.DRIVER_STATUS_KEY_PREFIX)) {
                    handleDriverExpiry(key, lastStatus);
                } else if (key.startsWith(PrefixConstant.DEVICE_STATUS_KEY_PREFIX)) {
                    handleDeviceExpiry(key, lastStatus);
                }
            } catch (Exception e) {
                log.warn("Offline expiry handling failed, key={}", key, e);
            }
        });
    }

    private void handleDriverExpiry(String key, String lastStatus) {
        if (Objects.equals(lastStatus, DriverStatusEnum.OFFLINE.getCode()))
            return;
        Long id = parseIdSuffix(key, PrefixConstant.DRIVER_STATUS_KEY_PREFIX);

        // Skip if the DB scanner has already processed this expiry
        EntityStateDO dbState = entityStateManager.lambdaQuery()
                .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DRIVER.getIndex())
                .eq(EntityStateDO::getEntityId, id)
                .one();
        if (Objects.nonNull(dbState) && Objects.equals(dbState.getStateFlag(), DriverStatusEnum.OFFLINE.getIndex())) {
            return;
        }
        if (Objects.isNull(id))
            return;

        // Mark DB state as offline so the scanner does not duplicate the alarm
        if (Objects.nonNull(dbState)) {
            entityStateManager.lambdaUpdate()
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DRIVER.getIndex())
                    .eq(EntityStateDO::getEntityId, id)
                    .eq(EntityStateDO::getLeaseVersion, dbState.getLeaseVersion())
                    .set(EntityStateDO::getStateFlag, (byte) DriverStatusEnum.OFFLINE.getIndex())
                    .set(EntityStateDO::getLeaseVersion, dbState.getLeaseVersion() + 1L)
                    .set(EntityStateDO::getExpireTime, LocalDateTime.now().plusSeconds(300))
                    .update();
        }

        FacadeDriverBO driver = driverFacade.getById(id);
        if (Objects.isNull(driver)) {
            log.debug("Driver {} not found when handling offline expiry", id);
            return;
        }
        if (Objects.isNull(driver.getTenantId()) || driver.getTenantId() <= 0) {
            log.warn("Drop driver offline alarm because tenantId could not be resolved, driverId={}", id);
            return;
        }

        String message = String.format("Driver heartbeat timed out (last=%s); marked OFFLINE", lastStatus);
        EntityAlarmDO entity = new EntityAlarmDO();
        entity.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.DRIVER.getIndex());
        entity.setEntityId(id);
        entity.setDriverId(id);
        entity.setDeviceId(0L);
        entity.setPointId(0L);
        entity.setRuleId(0L);
        entity.setAlarmTypeFlag(AlarmTypeFlagEnum.OFFLINE.getIndex());
        entity.setAlarmSourceFlag(AlarmSourceFlagEnum.STATE_TIMEOUT.getIndex());
        // Heartbeat-timeout offline events default to P1 — they indicate a
        // connectivity problem the operator usually wants to see ahead of normal
        // P2 device-reported issues.
        entity.setAlarmLevelFlag(AlarmMessageLevelFlagEnum.P1.getIndex());
        entity.setAlarmExt(JsonExt.builder()
                .type("driver-offline")
                .content(message)
                .version(1)
                .build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(driver.getTenantId());
        entityAlarmManager.save(entity);

        DriverAlarmDTO alarm = DriverAlarmDTO.builder()
                .tenantId(driver.getTenantId())
                .driverId(id)
                .status(null)
                .statusName(null)
                .message(message)
                .alarmId(entity.getId())
                .build();
        alarmRuleTriggerService.processDriverAlarm(alarm);
    }

    private void handleDeviceExpiry(String key, String lastStatus) {
        if (Objects.equals(lastStatus, DeviceStatusEnum.OFFLINE.getCode()))
            return;
        Long id = parseIdSuffix(key, PrefixConstant.DEVICE_STATUS_KEY_PREFIX);

        // Skip if the DB scanner has already processed this expiry
        EntityStateDO dbState = entityStateManager.lambdaQuery()
                .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DEVICE.getIndex())
                .eq(EntityStateDO::getEntityId, id)
                .one();
        if (Objects.nonNull(dbState) && Objects.equals(dbState.getStateFlag(), DeviceStatusEnum.OFFLINE.getIndex())) {
            return;
        }
        if (Objects.isNull(id))
            return;

        // Mark DB state as offline so the scanner does not duplicate the alarm
        if (Objects.nonNull(dbState)) {
            entityStateManager.lambdaUpdate()
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DEVICE.getIndex())
                    .eq(EntityStateDO::getEntityId, id)
                    .eq(EntityStateDO::getLeaseVersion, dbState.getLeaseVersion())
                    .set(EntityStateDO::getStateFlag, (byte) DeviceStatusEnum.OFFLINE.getIndex())
                    .set(EntityStateDO::getLeaseVersion, dbState.getLeaseVersion() + 1L)
                    .set(EntityStateDO::getExpireTime, LocalDateTime.now().plusSeconds(300))
                    .update();
        }

        FacadeDeviceBO device = deviceFacade.getById(id);
        if (Objects.isNull(device)) {
            log.debug("Device {} not found when handling offline expiry", id);
            return;
        }
        if (Objects.isNull(device.getTenantId()) || device.getTenantId() <= 0) {
            log.warn("Drop device offline alarm because tenantId could not be resolved, deviceId={}", id);
            return;
        }

        String message = String.format("Device heartbeat timed out (last=%s); marked OFFLINE", lastStatus);
        EntityAlarmDO entity = new EntityAlarmDO();
        entity.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.DEVICE.getIndex());
        entity.setEntityId(id);
        entity.setDriverId(Objects.nonNull(device.getDriverId()) ? device.getDriverId() : 0L);
        entity.setDeviceId(id);
        entity.setPointId(0L);
        entity.setRuleId(0L);
        entity.setAlarmTypeFlag(AlarmTypeFlagEnum.OFFLINE.getIndex());
        entity.setAlarmSourceFlag(AlarmSourceFlagEnum.STATE_TIMEOUT.getIndex());
        // See handleDriverExpiry — heartbeat-timeout offline events default to P1.
        entity.setAlarmLevelFlag(AlarmMessageLevelFlagEnum.P1.getIndex());
        entity.setAlarmExt(JsonExt.builder()
                .type("device-offline")
                .content(message)
                .version(1)
                .build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(device.getTenantId());
        entityAlarmManager.save(entity);

        DeviceAlarmDTO alarm = DeviceAlarmDTO.builder()
                .driverId(device.getDriverId())
                .tenantId(device.getTenantId())
                .deviceId(id)
                .status(null)
                .statusName(null)
                .message(message)
                .alarmId(entity.getId())
                .build();
        alarmRuleTriggerService.processDeviceAlarm(alarm);
    }

}
