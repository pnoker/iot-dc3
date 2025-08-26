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
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.redis.service.RedisService;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * DriverService Impl
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverEventServiceImpl implements DriverEventService {

    @Resource
    private RedisService redisService;


    @Override
    public void heartbeatEvent(DriverEventDTO entityDTO) {
        DriverEventDTO.DriverStatus driverStatus = JsonUtil.parseObject(entityDTO.getContent(), DriverEventDTO.DriverStatus.class);
        if (Objects.isNull(driverStatus)) {
            return;
        }
        redisService.setKey(PrefixConstant.DRIVER_STATUS_KEY_PREFIX + driverStatus.getDriverId(), driverStatus.getStatus(), 10, TimeUnit.SECONDS);
    }

}
