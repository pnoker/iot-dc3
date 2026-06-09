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
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Top event source by alarm count.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.3
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Top event source ranked by alarm count")
public class AlertTopSourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "alert source: device or driver")
    private String source;

    @Schema(description = "source entity ID")
    private long sourceId;

    @Schema(description = "alarm count for this source")
    private long count;

}
