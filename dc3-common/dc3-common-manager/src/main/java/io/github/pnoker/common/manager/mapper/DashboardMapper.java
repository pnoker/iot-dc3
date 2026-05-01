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

package io.github.pnoker.common.manager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * Manager-side aggregate queries backing the home-page breakdowns
 * (drivers by protocol / enable status, devices by enable status /
 * profile distribution).
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Mapper
public interface DashboardMapper {

    /**
     * Driver counts grouped by enable_flag (0 = enabled, 1 = disabled).
     */
    @Select({
            "SELECT enable_flag AS enable_flag, COUNT(*) AS count",
            "  FROM dc3_driver",
            " WHERE deleted = 0 AND tenant_id = #{tenantId}",
            " GROUP BY enable_flag"
    })
    List<Map<String, Object>> countDriverByEnable(@Param("tenantId") Long tenantId);

    /**
     * Driver counts grouped by driver_type_flag (0=client, 1=server, 2=gateway, 3=connect).
     */
    @Select({
            "SELECT driver_type_flag AS driver_type_flag, COUNT(*) AS count",
            "  FROM dc3_driver",
            " WHERE deleted = 0 AND tenant_id = #{tenantId}",
            " GROUP BY driver_type_flag"
    })
    List<Map<String, Object>> countDriverByType(@Param("tenantId") Long tenantId);

    /**
     * Device counts grouped by enable_flag.
     */
    @Select({
            "SELECT enable_flag AS enable_flag, COUNT(*) AS count",
            "  FROM dc3_device",
            " WHERE deleted = 0 AND tenant_id = #{tenantId}",
            " GROUP BY enable_flag"
    })
    List<Map<String, Object>> countDeviceByEnable(@Param("tenantId") Long tenantId);

    /**
     * Device counts grouped by driver_id (top-N binding) — drives the
     * "devices per driver" breakdown on the dashboard.
     */
    @Select({
            "SELECT driver_id AS driver_id, COUNT(*) AS count",
            "  FROM dc3_device",
            " WHERE deleted = 0 AND tenant_id = #{tenantId}",
            " GROUP BY driver_id",
            " ORDER BY count DESC",
            " LIMIT #{limit}"
    })
    List<Map<String, Object>> countDeviceByDriver(@Param("tenantId") Long tenantId,
                                                  @Param("limit") int limit);

    /**
     * Device counts grouped by profile (via dc3_profile_bind). Uses only
     * active bindings. Top-N profile-bind counts — one device can bind to
     * multiple profiles, so totals can exceed device count.
     */
    @Select({
            "SELECT b.profile_id AS profile_id, COUNT(*) AS count",
            "  FROM dc3_profile_bind b",
            " WHERE b.deleted = 0 AND b.tenant_id = #{tenantId}",
            " GROUP BY b.profile_id",
            " ORDER BY count DESC",
            " LIMIT #{limit}"
    })
    List<Map<String, Object>> countDeviceByProfile(@Param("tenantId") Long tenantId,
                                                   @Param("limit") int limit);
}
