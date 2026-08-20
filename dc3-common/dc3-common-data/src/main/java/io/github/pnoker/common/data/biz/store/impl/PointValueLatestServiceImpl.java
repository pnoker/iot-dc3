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

package io.github.pnoker.common.data.biz.store.impl;

import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.mapper.PointValueMapper;
import io.github.pnoker.common.entity.bo.PointValueBO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Default latest-value read over the shared {@code dc3_point_latest}
 * projection.
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Service
@RequiredArgsConstructor
public class PointValueLatestServiceImpl implements PointValueLatestService {

    private final PointValueMapper pointValueMapper;
    private final PointValueBuilder pointValueBuilder;

    @Override
    public PointValueBO latest(Long tenantId, Long deviceId, Long pointId) {
        return listLatest(tenantId, deviceId, List.of(pointId)).stream().findFirst().orElse(null);
    }

    @Override
    public List<PointValueBO> listLatest(Long tenantId, Long deviceId, List<Long> pointIds) {
        if (Objects.isNull(tenantId) || Objects.isNull(deviceId)
                || Objects.isNull(pointIds) || pointIds.isEmpty()) {
            return List.of();
        }
        return pointValueBuilder.buildBOListByDOList(pointValueMapper.selectLatestPointValues(tenantId, deviceId, pointIds));
    }
}
