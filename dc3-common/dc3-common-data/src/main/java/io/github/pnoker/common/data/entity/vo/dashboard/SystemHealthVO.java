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

package io.github.pnoker.common.data.entity.vo.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregate system health for the dashboard banner.
 *
 * <p>
 * {@code center} / {@code infra} values are {@code "up"} or {@code "down"}. Kept as Maps
 * (not typed fields) so the frontend can render whatever keys the backend decides to
 * expose without a coordinated deploy.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Aggregate system health for the dashboard banner")
public class SystemHealthVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Center service reachability (auth, data, manager).
     */
    @Schema(description = "center service reachability (auth, data, manager), values are up or down")
    private Map<String, String> center;

    /**
     * Infrastructure reachability (database, mq, gateway).
     */
    @Schema(description = "infrastructure reachability (database, mq, gateway), values are up or down")
    private Map<String, String> infra;

    /**
     * Driver population summary.
     */
    @Schema(description = "driver population summary")
    private FleetSummary drivers;

    /**
     * Device population summary.
     */
    @Schema(description = "device population summary")
    private FleetSummary devices;

    /**
     * Population summary shared by drivers and devices: count of records in the DB, how
     * many of them reported ONLINE to the status cache, and how many rows resolved to a
     * cache miss (never heartbeated since startup).
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "Population summary: total records and how many reported online")
    public static class FleetSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "total record count in the database")
        private int total;

        @Schema(description = "count of records that reported ONLINE to the status cache")
        private int online;

    }

    /**
     * Backwards-compat alias — drivers used to be typed as DriverSummary.
     */
    @Schema(description = "Backwards-compat alias for FleetSummary")
    public static class DriverSummary extends FleetSummary {

    }

}
