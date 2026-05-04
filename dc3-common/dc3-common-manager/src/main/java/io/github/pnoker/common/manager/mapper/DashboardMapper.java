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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Manager-side aggregate queries backing the home-page breakdowns
 * (drivers by protocol / enable status, devices by enable status /
 * profile distribution).
 *
 * <p>SQL lives in {@code resources/mapping/DashboardMapper.xml}.</p>
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Mapper
public interface DashboardMapper {

    /**
     * Driver counts grouped by enable_flag (0 = enabled, 1 = disabled).
     */
    List<Map<String, Object>> countDriverByEnable(@Param("tenantId") Long tenantId);

    /**
     * Driver counts grouped by driver_type_flag (0=client, 1=server, 2=gateway, 3=connect).
     */
    List<Map<String, Object>> countDriverByType(@Param("tenantId") Long tenantId);

    /**
     * Driver counts grouped by service_name — the "which protocol drivers
     * are registered" breakdown. Returns rows like
     * {@code {service_name: "dc3-driver-modbus-tcp", count: 5}}.
     */
    List<Map<String, Object>> countDriverByService(@Param("tenantId") Long tenantId);

    /**
     * Device counts grouped by enable_flag.
     */
    List<Map<String, Object>> countDeviceByEnable(@Param("tenantId") Long tenantId);

    /**
     * Device counts grouped by driver_id (top-N binding) — drives the
     * "devices per driver" breakdown on the dashboard.
     */
    List<Map<String, Object>> countDeviceByDriver(@Param("tenantId") Long tenantId,
                                                  @Param("limit") int limit);

    /**
     * Device counts grouped by profile (via dc3_profile_bind). Uses only
     * active bindings. Top-N profile-bind counts — one device can bind to
     * multiple profiles, so totals can exceed device count.
     */
    List<Map<String, Object>> countDeviceByProfile(@Param("tenantId") Long tenantId,
                                                   @Param("limit") int limit);

    /**
     * Daily new-row count for the given entity table. {@code table} is
     * interpolated (must be one of dc3_driver / dc3_device / dc3_point —
     * service layer whitelists it). Returns rows like {@code {day: 2026-05-01,
     * count: 12}} ordered ascending.
     */
    List<Map<String, Object>> dailyGrowth(@Param("tenantId") Long tenantId,
                                          @Param("table") String table,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}
