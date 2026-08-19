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

package io.github.pnoker.common.data.rabbit;

import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverTimeoutCheckDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelEnum;
import io.github.pnoker.common.enums.AlarmSourceTypeEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.AlarmTypeEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * RabbitMQ receiver for driver timeout check messages.
 * <p>
 * Consumes messages dead-lettered from the 45s TTL delay queue and performs
 * a secondary check against {@code dc3_entity_state}. Only marks the driver
 * OFFLINE when the lease version, expiry, and heartbeat-renewed state conditions all
 * confirm the driver has truly stopped sending heartbeats.
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverTimeoutCheckReceiver {

    private static final int OFFLINE_RENEW_SECONDS = 300;

    private final EntityStateManager entityStateManager;
    private final EntityAlarmManager entityAlarmManager;
    private final AlarmRuleTriggerService alarmRuleTriggerService;

    private static boolean statusIs(Byte stateFlag, EntityStatusEnum status) {
        return Objects.equals(stateFlag, status.getIndex());
    }

    /**
     * Consume a driver timeout check and, after re-verifying the lease version and
     * expiry to avoid racing a newer heartbeat, marks an expired driver offline and
     * raises a driver alarm.
     *
     * @param channel the RabbitMQ channel for manual ack
     * @param message the raw message carrying the delivery tag
     * @param dto     the driver timeout check carrying tenant, driver id, and lease version
     */
    @Dc3Listener(topic = MqTopic.STATE_TIMEOUT)
    public void driverTimeoutCheck(MqReceived<DriverTimeoutCheckDTO> message, Acknowledgment ack) {
        DriverTimeoutCheckDTO dto = message.payload();
        try {
            if (Objects.isNull(dto) || Objects.isNull(dto.getDriverId()) || Objects.isNull(dto.getTenantId())
                    || Objects.isNull(dto.getLeaseVersion())) {
                ack.reject(false);
                return;
            }

            EntityStateDO state = entityStateManager.lambdaQuery()
                    .eq(EntityStateDO::getTenantId, dto.getTenantId())
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeEnum.DRIVER.getIndex())
                    .eq(EntityStateDO::getEntityId, dto.getDriverId())
                    .one();

            // State row gone — nothing to do
            if (Objects.isNull(state)) {
                ack.ack();
                return;
            }

            // lease_version mismatched means a newer heartbeat arrived
            if (!Objects.equals(state.getLeaseVersion(), dto.getLeaseVersion())) {
                ack.ack();
                return;
            }

            // Not expired yet
            if (state.getExpireTime().isAfter(LocalDateTime.now())) {
                ack.ack();
                return;
            }

            // Already offline
            Byte offlineIndex = EntityStatusEnum.OFFLINE.getIndex();
            if (Objects.equals(state.getStateFlag(), offlineIndex)) {
                ack.ack();
                return;
            }

            // Heartbeat-renewed states should become OFFLINE once their lease expires.
            boolean heartbeatRenewed = statusIs(state.getStateFlag(), EntityStatusEnum.ONLINE)
                    || statusIs(state.getStateFlag(), EntityStatusEnum.MAINTAIN)
                    || statusIs(state.getStateFlag(), EntityStatusEnum.FAULT);
            if (!heartbeatRenewed) {
                ack.ack();
                return;
            }

            // Claim: atomically update to OFFLINE
            long newVersion = state.getLeaseVersion() + 1L;
            boolean claimed = entityStateManager.lambdaUpdate()
                    .eq(EntityStateDO::getTenantId, dto.getTenantId())
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeEnum.DRIVER.getIndex())
                    .eq(EntityStateDO::getEntityId, dto.getDriverId())
                    .eq(EntityStateDO::getLeaseVersion, state.getLeaseVersion())
                    .set(EntityStateDO::getLeaseVersion, newVersion)
                    .set(EntityStateDO::getStateFlag, offlineIndex)
                    .set(EntityStateDO::getLastStateFlag, state.getStateFlag())
                    .set(EntityStateDO::getExpireTime, LocalDateTime.now().plusSeconds(OFFLINE_RENEW_SECONDS))
                    .update();

            if (!claimed) {
                ack.ack();
                return;
            }

            // Write alarm
            EntityStatusEnum prevStatus = EntityStatusEnum.ofIndex(state.getStateFlag());
            String prevCode = Objects.nonNull(prevStatus) ? prevStatus.getCode() : "unknown";
            String alarmMessage = String.format("Driver heartbeat timed out (last=%s); marked OFFLINE", prevCode);

            EntityAlarmDO alarm = new EntityAlarmDO();
            alarm.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.DRIVER.getIndex());
            alarm.setEntityId(dto.getDriverId());
            alarm.setDriverId(dto.getDriverId());
            alarm.setDeviceId(0L);
            alarm.setPointId(0L);
            alarm.setRuleId(0L);
            alarm.setRuleStateId(0L);
            alarm.setAlarmTypeFlag(AlarmTypeEnum.OFFLINE.getIndex());
            alarm.setAlarmSourceFlag(AlarmSourceTypeEnum.STATE_TIMEOUT.getIndex());
            alarm.setAlarmLevelFlag(AlarmMessageLevelEnum.P1.getIndex());
            alarm.setAlarmExt(JsonExt.builder().type("driver-offline").content(alarmMessage).version(1).build());
            alarm.setExpiredTime(0L);
            alarm.setConfirmFlag((byte) 0);
            alarm.setTenantId(dto.getTenantId());
            entityAlarmManager.save(alarm);

            // Update lastAlarmId on state row
            entityStateManager.lambdaUpdate()
                    .eq(EntityStateDO::getTenantId, dto.getTenantId())
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeEnum.DRIVER.getIndex())
                    .eq(EntityStateDO::getEntityId, dto.getDriverId())
                    .eq(EntityStateDO::getLeaseVersion, newVersion)
                    .set(EntityStateDO::getLastAlarmId, alarm.getId())
                    .update();

            // Trigger alarm rule pipeline
            DriverAlarmDTO driverAlarm = DriverAlarmDTO.builder()
                    .tenantId(dto.getTenantId())
                    .driverId(dto.getDriverId())
                    .status(EntityStatusEnum.OFFLINE.getCode())
                    .statusName(EntityStatusEnum.OFFLINE.name())
                    .message(alarmMessage)
                    .alarmId(alarm.getId())
                    .build();
            alarmRuleTriggerService.processDriverAlarm(driverAlarm);

            log.info("Driver timeout check confirmed OFFLINE: driverId={}, tenantId={}, prevStatus={}",
                    dto.getDriverId(), dto.getTenantId(), prevCode);

            ack.ack();
        } catch (Exception e) {
            log.error("Driver timeout check failed.", e);
            ack.reject(true);
        }
    }
}
