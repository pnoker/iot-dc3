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

    /**
     * Unique tool identifier.
     */
    private String name;

    /**
     * Human-readable display name.
     */
    private String title;

    /**
     * Tool description, surfaced to the model.
     */
    private String description;

    /**
     * JSON Schema describing the tool's input arguments.
     */
    private Map<String, Object> inputSchema;

    /**
     * MCP hints describing the tool's behavioral characteristics.
     */
    private Annotations annotations;

    /**
     * DC3 platform metadata layered onto the standard tool definition.
     */
    @JsonProperty(McpConstant.Field.META)
    private Metadata meta;

    /**
     * MCP tool annotations: behavioral hints that help clients reason about the safety
     * and side effects of a tool call (see MCP specification).
     */
    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Annotations implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Hint that the tool does not modify its environment.
         */
        private boolean readOnlyHint;

        /**
         * Hint that the tool may perform destructive, irreversible changes.
         */
        private boolean destructiveHint;

        /**
         * Hint that repeated calls with the same arguments yield the same result.
         */
        private boolean idempotentHint;

        /**
         * Hint that the tool may interact with entities outside the local environment.
         */
        private boolean openWorldHint;

    }

    /**
     * DC3-specific tool metadata: stable tool id, authorization code, and risk level,
     * used for permission gating and confirmation flows.
     */
    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Stable tool id, independent of the API code that backs the tool.
         */
        @JsonProperty(McpConstant.Field.TOOL_ID_META)
        private String toolId;

        /**
         * Permission code required to call the tool.
         */
        @JsonProperty(McpConstant.Field.PERMISSION_CODE_META)
        private String permissionCode;

        /**
         * Risk level: LOW, MEDIUM, or HIGH; HIGH tools require confirmation.
         */
        @JsonProperty(McpConstant.Field.RISK_LEVEL_META)
        private String riskLevel;

    }

}
