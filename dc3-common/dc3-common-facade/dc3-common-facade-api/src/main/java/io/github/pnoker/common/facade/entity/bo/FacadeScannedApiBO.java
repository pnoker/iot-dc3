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

package io.github.pnoker.common.facade.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Facade-level representation of a single scanned HTTP endpoint.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Facade Scanned Api business object")
public class FacadeScannedApiBO {

    @Schema(description = "method")

    private String method;

    @Schema(description = "path")

    private String path;

    @Schema(description = "API name")

    private String apiName;

    @Schema(description = "Title")

    private String title;

    @Schema(description = "Description / remark")

    private String remark;

    /**
     * API grouping label — usually the owning controller's simple class name.
     */
    @Schema(description = "API grouping label")
    private String apiGroup;

    @Schema(description = "Declared MCP risk level, blank when derived")

    private String riskLevel;

    @Schema(description = "Declared MCP destructive hint, blank when derived")

    private String destructiveHint;

    @Schema(description = "Declared MCP open-world hint, blank when derived")

    private String openWorldHint;

    @Schema(description = "Declared MCP idempotent hint, blank when derived")

    private String idempotentHint;

    @Schema(description = "AI-facing MCP tool description override")

    private String aiDescription;

    @Schema(description = "Whether the MCP tool is hidden from tools/list by default")

    private String hidden;

}
