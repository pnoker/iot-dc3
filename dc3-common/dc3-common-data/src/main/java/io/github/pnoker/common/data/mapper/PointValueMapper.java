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

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Relational access to the {@code dc3_point_latest} projection — the
 * transactional latest-value state that deliberately stays out of the TSDB
 * port (docs/design/tsdb-abstraction.md §9.1). Point-value history lives
 * behind the {@code TsdbStore} port; nothing here touches the hypertable.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@DS("history")
public interface PointValueMapper {

    /**
     * Upsert the shared latest-value projection without allowing an older reading to
     * replace a newer one.
     */
    @InterceptorIgnore(tenantLine = "true")
    int upsertLatestBatch(@Param("values") List<PointValueDO> values);

    /**
     * Batch query the latest point value for each point within a single device.
     * Reads from the transactional latest-value projection.
     */
    List<PointValueDO> selectLatestPointValues(@Param("tenantId") Long tenantId,
                                               @Param("deviceId") Long deviceId,
                                               @Param("pointIds") List<Long> pointIds);

    /**
     * Newest slice of the tenant's latest values (one row per series) for the
     * dashboard live stream.
     */
    List<PointValueDO> selectLatestStream(@Param("tenantId") Long tenantId, @Param("limit") int limit);
}
