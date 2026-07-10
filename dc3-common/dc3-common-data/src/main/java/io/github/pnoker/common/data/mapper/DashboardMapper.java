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
import io.github.pnoker.common.data.entity.bo.dashboard.ActivityCellRow;
import io.github.pnoker.common.data.entity.bo.dashboard.CoverageGapRow;
import io.github.pnoker.common.data.entity.bo.dashboard.EntityCountRow;
import io.github.pnoker.common.data.entity.bo.dashboard.LatencyBinRow;
import io.github.pnoker.common.data.entity.bo.dashboard.LatestPointValueRow;
import io.github.pnoker.common.data.entity.bo.dashboard.SilentSourceRow;
import io.github.pnoker.common.data.entity.bo.dashboard.TimeBucketRow;
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
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Mapper
@DS("history")
public interface DashboardMapper {

    /**
     * Count point-value rows in the half-open {@code [from, to)} range.
     *
     * @param tenantId tenant scope
     * @param from     range start (inclusive)
     * @param to       range end (exclusive)
     * @return row count in the range
     */
    long countInRange(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);

    /**
     * Total point-value row count for a tenant.
     *
     * @param tenantId tenant scope
     * @return total row count
     */
    long countTotal(@Param("tenantId") Long tenantId);

    /**
     * Point-value counts bucketed by time, using a PostgreSQL {@code date_trunc}-style
     * bucket (e.g. {@code hour}, {@code day}).
     *
     * @param tenantId tenant scope
     * @param from     range start
     * @param to       range end
     * @param bucket   date_trunc bucket granularity
     * @return per-bucket count rows
     */
    List<TimeBucketRow> timeseries(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to, @Param("bucket") String bucket);

    /**
     * Top-N entities by point-value volume, ranked by a dimension (device/point/driver).
     *
     * @param tenantId  tenant scope
     * @param dimension ranking dimension
     * @param from      range start
     * @param to        range end
     * @param limit     maximum number of entities
     * @return top entities by volume
     */
    List<EntityCountRow> top(@Param("tenantId") Long tenantId, @Param("dimension") String dimension,
                             @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("limit") int limit);

    /**
     * The most recent point-value rows, newest first.
     *
     * @param tenantId tenant scope
     * @param limit    maximum number of rows
     * @return latest point-value rows
     */
    List<LatestPointValueRow> latestStream(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    /**
     * Point-value ingestion latency histogram over the range.
     *
     * @param tenantId tenant scope
     * @param from     range start
     * @param to       range end
     * @return latency bin rows
     */
    List<LatencyBinRow> latencyHistogram(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /**
     * Point-value activity on an hour-of-day grid over the range.
     *
     * @param tenantId tenant scope
     * @param from     range start
     * @param to       range end
     * @return activity cells keyed by hour
     */
    List<ActivityCellRow> hourlyActivity(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    // ===== Phase-2 insights ====================================================

    /**
     * Silent sources: points whose latest value predates the threshold instant, i.e.
     * have stopped reporting since then.
     *
     * @param tenantId        tenant scope
     * @param from            lookback start
     * @param silentThreshold a point is silent when its last value is before this instant
     * @param limit           maximum number of sources
     * @return silent source rows
     */
    List<SilentSourceRow> silentSources(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                        @Param("silentThreshold") LocalDateTime silentThreshold, @Param("limit") int limit);

    /**
     * Coverage gap items: points/devices expected to report but missing from recent data.
     *
     * @param tenantId tenant scope
     * @param limit    maximum number of items
     * @return coverage gap rows
     */
    List<CoverageGapRow> coverageGapItems(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    /**
     * Total point count for a tenant (for coverage-ratio denominators).
     *
     * @param tenantId tenant scope
     * @return total point count
     */
    long countPointsInTenant(@Param("tenantId") Long tenantId);

}
