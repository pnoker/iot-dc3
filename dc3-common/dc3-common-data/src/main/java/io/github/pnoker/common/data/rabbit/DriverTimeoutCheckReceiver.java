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

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.repository.ReactiveEntityAlarmStore;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverTimeoutCheckDTO;
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
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

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

    private final ReactiveEntityStateStore entityStateStore;
    private final ReactiveEntityAlarmStore entityAlarmStore;
    private final AlarmRuleTriggerService alarmRuleTriggerService;
    private final TransactionalOperator transactionalOperator;

    /**
     * Consume a driver timeout check and, after re-verifying the lease version and
     * expiry to avoid racing a newer heartbeat, marks an expired driver offline and
     * raises a driver alarm.
     *
     * @param message the raw message carrying the delivery tag
     * @param ack     acknowledgment handle for the message
     */
    @Dc3Listener(topic = MqTopic.STATE_TIMEOUT)
    public Mono<Void> driverTimeoutCheck(MqReceived<DriverTimeoutCheckDTO> message, Acknowledgment ack) {
        DriverTimeoutCheckDTO dto = message.payload();
        if (Objects.isNull(dto)
                || Objects.isNull(dto.getDriverId())
                || Objects.isNull(dto.getTenantId())
                || Objects.isNull(dto.getLeaseVersion())) {
            ack.reject(false);
            return Mono.empty();
        }
        return transactionalOperator
                .transactional(entityStateStore
                        .claimExpired(
                                dto.getTenantId(),
                                EntityTypeEnum.DRIVER,
                                dto.getDriverId(),
                                dto.getLeaseVersion(),
                                OFFLINE_RENEW_SECONDS)
                        .flatMap(this::persistOrResumeAlarm))
                .flatMap(context -> alarmRuleTriggerService.processDriverAlarm(context.alarm()))
                .doOnError(error -> log.error("Driver timeout check failed.", error))
                .then();
    }

    private Mono<DriverAlarmContext> persistOrResumeAlarm(ReactiveEntityStateStore.EntityStateLease state) {
        String previous = statusCode(state.lastStateFlag());
        String alarmMessage = String.format("Driver heartbeat timed out (last=%s); marked OFFLINE", previous);
        Mono<Long> alarmId = state.lastAlarmId() != null && state.lastAlarmId() > 0
                ? Mono.just(state.lastAlarmId())
                : persistAlarm(state, alarmMessage);
        return alarmId.map(id -> new DriverAlarmContext(DriverAlarmDTO.builder()
                        .tenantId(state.tenantId())
                        .driverId(state.entityId())
                        .status(EntityStatusEnum.OFFLINE.getCode())
                        .statusName(EntityStatusEnum.OFFLINE.name())
                        .message(alarmMessage)
                        .alarmId(id)
                        .build()))
                .doOnSuccess(ignored -> log.info(
                        "Driver timeout check confirmed OFFLINE: driverId={}, tenantId={}, prevStatus={}",
                        state.entityId(),
                        state.tenantId(),
                        previous));
    }

    private Mono<Long> persistAlarm(ReactiveEntityStateStore.EntityStateLease state, String alarmMessage) {
        EntityAlarmDO alarm = new EntityAlarmDO();
        alarm.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.DRIVER.getIndex());
        alarm.setEntityId(state.entityId());
        alarm.setDriverId(state.entityId());
        alarm.setDeviceId(0L);
        alarm.setPointId(0L);
        alarm.setRuleId(0L);
        alarm.setRuleStateId(0L);
        alarm.setAlarmTypeFlag(AlarmTypeEnum.OFFLINE.getIndex());
        alarm.setAlarmSourceFlag(AlarmSourceTypeEnum.STATE_TIMEOUT.getIndex());
        alarm.setAlarmLevelFlag(AlarmMessageLevelEnum.P1.getIndex());
        alarm.setAlarmExt(JsonExt.builder()
                .type("driver-offline")
                .content(alarmMessage)
                .version(1)
                .build());
        alarm.setExpiredTime(0L);
        alarm.setConfirmFlag((byte) 0);
        alarm.setTenantId(state.tenantId());
        return entityAlarmStore
                .insert(alarm)
                .flatMap(saved -> entityStateStore
                        .markAlarm(
                                state.tenantId(),
                                EntityTypeEnum.DRIVER,
                                state.entityId(),
                                state.leaseVersion(),
                                saved.getId())
                        .flatMap(updated -> updated
                                ? Mono.just(saved.getId())
                                : Mono.error(new IllegalStateException("driver timeout alarm lost lease ownership"))));
    }

    private String statusCode(byte flag) {
        EntityStatusEnum status = EntityStatusEnum.ofIndex(flag);
        return Objects.isNull(status) ? "unknown" : status.getCode();
    }

    private record DriverAlarmContext(DriverAlarmDTO alarm) {}
}
