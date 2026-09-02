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

import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.DeviceAlarmService;
import io.github.pnoker.common.data.biz.DeviceStateService;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.TimeoutSourceTypeEnum;
import io.github.pnoker.common.utils.JsonUtil;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Reactive device heartbeat and lease service. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStateServiceImpl implements DeviceStateService {

    private final DeviceAlarmService deviceAlarmService;
    private final ReactiveEntityStateStore stateStore;

    @Override
    public Mono<Void> heartbeat(DeviceStateDTO event) {
        if (event == null
                || event.getDeviceId() == null
                || event.getDriverId() == null
                || event.getTenantId() == null
                || event.getStatus() == null
                || event.getTimeoutUnit() == null
                || event.getTimeout() <= 0) return Mono.empty();
        long ttl = event.getTimeoutUnit().toSeconds(event.getTimeout());
        if (ttl <= 0 || ttl > Integer.MAX_VALUE) return Mono.empty();
        EntityStatusEnum current = EntityStatusEnum.ofCode(event.getStatus());
        if (current == null) current = EntityStatusEnum.OFFLINE;
        Map<String, Object> ext = new HashMap<>();
        ext.put("type", "device-heartbeat");
        ext.put("content", event.getStateDescription() == null ? "" : event.getStateDescription());
        ext.put("version", 1);
        EntityStatusEnum status = current;
        return stateStore
                .upsert(
                        null,
                        event.getTenantId(),
                        EntityTypeEnum.DEVICE,
                        event.getDeviceId(),
                        event.getDriverId(),
                        status.getIndex(),
                        EntityStatusEnum.OFFLINE.getIndex(),
                        Instant.now(),
                        (int) ttl,
                        TimeoutSourceTypeEnum.DRIVER.getIndex(),
                        JsonUtil.toJsonString(ext))
                .flatMap(state -> {
                    if (!isFlip(state.lastStateFlag(), status)) return Mono.empty();
                    String previous = code(state.lastStateFlag());
                    DeviceAlarmDTO alarm = DeviceAlarmDTO.builder()
                            .driverId(event.getDriverId())
                            .tenantId(event.getTenantId())
                            .deviceId(event.getDeviceId())
                            .status(status.getCode())
                            .statusName(status.name())
                            .message(String.format("Device status changed: %s -> %s", previous, status.getCode()))
                            .build();
                    return deviceAlarmService.alarm(alarm);
                });
    }

    private boolean isFlip(byte previous, EntityStatusEnum current) {
        return online(previous) != online(current);
    }

    private boolean online(byte value) {
        return value == EntityStatusEnum.ONLINE.getIndex() || value == EntityStatusEnum.MAINTAIN.getIndex();
    }

    private boolean online(EntityStatusEnum value) {
        return value == EntityStatusEnum.ONLINE || value == EntityStatusEnum.MAINTAIN;
    }

    private String code(byte value) {
        EntityStatusEnum status = EntityStatusEnum.ofIndex(value);
        return status == null ? DataConstant.STATUS_UNKNOWN : status.getCode();
    }
}
