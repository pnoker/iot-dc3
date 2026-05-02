/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.data.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * Alert aggregation queries over {@code dc3_device_event} and
 * {@code dc3_driver_event}. Both tables live in the master data source (not
 * Timescale hypertables) and carry a {@code tenant_id} column; all queries
 * are tenant-scoped.
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Mapper
public interface AlertMapper {

    /**
     * Overall counters for the alert stat card, scoped to one tenant.
     */
    @Select({
            "SELECT",
            "  (SELECT COUNT(*) FROM dc3_device_event",
            "     WHERE deleted = 0 AND tenant_id = #{tenantId}) +",
            "  (SELECT COUNT(*) FROM dc3_driver_event",
            "     WHERE deleted = 0 AND tenant_id = #{tenantId}) AS total,",
            "  (SELECT COUNT(*) FROM dc3_device_event",
            "     WHERE deleted = 0 AND tenant_id = #{tenantId} AND confirm_flag = 0) +",
            "  (SELECT COUNT(*) FROM dc3_driver_event",
            "     WHERE deleted = 0 AND tenant_id = #{tenantId} AND confirm_flag = 0) AS unconfirmed"
    })
    Map<String, Object> countAll(@Param("tenantId") Long tenantId);

    /**
     * Per-type breakdown across both event tables. event_type_flag is a
     * SMALLINT; we group on it and return (key, count) rows.
     */
    @Select({
            "SELECT event_type_flag::text AS key, COUNT(*) AS count",
            "  FROM (",
            "    SELECT event_type_flag FROM dc3_device_event",
            "     WHERE deleted = 0 AND tenant_id = #{tenantId}",
            "    UNION ALL",
            "    SELECT event_type_flag FROM dc3_driver_event",
            "     WHERE deleted = 0 AND tenant_id = #{tenantId}",
            "  ) merged",
            " GROUP BY event_type_flag",
            " ORDER BY count DESC"
    })
    List<Map<String, Object>> countByType(@Param("tenantId") Long tenantId);

    /**
     * Most recent N events across device + driver tables, flagged with source.
     */
    @Select({
            "SELECT * FROM (",
            "  (SELECT id, 'device' AS source, device_id AS source_id, point_id,",
            "          event_type_flag, confirm_flag, create_time",
            "     FROM dc3_device_event",
            "    WHERE deleted = 0 AND tenant_id = #{tenantId}",
            "    ORDER BY create_time DESC LIMIT #{limit})",
            "  UNION ALL",
            "  (SELECT id, 'driver' AS source, driver_id AS source_id, 0,",
            "          event_type_flag, confirm_flag, create_time",
            "     FROM dc3_driver_event",
            "    WHERE deleted = 0 AND tenant_id = #{tenantId}",
            "    ORDER BY create_time DESC LIMIT #{limit})",
            ") unioned",
            "ORDER BY create_time DESC",
            "LIMIT #{limit}"
    })
    List<Map<String, Object>> latest(@Param("tenantId") Long tenantId, @Param("limit") int limit);
}
