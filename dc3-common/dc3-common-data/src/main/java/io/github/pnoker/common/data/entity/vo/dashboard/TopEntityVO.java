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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One entry in a dashboard top-N ranking (e.g. device with the most point-value rows in
 * the given time window).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One entry in a dashboard top-N ranking")
public class TopEntityVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Device / point / driver id — the service resolves the human-readable name
     * asynchronously on the frontend via existing getXxxByIds APIs.
     */
    @Schema(description = "ranked entity ID (device, point or driver)")
    private Long entityId;

    @Schema(description = "ranking count for this entity")
    private long count;

}
