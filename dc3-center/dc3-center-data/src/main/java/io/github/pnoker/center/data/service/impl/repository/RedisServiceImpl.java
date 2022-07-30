/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.center.data.service.impl.repository;

import cn.hutool.core.util.StrUtil;
import io.github.pnoker.center.data.service.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.CacheConstant;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class RedisServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    private RedisUtil redisUtil;

    @Override
    public void savePointValue(PointValue pointValue) {
        if (!StrUtil.isAllNotEmpty(pointValue.getDeviceId(), pointValue.getPointId())) {
            return;
        }

        final String prefix = CacheConstant.Prefix.REAL_TIME_VALUE_KEY_PREFIX + pointValue.getDeviceId() + CommonConstant.Symbol.DOT;
        redisUtil.setKey(prefix + pointValue.getPointId(), pointValue);
    }

    @Override
    public void savePointValues(String deviceId, List<PointValue> pointValues) {
        if (StrUtil.isEmpty(deviceId)) {
            return;
        }

        final String prefix = CacheConstant.Prefix.REAL_TIME_VALUE_KEY_PREFIX + deviceId + CommonConstant.Symbol.DOT;
        Map<String, Object> collect = pointValues.stream()
                .filter(pointValue -> StrUtil.isNotEmpty(pointValue.getPointId()))
                .collect(Collectors.toMap(pointValue -> prefix + pointValue.getPointId(), pointValue -> pointValue));
        redisUtil.setKey(collect);
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(CommonConstant.RepositoryStrategy.REPOSITORY_STRATEGY_REDIS, this);
    }
}