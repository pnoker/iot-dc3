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
 * Rollup payload returned by GET /manager/dashboard/device/stats.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@Schema(description = "Device statistics rollup returned by the device dashboard endpoint")
public class DeviceStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Total number of devices for the tenant", example = "128")
    private long total;

    /**
     * Counts by enable flag.
     */
    @Schema(description = "Device counts grouped by enable flag")
    private List<BucketVO> byEnable = new ArrayList<>();

    /**
     * Top-N devices grouped by driver_id — key is stringified driver id.
     */
    @Schema(description = "Top-N device counts grouped by driver id (bucket key is the stringified driver id)")
    private List<BucketVO> byDriver = new ArrayList<>();

    /**
     * Top-N profile bindings — key is stringified profile id. Note that one device may
     * bind to multiple profiles, so these counts are bindings, not unique devices.
     */
    @Schema(description = "Top-N profile binding counts grouped by profile id (bucket key is the stringified profile id); counts are bindings, not unique devices")
    private List<BucketVO> byProfile = new ArrayList<>();

}
