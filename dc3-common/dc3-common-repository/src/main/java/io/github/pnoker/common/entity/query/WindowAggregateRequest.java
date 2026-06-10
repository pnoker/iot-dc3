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

package io.github.pnoker.common.entity.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Window-aggregation pushdown request used by the long-window evaluator.
 *
 * <p>The {@code function} string is one of {@code AVG / MIN / MAX / SUM / COUNT}
 * — the repository implementation maps it to the matching SQL aggregate
 * function. Tenant + device + point + create_time bracket the rows. {@code from}
 * is inclusive, {@code to} is exclusive.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Schema(description = "Window aggregation request")
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class WindowAggregateRequest {

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "Device ID")
    private Long deviceId;

    @Schema(description = "Point ID")
    private Long pointId;

    @Schema(description = "Aggregate function: AVG, MIN, MAX, SUM, or COUNT", example = "AVG")
    private String function;

    @Schema(description = "Inclusive start time")
    private LocalDateTime from;

    @Schema(description = "Exclusive end time")
    private LocalDateTime to;

}
