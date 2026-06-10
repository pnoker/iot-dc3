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

package io.github.pnoker.common.data.entity.query;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for {@code POST /dashboard/alert/page}. Replaces the earlier
 * Map&lt;String, Object&gt; shape so fields are typed and the controller doesn't have to
 * handwrite {@code Integer.parseInt(b.get("..").toString())} for every entry.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
@Schema(description = "Alert Page query parameters")
public class AlertPageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * {@code "device"} / {@code "driver"} / {@code null} (both).
     */
    @Schema(description = "Source identifier")
    private String source;

    @Schema(description = "Alarm type enum")

    private Integer alarmTypeFlag;

    /**
     * 0 = unconfirmed, 1 = confirmed, null = both.
     */
    @Schema(description = "Confirm flag enum")
    private Integer confirmFlag;

    /**
     * Legacy integer window; {@code rangeKey} wins when both set.
     */
    @Schema(description = "Fallback rolling time range in hours")
    private Integer rangeHours;

    /**
     * Preset time-range key — resolved server-side via TimeRangeUtil.
     */
    @Schema(description = "Preset time range key: today, 24h, 7d, or 30d")
    private String rangeKey;

    /**
     * 1-based page index. Defaults to 1 if null or less.
     */
    @Schema(description = "Current page number")
    private Long current;

    @Schema(description = "Page size")

    private Long size;

}
