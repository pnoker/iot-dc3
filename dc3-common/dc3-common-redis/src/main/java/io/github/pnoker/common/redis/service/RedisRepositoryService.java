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

package io.github.pnoker.common.redis.service;

import cn.hutool.core.collection.CollUtil;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.redis.entity.builder.RedisPointValueBuilder;
import io.github.pnoker.common.redis.entity.model.RedisPointValueDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class RedisRepositoryService {

    @Resource
    private RedisPointValueBuilder redisPointValueBuilder;

    @Resource
    private RedisService redisService;

    public void savePointValue(PointValueBO entityBO) {
        if (Objects.isNull(entityBO.getDeviceId()) || Objects.isNull(entityBO.getPointId())) {
            return;
        }

        final String prefix = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + entityBO.getDeviceId() + SymbolConstant.DOT;
        RedisPointValueDO entityDO = redisPointValueBuilder.buildDOByBO(entityBO);
        redisService.setKey(prefix + entityBO.getPointId(), entityDO);
    }

    public void savePointValue(Long deviceId, List<PointValueBO> entityBOList) {
        if (Objects.isNull(deviceId)) {
            return;
        }

        final String prefix = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + deviceId + SymbolConstant.DOT;
        List<RedisPointValueDO> entityDOList = redisPointValueBuilder.buildDOListByBOList(entityBOList);
        Map<String, RedisPointValueDO> entityDOMap = entityDOList.stream()
                .filter(entityBO -> Objects.nonNull(entityBO.getPointId()))
                .collect(Collectors.toMap(entityBO -> prefix + entityBO.getPointId(), Function.identity()));
        redisService.setKey(entityDOMap);
    }

    public Map<Long, PointValueBO> selectLatestPointValue(Long deviceId, List<Long> pointIds) {
        if (CollUtil.isEmpty(pointIds)) {
            return Collections.emptyMap();
        }

        String prefix = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + deviceId + SymbolConstant.DOT;
        List<String> keys = pointIds.stream().map(pointId -> prefix + pointId).toList();
        List<RedisPointValueDO> entityDOList = redisService.getKey(keys);
        entityDOList = entityDOList.stream().filter(Objects::nonNull).toList();
        List<PointValueBO> entityBOList = redisPointValueBuilder.buildBOListByDOList(entityDOList);
        return entityBOList.stream().collect(Collectors.toMap(PointValueBO::getPointId, Function.identity()));
    }

}