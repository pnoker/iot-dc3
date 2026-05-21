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
 * <p>The scanner runs every 15 seconds. For each expired row it performs a
 * lease-version recheck to avoid racing with a concurrent heartbeat that renewed
 * the lease between the scan query and the alarm write.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityStateExpiryScanner {

    private final EntityStateManager entityStateManager;

    private final EntityAlarmManager entityAlarmManager;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    /**
     * How far into the future to push expire_time after marking a row offline,
     * so the scanner does not re-process the same row every cycle.
     */
    private static final int OFFLINE_RENEW_SECONDS = 300;

    @Scheduled(fixedDelay = 15_000, initialDelay = 30_000)
    public void scanExpiredLeases() {
        LocalDateTime now = LocalDateTime.now();
        List<EntityStateDO> expired = entityStateManager.lambdaQuery()
                .lt(EntityStateDO::getExpireTime, now)
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
        // Re-read from DB for lease-version check (heartbeat may have renewed)
        EntityStateDO current = entityStateManager.lambdaQuery()
                .eq(EntityStateDO::getEntityTypeFlag, scanned.getEntityTypeFlag())
                .eq(EntityStateDO::getEntityId, scanned.getEntityId())
                .one();
        if (Objects.isNull(current)) {
            return;
        }
        // Lease was renewed since the scan query
        if (!Objects.equals(current.getLeaseVersion(), scanned.getLeaseVersion())) {
            return;
        }
        // Lease is no longer expired
        if (current.getExpireTime().isAfter(LocalDateTime.now())) {
            return;
        }

        // Check if already offline
        EntityTypeFlagEnum typeFlag = EntityTypeFlagEnum.ofIndex(current.getEntityTypeFlag());
        if (Objects.isNull(typeFlag)) {
            return;
        }
        byte offlineIndex;
        if (typeFlag == EntityTypeFlagEnum.DRIVER) {
            offlineIndex = (byte) DriverStatusEnum.OFFLINE.getIndex();
        } else {
            offlineIndex = (byte) DeviceStatusEnum.OFFLINE.getIndex();
        }
        if (Objects.equals(current.getStateFlag(), offlineIndex)) {
            // Already offline; just push expire_time forward to avoid re-scanning
            current.setLeaseVersion(current.getLeaseVersion() + 1L);
            current.setStateFlag(offlineIndex);
            current.setExpireTime(LocalDateTime.now().plusSeconds(OFFLINE_RENEW_SECONDS));
            entityStateManager.updateById(current);
            return;
        }

        // Record the previous status for the alarm message
        String prevStatusName;
        if (typeFlag == EntityTypeFlagEnum.DRIVER) {
            DriverStatusEnum prev = DriverStatusEnum.ofIndex(current.getStateFlag());
            prevStatusName = Objects.nonNull(prev) ? prev.getCode() : "unknown";
        } else {
            DeviceStatusEnum prev = DeviceStatusEnum.ofIndex(current.getStateFlag());
            prevStatusName = Objects.nonNull(prev) ? prev.getCode() : "unknown";
        }

        // Write alarm row
        String message = String.format("%s heartbeat timed out (last=%s); marked OFFLINE",
                typeFlag.getCode(), prevStatusName);
        EntityAlarmDO alarm = new EntityAlarmDO();
        alarm.setEntityId(current.getEntityId());
        alarm.setDriverId(current.getDriverId());

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
            alarm.setDeviceId(current.getEntityId());
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
        alarm.setTenantId(current.getTenantId());
        entityAlarmManager.save(alarm);

        // Update state to offline
        current.setLeaseVersion(current.getLeaseVersion() + 1L);
        current.setStateFlag(offlineIndex);
        current.setExpireTime(LocalDateTime.now().plusSeconds(OFFLINE_RENEW_SECONDS));
        entityStateManager.updateById(current);

        // Trigger alarm rule pipeline
        if (typeFlag == EntityTypeFlagEnum.DRIVER) {
            DriverAlarmDTO dto = DriverAlarmDTO.builder()
                    .tenantId(current.getTenantId())
                    .driverId(current.getEntityId())
                    .status(null)
                    .statusName(null)
                    .message(message)
                    .alarmId(alarm.getId())
                    .build();
            alarmRuleTriggerService.processDriverAlarm(dto);
        } else {
            DeviceAlarmDTO dto = DeviceAlarmDTO.builder()
                    .driverId(current.getDriverId())
                    .tenantId(current.getTenantId())
                    .deviceId(current.getEntityId())
                    .status(null)
                    .statusName(null)
                    .message(message)
                    .alarmId(alarm.getId())
                    .build();
            alarmRuleTriggerService.processDeviceAlarm(dto);
        }

        log.info("State lease expired: type={}, entityId={}, tenantId={}, prevStatus={}",
                typeFlag.getCode(), current.getEntityId(), current.getTenantId(), prevStatusName);
    }

}
