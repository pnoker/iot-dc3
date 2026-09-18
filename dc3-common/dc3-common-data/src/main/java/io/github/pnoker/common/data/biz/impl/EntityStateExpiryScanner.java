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

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.repository.ReactiveEntityAlarmStore;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelEnum;
import io.github.pnoker.common.enums.AlarmSourceTypeEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.AlarmTypeEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.sender.MessageSender;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive scanner that fences and expires device leases. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityStateExpiryScanner {

    private static final String TICK_BODY = "tick";
    private static final int OFFLINE_RENEW_SECONDS = 300;
    private static final int BATCH_LIMIT = 500;

    private final ReactiveEntityStateStore stateStore;
    private final ReactiveEntityAlarmStore entityAlarmStore;
    private final AlarmRuleTriggerService alarmRuleTriggerService;
    private final MessageSender messageSender;

    @EventListener(ApplicationReadyEvent.class)
    void publishInitialTick() {
        messageSender.send(MqMessage.of(MqTopic.DEVICE_SCAN, "", TICK_BODY));
    }

    /** Scan for expired entity leases and mark them offline. */
    @Dc3Listener(topic = MqTopic.DEVICE_SCAN)
    public Mono<Void> onScanTick(MqReceived<Object> message, Acknowledgment ack) {
        return scanExpiredDevices()
                .then(Mono.fromRunnable(() -> messageSender.send(MqMessage.of(MqTopic.DEVICE_SCAN, "", TICK_BODY))))
                .doOnError(error -> log.error("Device scan tick failed", error))
                .then();
    }

    private Mono<Void> scanExpiredDevices() {
        return stateStore
                .claimExpired(EntityTypeEnum.DEVICE, BATCH_LIMIT, OFFLINE_RENEW_SECONDS)
                .collectList()
                .flatMap(claimed -> {
                    if (claimed.isEmpty()) return Mono.empty();
                    List<ExpiredDeviceContext> contexts = claimed.stream()
                            .map(state -> new ExpiredDeviceContext(state, buildOfflineAlarm(state)))
                            .toList();
                    return entityAlarmStore
                            .insertBatch(contexts.stream()
                                    .map(ExpiredDeviceContext::alarm)
                                    .toList())
                            .flatMapMany(saved -> {
                                if (saved.size() != contexts.size()
                                        || saved.stream().anyMatch(alarm -> alarm.getId() == null)) {
                                    return Flux.error(
                                            new IllegalStateException("failed to persist device expiry alarms"));
                                }
                                return Flux.fromIterable(contexts).concatMap(this::completeExpiredDevice);
                            })
                            .then();
                });
    }

    private EntityAlarmDO buildOfflineAlarm(ReactiveEntityStateStore.EntityStateLease state) {
        EntityAlarmDO alarm = new EntityAlarmDO();
        alarm.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.DEVICE.getIndex());
        alarm.setEntityId(state.entityId());
        alarm.setDriverId(state.parentEntityId());
        alarm.setDeviceId(state.entityId());
        alarm.setPointId(0L);
        alarm.setRuleId(0L);
        alarm.setRuleStateId(0L);
        alarm.setAlarmTypeFlag(AlarmTypeEnum.OFFLINE.getIndex());
        alarm.setAlarmSourceFlag(AlarmSourceTypeEnum.STATE_TIMEOUT.getIndex());
        alarm.setAlarmLevelFlag(AlarmMessageLevelEnum.P1.getIndex());
        String previous = statusCode(state.lastStateFlag());
        alarm.setAlarmExt(JsonExt.builder()
                .type("device-offline")
                .content(String.format("Device heartbeat timed out (last=%s); marked OFFLINE", previous))
                .version(1)
                .build());
        alarm.setExpiredTime(0L);
        alarm.setConfirmFlag((byte) 0);
        alarm.setTenantId(state.tenantId());
        return alarm;
    }

    private Mono<Void> completeExpiredDevice(ExpiredDeviceContext context) {
        ReactiveEntityStateStore.EntityStateLease state = context.state();
        EntityAlarmDO alarm = context.alarm();
        return stateStore
                .markAlarm(
                        state.tenantId(), EntityTypeEnum.DEVICE, state.entityId(), state.leaseVersion(), alarm.getId())
                .flatMap(updated -> {
                    if (!updated) return Mono.empty();
                    String previous = statusCode(state.lastStateFlag());
                    return alarmRuleTriggerService.processDeviceAlarm(DeviceAlarmDTO.builder()
                            .driverId(state.parentEntityId())
                            .tenantId(state.tenantId())
                            .deviceId(state.entityId())
                            .status(EntityStatusEnum.OFFLINE.getCode())
                            .statusName(EntityStatusEnum.OFFLINE.name())
                            .message(String.format("Device heartbeat timed out (last=%s); marked OFFLINE", previous))
                            .alarmId(alarm.getId())
                            .build());
                });
    }

    private String statusCode(byte flag) {
        EntityStatusEnum status = EntityStatusEnum.ofIndex(flag);
        return status == null ? DataConstant.STATUS_UNKNOWN : status.getCode();
    }

    private record ExpiredDeviceContext(ReactiveEntityStateStore.EntityStateLease state, EntityAlarmDO alarm) {}
}
