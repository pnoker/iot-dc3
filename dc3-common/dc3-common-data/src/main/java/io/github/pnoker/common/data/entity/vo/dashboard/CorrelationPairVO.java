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
 * Two events that fired within a small time window of each other, enough times to suggest
 * a cascading-failure relationship. Frontend renders these as a network graph (A—B edge
 * weighted by coCount).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Two events that frequently co-occur, suggesting a cascading-failure relationship")
public class CorrelationPairVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "source of event A: device or driver")
    private String aSource;

    @Schema(description = "source entity ID of event A")
    private long aSourceId;

    @Schema(description = "event type of event A")
    private int aEventType;

    @Schema(description = "source of event B: device or driver")
    private String bSource;

    @Schema(description = "source entity ID of event B")
    private long bSourceId;

    @Schema(description = "event type of event B")
    private int bEventType;

    @Schema(description = "number of times A and B co-occurred")
    private long coCount;

}
