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
import java.util.Collection;
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

    // ---- Topology (GET /dashboard/topology) ----------------------------
    //
    // The five helpers below let the service layer compose the four-column
    // Sankey graph (Driver → Device → Profile → Point). Each method returns
    // plain Maps so the service can do Top-N / Others aggregation in Java
    // without widening the VO surface. Empty-collection callers short-
    // circuit in Java (MyBatis errors on empty IN clauses).

    /**
     * Every driver in the tenant with its device count. Used to rank
     * drivers for the "Top-N drivers" crop on the topology Sankey.
     * Row shape: {@code {id, driver_name, device_count}}.
     */
    List<Map<String, Object>> topologyDrivers(@Param("tenantId") Long tenantId);

    /**
     * Every device belonging to any driver in {@code driverIds}, with its
     * profile-bind count (used to rank devices for the "Top-N devices" crop).
     * Row shape: {@code {id, device_name, driver_id, profile_count}}.
     */
    List<Map<String, Object>> topologyDevicesByDrivers(@Param("tenantId") Long tenantId,
                                                       @Param("driverIds") Collection<Long> driverIds);

    /**
     * All profile-bind rows for the given device set. The Device→Profile
     * layer of the Sankey is drawn one link per row returned here.
     * Row shape: {@code {profile_id, device_id}}.
     */
    List<Map<String, Object>> topologyProfileBindings(@Param("tenantId") Long tenantId,
                                                      @Param("deviceIds") Collection<Long> deviceIds);

    /**
     * Profile names for the given profile id set. Row shape:
     * {@code {id, profile_name}}. Order is unspecified — the service layer
     * preserves the Sankey's layer-3 order by zipping with the bindings.
     */
    List<Map<String, Object>> topologyProfilesByIds(@Param("tenantId") Long tenantId,
                                                    @Param("profileIds") Collection<Long> profileIds);

    /**
     * All points belonging to any profile in {@code profileIds}. Service
     * groups by {@code profile_id} and keeps Top-N per profile; the rest
     * collapse into a per-profile {@code others:point:{profileId}} node.
     * Row shape: {@code {id, point_name, profile_id}}.
     */
    List<Map<String, Object>> topologyPointsByProfiles(@Param("tenantId") Long tenantId,
                                                       @Param("profileIds") Collection<Long> profileIds);

    /**
     * Volume rollup for the topology's "volume" mode — counts point_value
     * rows per {@code (device_id, point_id)} over the selected time window.
     * The query targets {@code dc3_history.v_dc3_point_value_all} (a
     * UNION-ALL view across the 7 typed hypertables); we cross-schema
     * qualify so it runs against manager's existing master DS without
     * needing a second dynamic datasource wired into the manager service.
     *
     * <p>Returned row set is bounded by unique active {@code (device,
     * point)} pairs (hundreds to a few thousand even for a busy tenant),
     * which the service joins back against the pre-fetched metadata to
     * roll values up through profile and device to driver.</p>
     *
     * <p>Row shape: {@code {device_id, point_id, cnt}}.</p>
     */
    List<Map<String, Object>> topologyPointVolumes(@Param("tenantId") Long tenantId,
                                                   @Param("from") LocalDateTime from);
}
