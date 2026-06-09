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
 * One edge in the Sankey. In cardinality mode {@code value} is the number of
 * relationships this edge represents (1 per device for Driver→Device, 1 per profile_bind
 * row for Device→Profile, 1 per point for Profile→Point; aggregated into N for
 * {@code *→others} edges). In volume mode (Phase 2) it carries the point_value sample
 * count rolled up along the edge.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
@Schema(description = "One edge in the topology Sankey graph")
public class TopologyLinkVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Prefixed id of the source node", example = "driver:1024")
    private String source;

    @Schema(description = "Prefixed id of the target node", example = "device:2048")
    private String target;

    @Schema(description = "Edge weight: relationship count in cardinality mode, or rolled-up sample count in volume mode", example = "5")
    private long value;

}
