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

import io.github.pnoker.common.manager.entity.bo.dashboard.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Manager-side aggregate queries backing the home-page breakdowns (drivers by protocol /
 * enable status, devices by enable status / profile distribution) and the topology
 * Sankey.
 *
 * <p>
 * All methods return typed Row DTOs from {@code entity/bo/dashboard/};
 * {@code map-underscore-to-camel-case: true} in application.yml handles the snake→camel
 * column mapping automatically. SQL lives in
 * {@code resources/mapping/DashboardMapper.xml}.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Mapper
public interface DashboardMapper {

    /**
     * Driver counts grouped by {@code enable_flag} (aliased to {@code key}).
     */
    List<BucketRow> countDriverByEnable(@Param("tenantId") Long tenantId);

    /**
     * Driver counts grouped by {@code driver_type_flag} (aliased to {@code key}).
     */
    List<BucketRow> countDriverByType(@Param("tenantId") Long tenantId);

    /**
     * Driver counts grouped by {@code service_name} (aliased to {@code key}).
     */
    List<BucketRow> countDriverByService(@Param("tenantId") Long tenantId);

    /**
     * Device counts grouped by {@code enable_flag} (aliased to {@code key}).
     */
    List<BucketRow> countDeviceByEnable(@Param("tenantId") Long tenantId);

    /**
     * Device counts grouped by {@code driver_id} (aliased to {@code key}). Top-N.
     */
    List<BucketRow> countDeviceByDriver(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    /**
     * Device counts grouped by {@code profile_id} (aliased to {@code key}). Top-N.
     */
    List<BucketRow> countDeviceByProfile(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    /**
     * Daily new-row count for the given entity table. {@code table} is interpolated
     * (service layer whitelists it). Missing days are padded with zero service-side so
     * the frontend always gets a fixed-length array.
     */
    List<DailyGrowthRow> dailyGrowth(@Param("tenantId") Long tenantId, @Param("table") String table,
                                     @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // ---- Topology (GET /dashboard/topology) ----------------------------

    List<TopologyDriverRow> topologyDrivers(@Param("tenantId") Long tenantId);

    List<TopologyDeviceRow> topologyDevicesByDrivers(@Param("tenantId") Long tenantId,
                                                     @Param("driverIds") Collection<Long> driverIds);

    List<ProfileBindingRow> topologyProfileBindings(@Param("tenantId") Long tenantId,
                                                    @Param("deviceIds") Collection<Long> deviceIds);

    List<TopologyProfileRow> topologyProfilesByIds(@Param("tenantId") Long tenantId,
                                                   @Param("profileIds") Collection<Long> profileIds);

    List<TopologyPointRow> topologyPointsByProfiles(@Param("tenantId") Long tenantId,
                                                    @Param("profileIds") Collection<Long> profileIds);

    /**
     * Volume rollup for the topology's "volume" mode — counts point_value rows per
     * {@code (device_id, point_id)} over the selected time window, queried against
     * {@code dc3_history.v_dc3_point_value_all} (cross-schema).
     */
    List<PointVolumeRow> topologyPointVolumes(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

}
