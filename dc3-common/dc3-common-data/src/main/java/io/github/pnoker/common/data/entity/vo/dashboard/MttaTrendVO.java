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
 * One day's MTTA (mean time to acknowledge) statistic, computed as (operate_time -
 * create_time) on confirm_flag=1 events. Event table has an UPDATE trigger that stamps
 * operate_time on confirm, so the diff is the ack latency.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "One day's MTTA (mean time to acknowledge) statistic")
public class MttaTrendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ISO date.
     */
    @Schema(description = "ISO date of the data point")
    private String date;

    @Schema(description = "50th percentile acknowledge latency in milliseconds")
    private long p50Ms;

    @Schema(description = "95th percentile acknowledge latency in milliseconds")
    private long p95Ms;

    /**
     * Count of confirmed events used in the percentile calc for this day.
     */
    @Schema(description = "count of confirmed events used in the percentile calculation for this day")
    private long confirmedCount;

}
