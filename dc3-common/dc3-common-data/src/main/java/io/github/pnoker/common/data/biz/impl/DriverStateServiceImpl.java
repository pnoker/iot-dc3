package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.data.biz.DriverStateService;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.entity.dto.DriverTimeoutCheckDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.TimeoutSourceTypeEnum;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.sender.ReactiveMessageSender;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Reactive driver heartbeat and lease service. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverStateServiceImpl implements DriverStateService {

    private static final int STATUS_TIMEOUT_SECONDS = 45;
    private final DriverAlarmService driverAlarmService;
    private final ReactiveEntityStateStore stateStore;
    private final ReactiveMessageSender messageSender;

    @Override
    public Mono<Void> heartbeat(DriverStateDTO event) {
        if (event == null || event.getDriverId() == null || event.getTenantId() == null || event.getStatus() == null) {
            return Mono.empty();
        }
        EntityStatusEnum current = EntityStatusEnum.ofCode(event.getStatus());
        if (current == null) current = EntityStatusEnum.OFFLINE;
        Map<String, Object> ext = new HashMap<>();
        ext.put("type", "driver-heartbeat");
        ext.put("content", event.getStateDescription() == null ? "" : event.getStateDescription());
        ext.put("version", 1);
        EntityStatusEnum status = current;
        return stateStore.upsert(null, event.getTenantId(), EntityTypeEnum.DRIVER, event.getDriverId(), 0L,
                        status.getIndex(), EntityStatusEnum.OFFLINE.getIndex(), Instant.now(), STATUS_TIMEOUT_SECONDS,
                        TimeoutSourceTypeEnum.SYSTEM.getIndex(), JsonUtil.toJsonString(ext))
                .flatMap(state -> messageSender.sendConfirmed(MqMessage.of(MqTopic.STATE_TIMEOUT, "",
                                        DriverTimeoutCheckDTO.builder().driverId(event.getDriverId())
                                                .leaseVersion(state.leaseVersion()).tenantId(event.getTenantId()).build()))
                        .then(alarmIfFlipped(event, state.lastStateFlag(), status)));
    }

    private Mono<Void> alarmIfFlipped(DriverStateDTO event, byte previous, EntityStatusEnum current) {
        if (online(previous) == online(current)) return Mono.empty();
        DriverAlarmDTO alarm = DriverAlarmDTO.builder().tenantId(event.getTenantId()).driverId(event.getDriverId())
                .status(current.getCode()).statusName(current.name())
                .message(String.format("Driver status changed: %s -> %s", code(previous), current.getCode())).build();
        return driverAlarmService.alarm(alarm);
    }
    private boolean online(byte value) { return value == EntityStatusEnum.ONLINE.getIndex() || value == EntityStatusEnum.MAINTAIN.getIndex(); }
    private boolean online(EntityStatusEnum value) { return value == EntityStatusEnum.ONLINE || value == EntityStatusEnum.MAINTAIN; }
    private String code(byte value) { EntityStatusEnum state = EntityStatusEnum.ofIndex(value); return state == null ? DataConstant.STATUS_UNKNOWN : state.getCode(); }
}
