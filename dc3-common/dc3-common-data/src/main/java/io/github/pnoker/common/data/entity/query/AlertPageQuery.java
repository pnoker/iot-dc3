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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request body for {@code POST /dashboard/alert/page}. Replaces the earlier
 * Map&lt;String, Object&gt; shape so fields are typed and the controller doesn't have to
 * handwrite {@code Integer.parseInt(b.get("..").toString())} for every entry.
 *
 * @author pnoker
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Alert Page query parameters")
public class AlertPageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * {@code "device"} / {@code "driver"} / {@code null} (both).
     */
    @Schema(description = "Source identifier", example = "device")
    private String source;

    @Schema(description = "Alarm type enum", example = "1")
    private Integer alarmTypeFlag;

    /**
     * 0 = unconfirmed, 1 = confirmed, null = both.
     */
    @Schema(description = "Confirm flag enum", example = "0")
    private Integer confirmFlag;

    /**
     * Preset time-range key — resolved server-side via TimeRangeUtil.
     */
    @Schema(description = "Preset time range key: today, 24h, 7d, or 30d", example = "24h")
    private String rangeKey;

    @Schema(description = "Zero-based row offset", example = "0")
    private long offset;

    @Schema(description = "Maximum rows to return", example = "50")
    private int limit = PageRequest.DEFAULT_LIMIT;

    @Schema(description = "Whitelisted sort fields")
    private java.util.List<SortSpec> sort = java.util.List.of();

}
