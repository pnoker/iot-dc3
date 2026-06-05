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

package io.github.pnoker.common.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.entity.bo.WindowAggregateResult;
import io.github.pnoker.common.entity.query.WindowAggregateRequest;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Point value Mapper.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface PointValueMapper extends BaseMapper<PointValueDO> {

    /**
     * Run a SQL aggregate (AVG/MIN/MAX/SUM/COUNT) over the rows that match
     * the tenant/device/point bracket and fall inside {@code [from, to)}.
     * The function name is selected by {@code <choose>} in the XML so the
     * parameter is safe from injection. Returns null aggregate + zero count
     * when the window is empty.
     */
    WindowAggregateResult aggregateInWindow(@Param("request") WindowAggregateRequest request);

    /**
     * Pull raw rows in {@code [from, to)} ordered oldest → newest. Used by
     * ALL/ANY long-window paths where the rule condition has to run per row.
     */
    List<PointValueDO> samplesInWindow(@Param("tenantId") Long tenantId,
                                       @Param("deviceId") Long deviceId,
                                       @Param("pointId") Long pointId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /**
     * Batch query the latest point value for each point within a single device.
     * Uses PostgreSQL {@code DISTINCT ON} to pick the row with the most recent
     * {@code create_time} per {@code (device_id, point_id)} pair, replacing the
     * N+1 loop that previously called {@code selectLatestPointValue} once per point.
     */
    List<PointValueDO> selectLatestPointValues(@Param("tenantId") Long tenantId,
                                               @Param("deviceId") Long deviceId,
                                               @Param("pointIds") List<Long> pointIds);

}
