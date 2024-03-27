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

package io.github.pnoker.center.data.repository.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.repository.RepositoryService;
import io.github.pnoker.common.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class RedisServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    private RedisService redisService;

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.REDIS;
    }

    @Override
    public void savePointValue(PointValueBO pointValueBO) {
        if (!ObjectUtil.isAllNotEmpty(pointValueBO.getDeviceId(), pointValueBO.getPointId())) {
            return;
        }

        final String prefix = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + pointValueBO.getDeviceId() + SymbolConstant.DOT;
        redisService.setKey(prefix + pointValueBO.getPointId(), pointValueBO);
    }

    @Override
    public void savePointValues(Long deviceId, List<PointValueBO> pointValueBOS) {
        if (ObjectUtil.isEmpty(deviceId)) {
            return;
        }

        final String prefix = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + deviceId + SymbolConstant.DOT;
        Map<String, PointValueBO> collect = pointValueBOS.stream()
                .filter(pointValue -> ObjectUtil.isNotEmpty(pointValue.getPointId()))
                .collect(Collectors.toMap(pointValue -> prefix + pointValue.getPointId(), pointValue -> pointValue));
        redisService.setKey(collect);
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.REDIS, this);
    }
}