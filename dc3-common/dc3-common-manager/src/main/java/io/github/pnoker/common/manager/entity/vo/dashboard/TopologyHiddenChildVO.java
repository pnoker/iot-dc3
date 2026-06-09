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
 * One entity that got collapsed into an {@code others:*} bucket. Has enough to route on
 * click in the drill-in dialog — id prefix + display name.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
@Schema(description = "An entity collapsed into an others bucket, retaining enough info to route on click")
public class TopologyHiddenChildVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Prefixed id, same scheme as {@link TopologyNodeVO#getId()} — {@code driver:{n}} /
     * {@code device:{n}} / {@code point:{n}}.
     */
    @Schema(description = "Prefixed id (driver:{n}, device:{n} or point:{n})", example = "device:1024")
    private String id;

    @Schema(description = "Display name of the collapsed entity")
    private String name;

    /**
     * {@code driver | device | point}. Profile layer does not get collapsed.
     */
    @Schema(description = "Entity type: driver, device or point (the profile layer is never collapsed)", example = "device")
    private String type;

}
