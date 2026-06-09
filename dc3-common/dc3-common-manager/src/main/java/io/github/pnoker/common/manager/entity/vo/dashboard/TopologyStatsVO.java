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

/**
 * Total counts across the tenant (not just the Top-N rendered in the Sankey). Frontend
 * uses this for the footer summary bar — "X Driver · Y Device · Z Profile · W Point" — so
 * the user can see how much the diagram has been cropped.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
@Schema(description = "Tenant-wide total counts for the topology footer summary")
public class TopologyStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Total number of drivers for the tenant", example = "16")
    private long driverCount;

    @Schema(description = "Total number of devices for the tenant", example = "128")
    private long deviceCount;

    @Schema(description = "Total number of profiles for the tenant", example = "32")
    private long profileCount;

    @Schema(description = "Total number of points for the tenant", example = "512")
    private long pointCount;

    /**
     * Short human-readable range label ("24h" / "7d" / ...) — present only when the
     * server computed volumes over a time window (volume mode). null for cardinality mode
     * so the frontend footer can skip it.
     */
    @Schema(description = "Short human-readable range label (e.g. 24h, 7d); present only in volume mode, null in cardinality mode", example = "24h")
    private String rangeLabel;

}
