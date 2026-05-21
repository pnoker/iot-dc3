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

import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.AlarmSourceFlagEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Scans {@code dc3_entity_state} for expired leases and generates offline alarm
 * records. This replaces the Caffeine-based {@code OfflineExpiryListener} as the
 * primary expiry detection mechanism, surviving restarts and working consistently
 * across multiple Data Center instances.
 *
 * <p>The scanner runs every 15 seconds. For each expired row it uses an atomic
 * {@code lambdaUpdate} with a {@code lease_version} WHERE condition so that only
 * one Data Center instance processes a given expiry — if another instance (or a
 * late heartbeat) already updated the row, the UPDATE affects zero rows and the
 * alarm is skipped.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityStateExpiryScanner {

    private static final int OFFLINE_RENEW_SECONDS = 300;
    private static final int BATCH_LIMIT = 500;
    private final EntityStateManager entityStateManager;
    private final EntityAlarmManager entityAlarmManager;
    private final AlarmRuleTriggerService alarmRuleTriggerService;

    @Scheduled(fixedDelay = 15_000, initialDelay = 30_000)
    public void scanExpiredLeases() {
        LocalDateTime now = LocalDateTime.now();
        List<EntityStateDO> expired = entityStateManager.lambdaQuery()
                .lt(EntityStateDO::getExpireTime, now)
                .last("LIMIT " + BATCH_LIMIT)
                .list();

        if (expired.isEmpty()) {
            return;
        }

        for (EntityStateDO state : expired) {
            try {
                processExpiredState(state);
            } catch (Exception e) {
                log.warn("Expiry processing failed, entity_type={}, entity_id={}",
                        state.getEntityTypeFlag(), state.getEntityId(), e);
            }
        }
    }

    private void processExpiredState(EntityStateDO scanned) {
        EntityTypeFlagEnum typeFlag = EntityTypeFlagEnum.ofIndex(scanned.getEntityTypeFlag());
        if (Objects.isNull(typeFlag)) {
            return;
        }

        byte offlineIndex;
        if (typeFlag == EntityTypeFlagEnum.DRIVER) {
            offlineIndex = (byte) DriverStatusEnum.OFFLINE.getIndex();
        } else {
            offlineIndex = (byte) DeviceStatusEnum.OFFLINE.getIndex();
        }

        // Already offline — just push expire_time forward
        if (Objects.equals(scanned.getStateFlag(), offlineIndex)) {
            entityStateManager.lambdaUpdate()
                    .eq(EntityStateDO::getEntityTypeFlag, scanned.getEntityTypeFlag())
                    .eq(EntityStateDO::getEntityId, scanned.getEntityId())
                    .eq(EntityStateDO::getLeaseVersion, scanned.getLeaseVersion())
                    .set(EntityStateDO::getLeaseVersion, scanned.getLeaseVersion() + 1L)
                    .set(EntityStateDO::getExpireTime, LocalDateTime.now().plusSeconds(OFFLINE_RENEW_SECONDS))
                    .update();
            return;
        }

        // Atomically claim: UPDATE ... WHERE lease_version = scanned version
        long newVersion = scanned.getLeaseVersion() + 1L;
        LocalDateTime renewTime = LocalDateTime.now().plusSeconds(OFFLINE_RENEW_SECONDS);
        boolean claimed = entityStateManager.lambdaUpdate()
                .eq(EntityStateDO::getEntityTypeFlag, scanned.getEntityTypeFlag())
                .eq(EntityStateDO::getEntityId, scanned.getEntityId())
                .eq(EntityStateDO::getLeaseVersion, scanned.getLeaseVersion())
                .set(EntityStateDO::getLeaseVersion, newVersion)
                .set(EntityStateDO::getStateFlag, offlineIndex)
                .set(EntityStateDO::getExpireTime, renewTime)
                .update();
        if (!claimed) {
            return;
        }

        // Write alarm row
        String prevStatusName;
        if (typeFlag == EntityTypeFlagEnum.DRIVER) {
            DriverStatusEnum prev = DriverStatusEnum.ofIndex(scanned.getStateFlag());
            prevStatusName = Objects.nonNull(prev) ? prev.getCode() : "unknown";
        } else {
            DeviceStatusEnum prev = DeviceStatusEnum.ofIndex(scanned.getStateFlag());
            prevStatusName = Objects.nonNull(prev) ? prev.getCode() : "unknown";
        }

        String message = String.format("%s heartbeat timed out (last=%s); marked OFFLINE",
                typeFlag.getCode(), prevStatusName);
        EntityAlarmDO alarm = new EntityAlarmDO();
        alarm.setEntityId(scanned.getEntityId());
        alarm.setDriverId(scanned.getDriverId());

        if (typeFlag == EntityTypeFlagEnum.DRIVER) {
            alarm.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.DRIVER.getIndex());
            alarm.setDeviceId(0L);
            alarm.setAlarmExt(JsonExt.builder()
                    .type("driver-offline")
                    .content(message)
                    .version(1)
                    .build());
        } else {
            alarm.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.DEVICE.getIndex());
            alarm.setDeviceId(scanned.getEntityId());
            alarm.setAlarmExt(JsonExt.builder()
                    .type("device-offline")
                    .content(message)
                    .version(1)
                    .build());
        }

        alarm.setPointId(0L);
        alarm.setRuleId(0L);
        alarm.setRuleStateId(0L);
        alarm.setAlarmTypeFlag(AlarmTypeFlagEnum.OFFLINE.getIndex());
        alarm.setAlarmSourceFlag(AlarmSourceFlagEnum.STATE_TIMEOUT.getIndex());
        alarm.setAlarmLevelFlag(AlarmMessageLevelFlagEnum.P1.getIndex());
        alarm.setExpiredTime(0L);
        alarm.setConfirmFlag((byte) 0);
        alarm.setTenantId(scanned.getTenantId());
        entityAlarmManager.save(alarm);

        // Trigger alarm rule pipeline
        if (typeFlag == EntityTypeFlagEnum.DRIVER) {
            DriverAlarmDTO dto = DriverAlarmDTO.builder()
                    .tenantId(scanned.getTenantId())
                    .driverId(scanned.getEntityId())
                    .status(null)
                    .statusName(null)
                    .message(message)
                    .alarmId(alarm.getId())
                    .build();
            alarmRuleTriggerService.processDriverAlarm(dto);
        } else {
            DeviceAlarmDTO dto = DeviceAlarmDTO.builder()
                    .driverId(scanned.getDriverId())
                    .tenantId(scanned.getTenantId())
                    .deviceId(scanned.getEntityId())
                    .status(null)
                    .statusName(null)
                    .message(message)
                    .alarmId(alarm.getId())
                    .build();
            alarmRuleTriggerService.processDeviceAlarm(dto);
        }

        log.info("State lease expired: type={}, entityId={}, tenantId={}, prevStatus={}",
                typeFlag.getCode(), scanned.getEntityId(), scanned.getTenantId(), prevStatusName);
    }

}
