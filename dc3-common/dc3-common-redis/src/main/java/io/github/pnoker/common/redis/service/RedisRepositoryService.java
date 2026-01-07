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

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.redis.entity.builder.RedisPointValueBuilder;
import io.github.pnoker.common.redis.entity.model.RedisPointValueDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Redis Repository Service Implementation
 * <p>
 * Service implementation for storing and retrieving point values in Redis.
 * Provides operations for saving single or multiple point values and
 * querying latest values by device and point IDs.
 * Uses Redis key prefixes for organized data storage.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class RedisRepositoryService {

    @Resource
    private RedisPointValueBuilder redisPointValueBuilder;

    @Resource
    private RedisService redisService;

    /**
     * Save a single point value to Redis
     *
     * @param entityBO Point value business object to save
     */
    public void savePointValue(PointValueBO entityBO) {
        if (Objects.isNull(entityBO.getDeviceId()) || Objects.isNull(entityBO.getPointId())) {
            return;
        }

        final String prefix = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + entityBO.getDeviceId() + SymbolConstant.DOT;
        RedisPointValueDO entityDO = redisPointValueBuilder.buildDOByBO(entityBO);
        redisService.setKey(prefix + entityBO.getPointId(), entityDO);
    }

    /**
     * Save multiple point values for a device to Redis
     *
     * @param deviceId     Device ID for the point values
     * @param entityBOList List of point value business objects to save
     */
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

    /**
     * Select latest point values by device ID and point IDs
     *
     * @param deviceId Device ID to query
     * @param pointIds List of point IDs to retrieve
     * @return Map of point ID to point value business object
     */
    public Map<Long, PointValueBO> selectLatestPointValue(Long deviceId, List<Long> pointIds) {
        if (CollectionUtils.isEmpty(pointIds)) {
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