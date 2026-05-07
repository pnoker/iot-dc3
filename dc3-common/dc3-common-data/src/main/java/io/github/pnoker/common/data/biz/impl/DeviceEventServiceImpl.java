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
import io.github.pnoker.common.data.biz.DeviceEventService;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.dal.DeviceEventManager;
import io.github.pnoker.common.data.entity.model.DeviceEventDO;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * DeviceEventService Impl. Persists heartbeat/alarm events to {@code dc3_device_event}
 * and keeps the online-status key in {@link LocalCacheService} up to date. When a
 * heartbeat flips the device status between ONLINE/MAINTAIN and OFFLINE/FAULT, a derived
 * ALARM row is also written so the operator can see state transitions without relying on
 * drivers to report them explicitly.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DeviceEventServiceImpl implements DeviceEventService {

    @Resource
    private LocalCacheService localCacheService;

    @Resource
    private DeviceEventManager deviceEventManager;

    /**
     * Flip between the online family (ONLINE / MAINTAIN) and the unavailable family
     * (OFFLINE / FAULT) is a user-visible transition worth a derived ALARM. Within-family
     * flips (e.g. ONLINE -> MAINTAIN) are not.
     */
    private static boolean isFlip(String prev, String current) {
        return online(prev) != online(current);
    }

    private static boolean online(String code) {
        return DeviceStatusEnum.ONLINE.getCode().equals(code) || DeviceStatusEnum.MAINTAIN.getCode().equals(code);
    }

    @Override
    public void heartbeatEvent(DeviceEventDTO entityDTO) {
        DeviceEventDTO.DeviceStatus payload = JsonUtil.parseObject(entityDTO.getContent(),
                DeviceEventDTO.DeviceStatus.class);
        if (Objects.isNull(payload) || Objects.isNull(payload.getDeviceId()) || Objects.isNull(payload.getStatus())) {
            return;
        }

        String statusKey = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + payload.getDeviceId();
        String prev = localCacheService.getKey(statusKey);
        String current = payload.getStatus().getCode();

        // Refresh the online-status cache so the dashboard's "online" badge reacts
        // immediately.
        // Heartbeats do NOT write to dc3_device_event any more — they'd flood the table
        // at
        // the heartbeat rate (potentially per-second per device). The cache entry alone
        // is
        // enough to drive the online badge, and an ALARM row is still persisted on a real
        // state flip below.
        localCacheService.setKey(statusKey, current, payload.getTimeOut(), payload.getTimeUnit());

        // Derive an ALARM row on state flips so operators see transitions in the alert
        // list.
        if (prev != null && !Objects.equals(prev, current) && isFlip(prev, current)) {
            String message = String.format("Device status changed: %s -> %s", prev, current);
            persist(payload, DeviceEventTypeEnum.ALARM, "device-state-flip", message);
        }
    }

    @Override
    public void alarmEvent(DeviceEventDTO entityDTO) {
        DeviceEventDTO.DeviceStatus payload = JsonUtil.parseObject(entityDTO.getContent(),
                DeviceEventDTO.DeviceStatus.class);
        if (Objects.isNull(payload) || Objects.isNull(payload.getDeviceId())) {
            log.warn("Drop device alarm without deviceId: {}", entityDTO.getContent());
            return;
        }
        String msg = payload.getMessage() != null ? payload.getMessage() : entityDTO.getContent();
        persist(payload, DeviceEventTypeEnum.ALARM, "device-alarm", msg);
    }

    private void persist(DeviceEventDTO.DeviceStatus payload, DeviceEventTypeEnum type, String extType,
                         String extContent) {
        DeviceEventDO entity = new DeviceEventDO();
        entity.setDeviceId(payload.getDeviceId());
        entity.setPointId(0L);
        entity.setEventTypeFlag(type.getIndex());
        entity.setEventExt(JsonExt.builder().type(extType).content(extContent).version(1).build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(payload.getTenantId() != null ? payload.getTenantId() : 0L);
        deviceEventManager.save(entity);
    }

}
