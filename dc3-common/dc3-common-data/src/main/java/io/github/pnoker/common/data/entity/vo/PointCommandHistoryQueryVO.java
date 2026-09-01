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

package io.github.pnoker.common.data.entity.vo;

import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.enums.PointCommandTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Query view object for point command list API.
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Point Command History view object")
public class PointCommandHistoryQueryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Identifier of the device to filter by; must belong to the current tenant.", example = "1024")
    private String deviceId;

    @Schema(description = "Identifier of the data point to filter by; must belong to the current tenant.", example = "2048")
    private String pointId;

    @Schema(description = "Lifecycle status of the command to filter by (e.g. pending, success, failed).", example = "SUCCESS")
    private PointCommandStatusEnum status;

    @Schema(description = "Type of the point command to filter by (e.g. read, write).", example = "READ")
    private PointCommandTypeEnum type;

    @Schema(description = "Zero-based result offset.", example = "0")
    private Long offset = 0L;

    @Schema(description = "Maximum number of records.", example = "50")
    private Integer limit = PageRequest.DEFAULT_LIMIT;

    @Schema(description = "Stable, whitelisted sort fields.")
    private List<SortSpec> sort = List.of();

}
