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
import io.github.pnoker.common.data.entity.bo.dashboard.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard aggregation queries. Every statement runs against the {@code history} data
 * source (the Timescale hypertables live there) via {@link DS}. Returns typed Row DTOs —
 * see {@code entity/bo/dashboard/}.
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Mapper
@DS("history")
public interface DashboardMapper {

    long countInRange(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);

    long countTotal(@Param("tenantId") Long tenantId);

    List<TimeBucketRow> timeseries(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to, @Param("bucket") String bucket);

    List<EntityCountRow> top(@Param("tenantId") Long tenantId, @Param("dimension") String dimension,
                             @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("limit") int limit);

    List<LatestPointValueRow> latestStream(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    List<LatencyBinRow> latencyHistogram(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    List<ActivityCellRow> hourlyActivity(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    // ===== Phase-2 insights ====================================================

    List<SilentSourceRow> silentSources(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                        @Param("silentThreshold") LocalDateTime silentThreshold, @Param("limit") int limit);

    List<CoverageGapRow> coverageGapItems(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    long countPointsInTenant(@Param("tenantId") Long tenantId);

}
