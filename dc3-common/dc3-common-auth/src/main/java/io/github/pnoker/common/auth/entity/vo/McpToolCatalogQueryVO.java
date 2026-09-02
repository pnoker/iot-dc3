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
package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Query view object for paging the MCP tool catalog.
 *
 * @author pnoker
 * @since 2026.6.19
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "MCP tool catalog query")
public class McpToolCatalogQueryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Zero-based result offset.", example = "0", minimum = "0")
    private Long offset;

    @Schema(
            description = "Maximum number of results; bounded to 1..200.",
            example = "50",
            minimum = "1",
            maximum = "200")
    private Integer limit;

    @Schema(description = "Stable sort fields; only server-approved fields are accepted.")
    private List<SortSpec> sort;

    @Schema(description = "Fuzzy keyword over tool id, name and title.", example = "device")
    private String keyword;

    @Schema(description = "Filter by tool risk level: LOW, MEDIUM or HIGH.", example = "LOW")
    private String riskLevel;
}
