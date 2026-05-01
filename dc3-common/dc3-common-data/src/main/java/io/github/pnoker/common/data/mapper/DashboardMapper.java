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
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dashboard aggregation queries. Every statement runs against the {@code history}
 * data source (the Timescale hypertables live there) via {@link DS}.
 *
 * <p>All queries target {@code v_dc3_point_value_all}, the UNION ALL view
 * across the seven typed point-value hypertables, so chunk exclusion on
 * create_time and segment-by device_id still kicks in per branch.</p>
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Mapper
@DS("history")
public interface DashboardMapper {

    /**
     * Count of point-value rows in a time window, optionally scoped to a tenant.
     * Used by the today / yesterday totals on the dashboard header card.
     */
    @Select({
            "SELECT COUNT(*) FROM v_dc3_point_value_all",
            "WHERE create_time >= #{from} AND create_time < #{to}",
            "  AND (#{tenantId} IS NULL OR tenant_id = #{tenantId})"
    })
    long countInRange(@Param("tenantId") Long tenantId,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);

    /**
     * Time-bucketed count series for the trend chart. {@code bucket} is one of
     * {@code '1 hour'} or {@code '1 day'}. Returns rows like
     * {@code {bucket: 2026-05-02 10:00:00, count: 1234}} ordered ascending.
     */
    @Select({
            "SELECT time_bucket(#{bucket}::interval, create_time) AS bucket,",
            "       COUNT(*)                                      AS count",
            "  FROM v_dc3_point_value_all",
            " WHERE create_time >= #{from} AND create_time < #{to}",
            "   AND (#{tenantId} IS NULL OR tenant_id = #{tenantId})",
            " GROUP BY bucket",
            " ORDER BY bucket ASC"
    })
    List<Map<String, Object>> timeseries(@Param("tenantId") Long tenantId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         @Param("bucket") String bucket);

    /**
     * Top-N entities by point-value count over a time window. {@code dimension}
     * must be one of {@code device_id}, {@code point_id}, {@code driver_id} —
     * it's interpolated directly into the SQL so only accept validated values
     * from the service layer (never user input).
     */
    @Select({
            "SELECT ${dimension} AS entity_id,",
            "       COUNT(*)    AS count",
            "  FROM v_dc3_point_value_all",
            " WHERE create_time >= #{from} AND create_time < #{to}",
            "   AND (#{tenantId} IS NULL OR tenant_id = #{tenantId})",
            " GROUP BY ${dimension}",
            " ORDER BY count DESC",
            " LIMIT #{limit}"
    })
    List<Map<String, Object>> top(@Param("tenantId") Long tenantId,
                                  @Param("dimension") String dimension,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("limit") int limit);

    /**
     * Most-recent point-value rows for the live data feed. Unlike the other
     * queries this keeps raw_value / cal_value so the UI can render the value;
     * because value types vary we coerce every column to text. Tenant-scoped.
     */
    @Select({
            "SELECT * FROM (",
            "  SELECT tenant_id, device_id, point_id, driver_id, create_time,",
            "         raw_value::text AS raw_value,",
            "         cal_value::text AS cal_value,",
            "         'STRING'        AS value_type",
            "    FROM dc3_point_value",
            "   WHERE (#{tenantId} IS NULL OR tenant_id = #{tenantId})",
            "   ORDER BY create_time DESC LIMIT #{limit}",
            "  UNION ALL",
            "  SELECT tenant_id, device_id, point_id, driver_id, create_time,",
            "         raw_value::text, cal_value::text, 'INT'",
            "    FROM dc3_point_value_int",
            "   WHERE (#{tenantId} IS NULL OR tenant_id = #{tenantId})",
            "   ORDER BY create_time DESC LIMIT #{limit}",
            "  UNION ALL",
            "  SELECT tenant_id, device_id, point_id, driver_id, create_time,",
            "         raw_value::text, cal_value::text, 'LONG'",
            "    FROM dc3_point_value_long",
            "   WHERE (#{tenantId} IS NULL OR tenant_id = #{tenantId})",
            "   ORDER BY create_time DESC LIMIT #{limit}",
            "  UNION ALL",
            "  SELECT tenant_id, device_id, point_id, driver_id, create_time,",
            "         raw_value::text, cal_value::text, 'BOOL'",
            "    FROM dc3_point_value_bool",
            "   WHERE (#{tenantId} IS NULL OR tenant_id = #{tenantId})",
            "   ORDER BY create_time DESC LIMIT #{limit}",
            "  UNION ALL",
            "  SELECT tenant_id, device_id, point_id, driver_id, create_time,",
            "         raw_value::text, cal_value::text, 'FLOAT'",
            "    FROM dc3_point_value_float",
            "   WHERE (#{tenantId} IS NULL OR tenant_id = #{tenantId})",
            "   ORDER BY create_time DESC LIMIT #{limit}",
            "  UNION ALL",
            "  SELECT tenant_id, device_id, point_id, driver_id, create_time,",
            "         raw_value::text, cal_value::text, 'DOUBLE'",
            "    FROM dc3_point_value_double",
            "   WHERE (#{tenantId} IS NULL OR tenant_id = #{tenantId})",
            "   ORDER BY create_time DESC LIMIT #{limit}",
            "  UNION ALL",
            "  SELECT tenant_id, device_id, point_id, driver_id, create_time,",
            "         raw_value::text, cal_value::text, 'JSON'",
            "    FROM dc3_point_value_json",
            "   WHERE (#{tenantId} IS NULL OR tenant_id = #{tenantId})",
            "   ORDER BY create_time DESC LIMIT #{limit}",
            ") unioned",
            "ORDER BY create_time DESC",
            "LIMIT #{limit}"
    })
    List<Map<String, Object>> latestStream(@Param("tenantId") Long tenantId,
                                           @Param("limit") int limit);
}
