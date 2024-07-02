/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
