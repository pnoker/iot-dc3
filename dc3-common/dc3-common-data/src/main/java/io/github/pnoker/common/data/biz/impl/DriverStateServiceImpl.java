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
import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.data.biz.DriverStateService;
import io.github.pnoker.common.data.cache.LocalCacheService;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.enums.DriverStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Business service implementation for driver heartbeat and state processing.
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverStateServiceImpl implements DriverStateService {

    private static final int STATUS_TTL_SECONDS = 45;

    private final LocalCacheService localCacheService;

    private final DriverAlarmService driverAlarmService;

    private static boolean isFlip(String prev, String current) {
        return online(prev) != online(current);
    }

    private static boolean online(String code) {
        return DriverStatusEnum.ONLINE.getCode().equals(code) || DriverStatusEnum.MAINTAIN.getCode().equals(code);
    }

    @Override
    public void heartbeat(DriverStateDTO entityDTO) {
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDriverId()) || Objects.isNull(entityDTO.getStatus())) {
            return;
        }

        String statusKey = PrefixConstant.DRIVER_STATUS_KEY_PREFIX + entityDTO.getDriverId();
        String prev = localCacheService.getKey(statusKey);
        String current = entityDTO.getStatus();

        localCacheService.setKey(statusKey, current, STATUS_TTL_SECONDS, TimeUnit.SECONDS);

        if (Objects.nonNull(prev) && !Objects.equals(prev, current) && isFlip(prev, current)) {
            String message = String.format("Driver status changed: %s -> %s", prev, current);
            DriverAlarmDTO alarm = DriverAlarmDTO.builder()
                    .tenantId(entityDTO.getTenantId())
                    .driverId(entityDTO.getDriverId())
                    .status(current)
                    .statusName(DriverStatusEnum.ofCode(current) != null ? DriverStatusEnum.ofCode(current).name() : current)
                    .message(message)
                    .build();
            driverAlarmService.alarm(alarm);
        }
    }

}
