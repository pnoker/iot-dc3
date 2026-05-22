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

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverTimeoutCheckDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.AlarmSourceFlagEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
 * @version 2026.5.22
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

    @RabbitHandler
    @RabbitListener(queues = "#{driverTimeoutCheckQueue.name}")
    public void driverTimeoutCheck(Channel channel, Message message, DriverTimeoutCheckDTO dto) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            if (Objects.isNull(dto) || Objects.isNull(dto.getDriverId()) || Objects.isNull(dto.getTenantId())
                    || Objects.isNull(dto.getLeaseVersion())) {
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }

            EntityStateDO state = entityStateManager.lambdaQuery()
                    .eq(EntityStateDO::getTenantId, dto.getTenantId())
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DRIVER.getIndex())
                    .eq(EntityStateDO::getEntityId, dto.getDriverId())
                    .one();

            // State row gone — nothing to do
            if (Objects.isNull(state)) {
                RabbitAckUtil.ack(channel, deliveryTag);
                return;
            }

            // lease_version mismatched means a newer heartbeat arrived
            if (!Objects.equals(state.getLeaseVersion(), dto.getLeaseVersion())) {
                RabbitAckUtil.ack(channel, deliveryTag);
                return;
            }

            // Not expired yet
            if (state.getExpireTime().isAfter(LocalDateTime.now())) {
                RabbitAckUtil.ack(channel, deliveryTag);
                return;
            }

            // Already offline
            Byte offlineIndex = DriverStatusEnum.OFFLINE.getIndex();
            if (Objects.equals(state.getStateFlag(), offlineIndex)) {
                RabbitAckUtil.ack(channel, deliveryTag);
                return;
            }

            // Heartbeat-renewed states should become OFFLINE once their lease expires.
            boolean heartbeatRenewed = statusIs(state.getStateFlag(), DriverStatusEnum.ONLINE)
                    || statusIs(state.getStateFlag(), DriverStatusEnum.MAINTAIN)
                    || statusIs(state.getStateFlag(), DriverStatusEnum.FAULT);
            if (!heartbeatRenewed) {
                RabbitAckUtil.ack(channel, deliveryTag);
                return;
            }

            // Claim: atomically update to OFFLINE
            long newVersion = state.getLeaseVersion() + 1L;
            boolean claimed = entityStateManager.lambdaUpdate()
                    .eq(EntityStateDO::getTenantId, dto.getTenantId())
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DRIVER.getIndex())
                    .eq(EntityStateDO::getEntityId, dto.getDriverId())
                    .eq(EntityStateDO::getLeaseVersion, state.getLeaseVersion())
                    .set(EntityStateDO::getLeaseVersion, newVersion)
                    .set(EntityStateDO::getStateFlag, offlineIndex)
                    .set(EntityStateDO::getLastStateFlag, state.getStateFlag())
                    .set(EntityStateDO::getExpireTime, LocalDateTime.now().plusSeconds(OFFLINE_RENEW_SECONDS))
                    .update();

            if (!claimed) {
                RabbitAckUtil.ack(channel, deliveryTag);
                return;
            }

            // Write alarm
            DriverStatusEnum prevStatus = DriverStatusEnum.ofIndex(state.getStateFlag());
            String prevCode = Objects.nonNull(prevStatus) ? prevStatus.getCode() : "unknown";
            String alarmMessage = String.format("Driver heartbeat timed out (last=%s); marked OFFLINE", prevCode);

            EntityAlarmDO alarm = new EntityAlarmDO();
            alarm.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.DRIVER.getIndex());
            alarm.setEntityId(dto.getDriverId());
            alarm.setDriverId(dto.getDriverId());
            alarm.setDeviceId(0L);
            alarm.setPointId(0L);
            alarm.setRuleId(0L);
            alarm.setRuleStateId(0L);
            alarm.setAlarmTypeFlag(AlarmTypeFlagEnum.OFFLINE.getIndex());
            alarm.setAlarmSourceFlag(AlarmSourceFlagEnum.STATE_TIMEOUT.getIndex());
            alarm.setAlarmLevelFlag(AlarmMessageLevelFlagEnum.P1.getIndex());
            alarm.setAlarmExt(JsonExt.builder().type("driver-offline").content(alarmMessage).version(1).build());
            alarm.setExpiredTime(0L);
            alarm.setConfirmFlag((byte) 0);
            alarm.setTenantId(dto.getTenantId());
            entityAlarmManager.save(alarm);

            // Update lastAlarmId on state row
            entityStateManager.lambdaUpdate()
                    .eq(EntityStateDO::getTenantId, dto.getTenantId())
                    .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DRIVER.getIndex())
                    .eq(EntityStateDO::getEntityId, dto.getDriverId())
                    .eq(EntityStateDO::getLeaseVersion, newVersion)
                    .set(EntityStateDO::getLastAlarmId, alarm.getId())
                    .update();

            // Trigger alarm rule pipeline
            DriverAlarmDTO driverAlarm = DriverAlarmDTO.builder()
                    .tenantId(dto.getTenantId())
                    .driverId(dto.getDriverId())
                    .status(DriverStatusEnum.OFFLINE.getCode())
                    .statusName(DriverStatusEnum.OFFLINE.name())
                    .message(alarmMessage)
                    .alarmId(alarm.getId())
                    .build();
            alarmRuleTriggerService.processDriverAlarm(driverAlarm);

            log.info("Driver timeout check confirmed OFFLINE: driverId={}, tenantId={}, prevStatus={}",
                    dto.getDriverId(), dto.getTenantId(), prevCode);

            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Driver timeout check failed, deliveryTag={}", deliveryTag, e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

    private static boolean statusIs(Byte stateFlag, DriverStatusEnum status) {
        return Objects.equals(stateFlag, status.getIndex());
    }
}
