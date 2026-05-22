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

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.AlarmSourceFlagEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.utils.RabbitAckUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Tick-triggered scanner for expired device state leases.
 *
 * <p>A RabbitMQ TTL + DLX tick queue fires every 10 seconds. On each tick the
 * scanner queries {@code dc3_entity_state} for devices whose
 * {@code expire_time <= now()} and whose {@code state_flag} is still in the
 * heartbeat-renewed family. Each expired device is atomically claimed via
 * {@code lease_version} and an offline alarm is written.
 *
 * <p>After processing, the scanner publishes the next tick so the cycle
 * continues indefinitely.
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
    private final EntityStateMapper entityStateMapper;
    private final EntityAlarmManager entityAlarmManager;
    private final AlarmRuleTriggerService alarmRuleTriggerService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Bootstrap the first tick on startup. Subsequent ticks are self-sustaining:
     * each scan cycle publishes the next tick.
     */
    @PostConstruct
    void publishInitialTick() {
        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE_STATE_TIMEOUT_DELAY,
                RabbitConstant.ROUTING_DEVICE_SCAN_TICK,
                "tick");
        log.info("Published initial device scan tick");
    }

    /**
     * Process one scan cycle: find expired devices, mark offline, write alarms,
     * then publish the next tick.
     */
    @RabbitHandler
    @RabbitListener(queues = "#{deviceScanQueue.name}")
    public void onScanTick(Channel channel, Message message) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            scanExpiredDevices();

            // Publish next tick to keep the cycle going
            rabbitTemplate.convertAndSend(
                    RabbitConstant.TOPIC_EXCHANGE_STATE_TIMEOUT_DELAY,
                    RabbitConstant.ROUTING_DEVICE_SCAN_TICK,
                    "tick");

            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Device scan tick failed", e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

    private void scanExpiredDevices() {
        List<EntityStateDO> expired = entityStateMapper.claimExpiredDevices(
                EntityTypeFlagEnum.DEVICE.getIndex(),
                EntityStatusEnum.ONLINE.getIndex(),
                EntityStatusEnum.MAINTAIN.getIndex(),
                EntityStatusEnum.FAULT.getIndex(),
                EntityStatusEnum.OFFLINE.getIndex(),
                BATCH_LIMIT,
                OFFLINE_RENEW_SECONDS);

        if (expired.isEmpty()) {
            return;
        }

        for (EntityStateDO state : expired) {
            try {
                processExpiredDevice(state);
            } catch (Exception e) {
                log.warn("Device expiry processing failed, deviceId={}", state.getEntityId(), e);
            }
        }
    }

    private void processExpiredDevice(EntityStateDO scanned) {
        // Write alarm row
        EntityStatusEnum prev = EntityStatusEnum.ofIndex(scanned.getLastStateFlag());
        String prevCode = Objects.nonNull(prev) ? prev.getCode() : "unknown";
        String message = String.format("Device heartbeat timed out (last=%s); marked OFFLINE", prevCode);

        EntityAlarmDO alarm = new EntityAlarmDO();
        alarm.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.DEVICE.getIndex());
        alarm.setEntityId(scanned.getEntityId());
        alarm.setDriverId(scanned.getParentEntityId());
        alarm.setDeviceId(scanned.getEntityId());
        alarm.setPointId(0L);
        alarm.setRuleId(0L);
        alarm.setRuleStateId(0L);
        alarm.setAlarmTypeFlag(AlarmTypeFlagEnum.OFFLINE.getIndex());
        alarm.setAlarmSourceFlag(AlarmSourceFlagEnum.STATE_TIMEOUT.getIndex());
        alarm.setAlarmLevelFlag(AlarmMessageLevelFlagEnum.P1.getIndex());
        alarm.setAlarmExt(JsonExt.builder().type("device-offline").content(message).version(1).build());
        alarm.setExpiredTime(0L);
        alarm.setConfirmFlag((byte) 0);
        alarm.setTenantId(scanned.getTenantId());
        entityAlarmManager.save(alarm);

        // Update lastAlarmId
        entityStateManager.lambdaUpdate()
                .eq(EntityStateDO::getTenantId, scanned.getTenantId())
                .eq(EntityStateDO::getId, scanned.getId())
                .eq(EntityStateDO::getEntityId, scanned.getEntityId())
                .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DEVICE.getIndex())
                .eq(EntityStateDO::getLeaseVersion, scanned.getLeaseVersion())
                .set(EntityStateDO::getLastAlarmId, alarm.getId())
                .update();

        // Trigger alarm rule pipeline
        DeviceAlarmDTO dto = DeviceAlarmDTO.builder()
                .driverId(scanned.getParentEntityId())
                .tenantId(scanned.getTenantId())
                .deviceId(scanned.getEntityId())
                .status(EntityStatusEnum.OFFLINE.getCode())
                .statusName(EntityStatusEnum.OFFLINE.name())
                .message(message)
                .alarmId(alarm.getId())
                .build();
        alarmRuleTriggerService.processDeviceAlarm(dto);

        log.info("Device scan marked OFFLINE: deviceId={}, tenantId={}, prevStatus={}",
                scanned.getEntityId(), scanned.getTenantId(), prevCode);
    }
}
