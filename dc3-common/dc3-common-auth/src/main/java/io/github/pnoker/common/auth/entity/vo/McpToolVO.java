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
import io.github.pnoker.common.entity.base.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for MCP tool catalog API responses.
 *
 * @author pnoker
 * @version 2026.6.19
 * @since 2026.6.19
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "MCP tool view object")
public class McpToolVO extends BaseVO {

    @Schema(description = "Stable tool identifier.", example = "tool_read_device")
    private String toolId;

    @Schema(description = "Tool name exposed to MCP clients.", example = "read_device")
    private String toolName;

    @Schema(description = "Human-readable tool title.", example = "Read Device")
    private String toolTitle;

    @Schema(description = "Tool category.", example = "device")
    private String toolCategory;

    @Schema(description = "Backing service name.", example = "dc3-manager")
    private String serviceName;

    @Schema(description = "Backing API code.", example = "device.get")
    private String apiCode;

    @Schema(description = "Permission code required to invoke the tool.", example = "device:get")
    private String permissionCode;

    @Schema(description = "Backing HTTP method.", example = "POST")
    private String httpMethod;

    @Schema(description = "Backing API path.", example = "/device/get_by_id")
    private String apiPath;

    @Schema(description = "Hash of the tool input schema.", example = "a1b2c3d4")
    private String schemaHash;

    @Schema(description = "Tool risk level: LOW, MEDIUM or HIGH.", example = "LOW")
    private String riskLevel;

    @Schema(description = "Read-only hint: 1 yes, 0 no.", example = "1")
    private Byte readOnlyHint;

    @Schema(description = "Destructive hint: 1 yes, 0 no.", example = "0")
    private Byte destructiveHint;

    @Schema(description = "Idempotent hint: 1 yes, 0 no.", example = "1")
    private Byte idempotentHint;

    @Schema(description = "Open-world hint: 1 yes, 0 no.", example = "0")
    private Byte openWorldHint;

    @Schema(description = "Enable flag: 0 enabled, 1 disabled.", example = "0")
    private Byte enableFlag;

    @Schema(description = "JSON envelope carrying the tool input schema.", example = "{\"inputSchema\":{}}")
    private String toolExt;

}
