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
import java.util.List;

/**
 * One node in the topology Sankey. {@code id} is a prefixed string so the frontend can
 * route on type + key in one hop (e.g. {@code "driver:42"} → {@code driverDetail/42};
 * {@code "others:point:51"} → dialog for profile 51's cropped points).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
@Schema(description = "One node in the topology Sankey graph")
public class TopologyNodeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Prefixed id — one of {@code driver:{n}}, {@code device:{n}}, {@code profile:{n}},
     * {@code point:{n}}, {@code others:{layer}:{parentId}}.
     */
    @Schema(description = "Prefixed id (driver:{n}, device:{n}, profile:{n}, point:{n} or others:{layer}:{parentId})", example = "driver:1024")
    private String id;

    /**
     * Human-readable label. For {@code others:*} nodes, {@code "Others (N)"} where N is
     * the count of hidden children.
     */
    @Schema(description = "Human-readable label; for others:* nodes it is \"Others (N)\" where N is the hidden child count")
    private String name;

    /**
     * 1 = driver, 2 = device, 3 = profile, 4 = point. Frontend fixes column x-position
     * from this.
     */
    @Schema(description = "Sankey column layer: 1=driver, 2=device, 3=profile, 4=point", example = "1")
    private int layer;

    /**
     * {@code driver | device | profile | point | others}. Drives node colour + click
     * routing.
     */
    @Schema(description = "Node type: driver, device, profile, point or others", example = "driver")
    private String type;

    /**
     * Only populated on {@code others:*} nodes. List of the actual entities collapsed
     * here — frontend shows these in a drill-in dialog.
     */
    @Schema(description = "Entities collapsed into this node; populated only on others:* nodes")
    private List<TopologyHiddenChildVO> hiddenChildren;

}
