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
import io.github.pnoker.common.entity.query.WindowAggregateRequest;

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
     * Save PointValue
     *
     * @param entityBO PointValue
     * @throws IOException IOException
     */
    void savePointValue(PointValueBO entityBO) throws IOException;

    /**
     * Save PointValue list
     *
     * @param entityBOList PointValue Array
     * @throws IOException IOException
     */
    void savePointValues(List<PointValueBO> entityBOList) throws IOException;

    /**
     * Get historical PointValue within the tenant scope.
     *
     * @param tenantId Tenant ID (required for tenant isolation)
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @param count    Count
     * @return History Value Array
     */
    List<String> listHistoryPointValue(Long tenantId, Long deviceId, Long pointId, int count);

    /**
     * Query latest PointValue within the tenant scope.
     *
     * @param tenantId Tenant ID (required for tenant isolation)
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @return PointValueBO
     */
    PointValueBO selectLatestPointValue(Long tenantId, Long deviceId, Long pointId);

    /**
     * Query latest PointValue list within the tenant scope.
     *
     * @param tenantId Tenant ID (required for tenant isolation)
     * @param deviceId Device ID
     * @param pointIds Point ID list
     * @return PointValueBO Array
     */
    List<PointValueBO> listLatestPointValues(Long tenantId, Long deviceId, List<Long> pointIds);

    /**
     * Page query PointValue
     *
     * @param entityQuery Entry of Query
     * @return Entity of BO Page
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
    WindowAggregateResult aggregateInWindow(WindowAggregateRequest request);

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
