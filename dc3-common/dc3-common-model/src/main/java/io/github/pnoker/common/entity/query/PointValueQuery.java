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

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Point value query parameters.
 *
 * <p>Latest snapshots use canonical zero-based offset pagination. Historical
 * point-value reads use an opaque cursor and never accept an offset.</p>
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Point value query parameters")
public class PointValueQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(
            description = "Zero-based result offset; supported by latest snapshots only (history uses cursor)",
            example = "0",
            minimum = "0")
    private Long offset;

    @Builder.Default
    @Schema(description = "Maximum number of records to return", example = "50", minimum = "1", maximum = "200")
    private Integer limit = PageRequest.DEFAULT_LIMIT;

    @Builder.Default
    @Schema(description = "Stable allow-listed sort fields")
    private List<SortSpec> sort = List.of();

    /**
     * Null-safe paging accessors: the runtime Jackson 3 mapper binds request bodies
     * through the all-args constructor, leaving absent fields null. Boxed fields keep
     * "unspecified" distinguishable from explicit (possibly invalid) values.
     */
    public long getOffset() {
        return offset == null ? 0L : offset;
    }

    /** Return the requested page limit. */
    public int getLimit() {
        return limit == null ? PageRequest.DEFAULT_LIMIT : limit;
    }

    /** Return the requested sort. */
    public List<SortSpec> getSort() {
        return sort == null ? List.of() : sort;
    }

    @Schema(description = "Opaque signed cursor returned by the previous history page")
    private String cursor;

    @Schema(description = "Tenant ID for multi-tenant isolation. Required for query scope.")
    private Long tenantId;

    @Schema(description = "Filter by device ID", example = "1024")
    private Long deviceId;

    @Schema(description = "Filter by device name", example = "Temperature Sensor 01")
    private String deviceName;

    @Schema(description = "Filter by data point ID", example = "2048")
    private Long pointId;

    @Schema(description = "Filter by data point name", example = "Temperature")
    private String pointName;

    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1)", example = "ENABLE")
    private EnableFlagEnum enableFlag;

    @Schema(description = "Lower bound for create_time filtering")
    private LocalDateTime createTimeFrom;

    @Schema(description = "Fallback rolling time range in hours", example = "24")
    private Integer rangeHours;

    @Schema(description = "Preset time range key: today, 24h, 7d, or 30d", example = "24h")
    private String rangeKey;
}
