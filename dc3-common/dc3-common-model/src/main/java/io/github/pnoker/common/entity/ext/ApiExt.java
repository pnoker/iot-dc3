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

package io.github.pnoker.common.entity.ext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JSON extension object for API interface configuration.
 * <p>
 * Extended information related to API interfaces.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Schema(description = "JSON extension object for API interface configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiExt extends BaseExt {

    /**
     * Extended content.
     * <p>
     * The content can be distinguished by Type and Version.
     */
    @Schema(description = "Extended content of the API interface; discriminated by Type and Version of the parent extension")
    private Content content;

    @Schema(description = "Extended content of the API interface")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {

        /**
         * Title.
         */
        @Schema(description = "Human-readable title of the API interface", example = "Device Temperature Query")
        private String title;

        /**
         * URL link.
         */
        @Schema(description = "URL link of the API interface; must be a valid HTTP(S) endpoint", example = "https://example.com/api/device/temperature")
        private String url;

        /**
         * Description.
         */
        @Schema(description = "Free-form description remark of the API interface", example = "Returns the latest temperature reading for a device")
        private String remark;

        /**
         * Declared MCP risk level (LOW / MEDIUM / HIGH); blank when derived automatically.
         */
        @Schema(description = "Declared MCP risk level (LOW / MEDIUM / HIGH); blank when derived automatically", example = "MEDIUM")
        private String riskLevel;

        /**
         * Declared destructive hint ("true" / "false"); blank when derived.
         */
        @Schema(description = "Declared MCP destructive hint (\"true\" / \"false\"); blank when derived automatically", example = "false")
        private String destructiveHint;

        /**
         * Declared open-world hint ("true" / "false"); blank when derived.
         */
        @Schema(description = "Declared MCP open-world hint (\"true\" / \"false\"); blank when derived automatically", example = "false")
        private String openWorldHint;

        /**
         * Declared idempotent hint ("true" / "false"); blank when derived.
         */
        @Schema(description = "Declared MCP idempotent hint (\"true\" / \"false\"); blank when derived automatically", example = "true")
        private String idempotentHint;

        /**
         * AI-facing description override; blank when the operation text is used.
         */
        @Schema(description = "AI-facing MCP tool description override; blank when the operation text is used", example = "Fetch the current temperature for a given device id")
        private String aiDescription;

        /**
         * Whether the tool is hidden from tools/list by default ("true" / "false"); blank = visible.
         */
        @Schema(description = "Whether the tool is hidden from tools/list by default (\"true\" / \"false\"); blank means visible", example = "false")
        private String hidden;

    }

}
