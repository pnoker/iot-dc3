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
 * Payload for GET /manager/dashboard/topology — a 4-column Sankey graph (Driver → Device
 * → Profile → Point) the home page uses to show how the tenant's metadata and data flow
 * wire together.
 *
 * <p>
 * Top-N cropping happens server-side so large tenants don't blow up the payload. Whatever
 * was cropped rolls up into {@code others:*} nodes whose
 * {@link TopologyNodeVO#getHiddenChildren()} holds the real list for a drill-in dialog.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
@Schema(description = "Topology payload: a 4-column Sankey graph (Driver to Device to Profile to Point)")
public class TopologyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Sankey nodes across the driver, device, profile and point columns")
    private List<TopologyNodeVO> nodes = new ArrayList<>();

    @Schema(description = "Sankey edges connecting the nodes")
    private List<TopologyLinkVO> links = new ArrayList<>();

    @Schema(description = "Tenant-wide total counts used for the footer summary")
    private TopologyStatsVO stats = new TopologyStatsVO();

}
