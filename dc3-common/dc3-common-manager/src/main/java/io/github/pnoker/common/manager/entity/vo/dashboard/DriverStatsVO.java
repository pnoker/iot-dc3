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

package io.github.pnoker.common.manager.entity.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Rollup payload returned by GET /manager/dashboard/driver/stats.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@Schema(description = "Driver statistics rollup returned by the driver dashboard endpoint")
public class DriverStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Total number of drivers for the tenant", example = "16")
    private long total;

    /**
     * Counts by enable flag. {@link BucketVO#getKey()} is "ENABLE" or "DISABLE".
     */
    @Schema(description = "Driver counts grouped by enable flag (bucket key is ENABLE or DISABLE)")
    private List<BucketVO> byEnable = new ArrayList<>();

    /**
     * Counts by driver_type_flag. Key is the enum name (DRIVER_CLIENT, DRIVER_SERVER,
     * GATEWAY, CONNECT).
     */
    @Schema(description = "Driver counts grouped by driver type flag (bucket key is the driver type enum name)")
    private List<BucketVO> byType = new ArrayList<>();

    /**
     * Counts by service_name — the real "protocol breakdown" the home dashboard exposes.
     * Key is the raw service name (e.g. {@code dc3-driver-modbus-tcp},
     * {@code dc3-driver-mqtt}); the frontend strips the {@code dc3-driver-} prefix before
     * rendering.
     */
    @Schema(description = "Driver counts grouped by service name (bucket key is the raw service name, e.g. dc3-driver-modbus-tcp)")
    private List<BucketVO> byService = new ArrayList<>();

}
