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

package io.github.pnoker.common.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.pnoker.common.constant.service.McpConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Public MCP tool definition returned by JSON-RPC tools/list.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class McpToolDefinitionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;

    private String title;

    private String description;

    private Map<String, Object> inputSchema;

    private Annotations annotations;

    @JsonProperty(McpConstant.Field.META)
    private Metadata meta;

    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Annotations implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private boolean readOnlyHint;

        private boolean destructiveHint;

        private boolean idempotentHint;

        private boolean openWorldHint;

    }

    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @JsonProperty(McpConstant.Field.TOOL_ID_META)
        private String toolId;

        @JsonProperty(McpConstant.Field.PERMISSION_CODE_META)
        private String permissionCode;

        @JsonProperty(McpConstant.Field.RISK_LEVEL_META)
        private String riskLevel;

    }

}
