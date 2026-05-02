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
import io.github.pnoker.common.data.biz.DriverEventService;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.data.dal.DriverEventManager;
import io.github.pnoker.common.data.entity.model.DriverEventDO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * DriverEventService Impl. Mirrors {@link DeviceEventServiceImpl}: the
 * heartbeat path refreshes the status key in {@link LocalCacheService} and
 * writes one row per event to {@code dc3_driver_event}, plus a derived ALARM
 * row whenever the driver status flips between ONLINE/MAINTAIN and
 * OFFLINE/FAULT.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverEventServiceImpl implements DriverEventService {

    private static final int STATUS_TTL_SECONDS = 10;

    @Resource
    private LocalCacheService localCacheService;

    @Resource
    private DriverEventManager driverEventManager;

    @Override
    public void heartbeatEvent(DriverEventDTO entityDTO) {
        DriverEventDTO.DriverStatus payload = JsonUtil.parseObject(entityDTO.getContent(), DriverEventDTO.DriverStatus.class);
        if (Objects.isNull(payload) || Objects.isNull(payload.getDriverId()) || Objects.isNull(payload.getStatus())) {
            return;
        }

        String statusKey = PrefixConstant.DRIVER_STATUS_KEY_PREFIX + payload.getDriverId();
        String prev = localCacheService.getKey(statusKey);
        String current = payload.getStatus().getCode();

        localCacheService.setKey(statusKey, current, STATUS_TTL_SECONDS, TimeUnit.SECONDS);

        persist(payload, DriverEventTypeEnum.HEARTBEAT, "driver-heartbeat", entityDTO.getContent());

        if (prev != null && !Objects.equals(prev, current) && isFlip(prev, current)) {
            String message = String.format("Driver status changed: %s -> %s", prev, current);
            persist(payload, DriverEventTypeEnum.ALARM, "driver-state-flip", message);
        }
    }

    @Override
    public void alarmEvent(DriverEventDTO entityDTO) {
        DriverEventDTO.DriverStatus payload = JsonUtil.parseObject(entityDTO.getContent(), DriverEventDTO.DriverStatus.class);
        if (Objects.isNull(payload) || Objects.isNull(payload.getDriverId())) {
            log.warn("Drop driver alarm without driverId: {}", entityDTO.getContent());
            return;
        }
        String msg = payload.getMessage() != null ? payload.getMessage() : entityDTO.getContent();
        persist(payload, DriverEventTypeEnum.ALARM, "driver-alarm", msg);
    }

    private void persist(DriverEventDTO.DriverStatus payload, DriverEventTypeEnum type, String extType, String extContent) {
        DriverEventDO entity = new DriverEventDO();
        entity.setDriverId(payload.getDriverId());
        entity.setEventTypeFlag(type.getIndex());
        entity.setEventExt(JsonExt.builder().type(extType).content(extContent).version(1).build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(payload.getTenantId() != null ? payload.getTenantId() : 0L);
        driverEventManager.save(entity);
    }

    private static boolean isFlip(String prev, String current) {
        return online(prev) != online(current);
    }

    private static boolean online(String code) {
        return DriverStatusEnum.ONLINE.getCode().equals(code) || DriverStatusEnum.MAINTAIN.getCode().equals(code);
    }
}
