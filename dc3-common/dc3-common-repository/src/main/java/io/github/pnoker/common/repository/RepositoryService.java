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

package io.github.pnoker.common.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.bo.WindowAggregateResult;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.entity.query.WindowAggregateQuery;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data storage strategy service interface for point value persistence.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface RepositoryService {

    /**
     * Get repository service name
     *
     * @return Repository Name
     */
    String getRepositoryName();

    /**
     * Persist a single point value to the time-series store.
     *
     * @param entityBO point value to persist
     * @throws IOException on write failure
     */
    void savePointValue(PointValueBO entityBO) throws IOException;

    /**
     * Persist a batch of point values to the time-series store.
     *
     * @param entityBOList point values to persist
     * @throws IOException on write failure
     */
    void savePointValues(List<PointValueBO> entityBOList) throws IOException;

    /**
     * Get historical point values within the tenant scope.
     *
     * @param tenantId tenant ID (required for tenant isolation)
     * @param deviceId device ID
     * @param pointId  point ID
     * @param count    maximum number of records to retrieve
     * @return list of serialized point value strings, newest first
     */
    List<String> listHistoryPointValue(Long tenantId, Long deviceId, Long pointId, int count);

    /**
     * Query the latest point value within the tenant scope.
     *
     * @param tenantId tenant ID (required for tenant isolation)
     * @param deviceId device ID
     * @param pointId  point ID
     * @return the most recent point value, or {@code null} if none found
     */
    PointValueBO selectLatestPointValue(Long tenantId, Long deviceId, Long pointId);

    /**
     * Query the latest point values for multiple points within the tenant scope.
     *
     * @param tenantId tenant ID (required for tenant isolation)
     * @param deviceId device ID
     * @param pointIds point ID list
     * @return list of most recent point values per point, may contain fewer entries than requested
     */
    List<PointValueBO> listLatestPointValues(Long tenantId, Long deviceId, List<Long> pointIds);

    /**
     * Paginated query of point values.
     *
     * @param entityQuery query criteria including tenant, device, point, and time range
     * @return paginated results of matching point values
     */
    Page<PointValueBO> listPagePointValue(PointValueQuery entityQuery);

    /**
     * Compute a single SQL aggregate (AVG/MIN/MAX/SUM/COUNT) over the rows of
     * {@code dc3_point_value} that fall inside {@code [from, to)} for the
     * given tenant + device + point. Used by the long-window alarm evaluator
     * when the rule's window exceeds the local-buffer cutoff.
     *
     * @param request aggregation parameters
     * @return aggregate value and sample count
     */
    WindowAggregateResult aggregateInWindow(WindowAggregateQuery request);

    /**
     * Pull the raw samples in {@code [from, to)} for the given tenant +
     * device + point, ordered oldest → newest. Used by ALL/ANY long-window
     * evaluation, where the rule condition has to be applied per-sample
     * (which cannot be pushed into a single SQL aggregate).
     *
     * <p>Callers should bound the window — pulling unbounded samples
     * defeats the whole point of the time-series store.
     *
     * @param tenantId tenant id
     * @param deviceId device id
     * @param pointId  point id
     * @param from     inclusive lower bound on create_time
     * @param to       exclusive upper bound on create_time
     * @return ordered samples
     */
    List<PointValueBO> samplesInWindow(Long tenantId, Long deviceId, Long pointId,
                                       LocalDateTime from, LocalDateTime to);

}
