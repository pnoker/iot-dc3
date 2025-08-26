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
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.redis.service.RedisService;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * DeviceService Impl
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DeviceEventServiceImpl implements DeviceEventService {

    @Resource
    private RedisService redisService;


    @Override
    public void heartbeatEvent(DeviceEventDTO entityDTO) {
        DeviceEventDTO.DeviceStatus deviceStatus = JsonUtil.parseObject(entityDTO.getContent(), DeviceEventDTO.DeviceStatus.class);
        if (Objects.isNull(deviceStatus)) {
            return;
        }
        redisService.setKey(PrefixConstant.DEVICE_STATUS_KEY_PREFIX + deviceStatus.getDeviceId(), deviceStatus.getStatus(), deviceStatus.getTimeOut(), deviceStatus.getTimeUnit());
    }

}
