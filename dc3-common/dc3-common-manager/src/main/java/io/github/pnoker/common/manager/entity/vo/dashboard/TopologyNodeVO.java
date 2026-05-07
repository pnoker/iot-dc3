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
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
public class TopologyNodeVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Prefixed id — one of {@code driver:{n}}, {@code device:{n}}, {@code profile:{n}},
	 * {@code point:{n}}, {@code others:{layer}:{parentId}}.
	 */
	private String id;

	/**
	 * Human-readable label. For {@code others:*} nodes, {@code "Others (N)"} where N is
	 * the count of hidden children.
	 */
	private String name;

	/**
	 * 1 = driver, 2 = device, 3 = profile, 4 = point. Frontend fixes column x-position
	 * from this.
	 */
	private int layer;

	/**
	 * {@code driver | device | profile | point | others}. Drives node colour + click
	 * routing.
	 */
	private String type;

	/**
	 * Only populated on {@code others:*} nodes. List of the actual entities collapsed
	 * here — frontend shows these in a drill-in dialog.
	 */
	private List<TopologyHiddenChildVO> hiddenChildren;

}
