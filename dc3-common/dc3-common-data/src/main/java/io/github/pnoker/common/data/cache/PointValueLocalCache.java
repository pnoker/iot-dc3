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

package io.github.pnoker.common.data.cache;

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.bo.PointValueBO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Local in-process hot cache for the latest point values.
 * <p>
 * Replaces the previous Redis-backed repository layer with a Caffeine cache kept inside
 * the data service JVM. Misses fall through to the underlying time-series repository,
 * exactly as the Redis version did.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class PointValueLocalCache {

    private final LocalCacheImpl localCacheService;

    public void savePointValue(PointValueBO entityBO) {
        if (Objects.isNull(entityBO.getTenantId()) || Objects.isNull(entityBO.getDeviceId())
                || Objects.isNull(entityBO.getPointId())) {
            return;
        }
        String key = buildKey(entityBO.getTenantId(), entityBO.getDeviceId(), entityBO.getPointId());
        localCacheService.setKey(key, entityBO);
    }

    public void savePointValue(Long deviceId, List<PointValueBO> entityBOList) {
        if (Objects.isNull(deviceId) || CollectionUtils.isEmpty(entityBOList)) {
            return;
        }
        Map<String, PointValueBO> valuesMap = entityBOList.stream()
                .filter(entityBO -> Objects.nonNull(entityBO.getTenantId()) && Objects.nonNull(entityBO.getPointId()))
                .collect(Collectors.toMap(entityBO -> buildKey(entityBO.getTenantId(), deviceId, entityBO.getPointId()),
                        Function.identity()));
        localCacheService.setKey(valuesMap);
    }

    public Map<Long, PointValueBO> selectLatestPointValue(Long tenantId, Long deviceId, List<Long> pointIds) {
        if (Objects.isNull(tenantId) || Objects.isNull(deviceId) || CollectionUtils.isEmpty(pointIds)) {
            return Collections.emptyMap();
        }
        List<String> keys = pointIds.stream().map(pointId -> buildKey(tenantId, deviceId, pointId)).toList();
        List<PointValueBO> hits = localCacheService.getKey(keys);
        return hits.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PointValueBO::getPointId, Function.identity()));
    }

    private String buildKey(Long tenantId, Long deviceId, Long pointId) {
        return PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + tenantId + SymbolConstant.DOT + deviceId + SymbolConstant.DOT
                + pointId;
    }

}
