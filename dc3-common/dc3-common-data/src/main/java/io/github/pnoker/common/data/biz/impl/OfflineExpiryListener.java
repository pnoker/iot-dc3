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
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.dal.DeviceEventManager;
import io.github.pnoker.common.data.dal.DriverEventManager;
import io.github.pnoker.common.data.entity.model.DeviceEventDO;
import io.github.pnoker.common.data.entity.model.DriverEventDO;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Turns expired online-status cache keys into OFFLINE alarm rows in
 * {@code dc3_device_event} / {@code dc3_driver_event}. Without this a driver or device
 * that stops sending heartbeats would silently drop off the dashboard — the badge would
 * show the right online/offline split thanks to the cache miss, but nothing would land in
 * the alert log explaining why.
 *
 * <p>
 * Hooks into {@link LocalCacheService}'s expiry listener so we don't need a polling
 * thread. Callbacks run off the Caffeine removal path onto the common ForkJoinPool so a
 * slow gRPC facade lookup can't stall the cache.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Slf4j
@Component
public class OfflineExpiryListener {

    @Resource
    private LocalCacheService localCacheService;

    @Resource
    private DriverFacade driverFacade;

    @Resource
    private DeviceFacade deviceFacade;

    @Resource
    private DriverEventManager driverEventManager;

    @Resource
    private DeviceEventManager deviceEventManager;

    @Resource
    private AlarmRuleTriggerService alarmRuleTriggerService;

    private static Long parseIdSuffix(String key, String prefix) {
        try {
            return Long.parseLong(key.substring(prefix.length()));
        } catch (Exception e) {
            log.debug("Unexpected status key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    @PostConstruct
    void register() {
        localCacheService.onExpire(this::onExpire);
    }

    private void onExpire(String key, Object lastValue) {
        if (Objects.isNull(key))
            return;
        final String lastStatus = lastValue instanceof String s ? s : null;
        // Skip if the key was already offline — avoids a duplicate alarm on
        // the same outage when the cache is manually re-seeded below.
        CompletableFuture.runAsync(() -> {
            try {
                if (key.startsWith(PrefixConstant.DRIVER_STATUS_KEY_PREFIX)) {
                    handleDriverExpiry(key, lastStatus);
                } else if (key.startsWith(PrefixConstant.DEVICE_STATUS_KEY_PREFIX)) {
                    handleDeviceExpiry(key, lastStatus);
                }
            } catch (Exception e) {
                log.warn("Offline expiry handling failed, key={}", key, e);
            }
        });
    }

    private void handleDriverExpiry(String key, String lastStatus) {
        if (Objects.equals(lastStatus, DriverStatusEnum.OFFLINE.getCode()))
            return;
        Long id = parseIdSuffix(key, PrefixConstant.DRIVER_STATUS_KEY_PREFIX);
        if (Objects.isNull(id))
            return;

        FacadeDriverBO driver = driverFacade.selectById(id);
        if (Objects.isNull(driver)) {
            log.debug("Driver {} not found when handling offline expiry", id);
            return;
        }

        String message = String.format("Driver heartbeat timed out (last=%s); marked OFFLINE", lastStatus);
        DriverEventDO entity = new DriverEventDO();
        entity.setDriverId(id);
        entity.setEventTypeFlag(DriverEventTypeEnum.ALARM.getIndex());
        entity.setEventExt(JsonExt.builder()
                .type("driver-offline")
                .content(message)
                .version(1)
                .build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(Objects.nonNull(driver.getTenantId()) ? driver.getTenantId() : 0L);
        driverEventManager.save(entity);
        DriverEventDTO.DriverStatus payload = new DriverEventDTO.DriverStatus(id, DriverStatusEnum.OFFLINE);
        payload.setTenantId(driver.getTenantId());
        payload.setMessage(message);
        alarmRuleTriggerService.processDriverEvent(payload, DriverEventTypeEnum.ALARM, "driver-offline",
                entity.getId());

        // Deliberately do NOT re-seed the cache. The previous version wrote
        // OFFLINE back with a 1-day TTL so the dashboard "saw" the state,
        // but that made every subsequent heartbeat look like an
        // OFFLINE→ONLINE flip and fire a state-flip ALARM, producing an
        // alarm storm every cron cycle. Consumers (DriverStatusServiceImpl,
        // SystemHealthServiceImpl) already treat a missing key as OFFLINE,
        // so leaving the key evicted gives the right UI behaviour without
        // any spurious flip alarms.
    }

    private void handleDeviceExpiry(String key, String lastStatus) {
        if (Objects.equals(lastStatus, DeviceStatusEnum.OFFLINE.getCode()))
            return;
        Long id = parseIdSuffix(key, PrefixConstant.DEVICE_STATUS_KEY_PREFIX);
        if (Objects.isNull(id))
            return;

        FacadeDeviceBO device = deviceFacade.selectById(id);
        if (Objects.isNull(device)) {
            log.debug("Device {} not found when handling offline expiry", id);
            return;
        }

        String message = String.format("Device heartbeat timed out (last=%s); marked OFFLINE", lastStatus);
        DeviceEventDO entity = new DeviceEventDO();
        entity.setDeviceId(id);
        entity.setPointId(0L);
        entity.setEventTypeFlag(DeviceEventTypeEnum.ALARM.getIndex());
        entity.setEventExt(JsonExt.builder()
                .type("device-offline")
                .content(message)
                .version(1)
                .build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(Objects.nonNull(device.getTenantId()) ? device.getTenantId() : 0L);
        deviceEventManager.save(entity);
        DeviceEventDTO.DeviceStatus payload = new DeviceEventDTO.DeviceStatus(id, DeviceStatusEnum.OFFLINE);
        payload.setTenantId(device.getTenantId());
        payload.setDriverId(device.getDriverId());
        payload.setMessage(message);
        alarmRuleTriggerService.processDeviceEvent(payload, DeviceEventTypeEnum.ALARM, "device-offline",
                entity.getId());

        // See handleDriverExpiry — don't re-seed: the previous OFFLINE
        // re-seed with a 1-day TTL turned every subsequent heartbeat into
        // a state-flip ALARM. Consumers read a missing key as OFFLINE.
    }

}
