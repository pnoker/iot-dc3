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

import java.util.List;
import java.util.Map;

/**
 * JSON extension object for event attribute configuration.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Schema(description = "JSON extension object for event attribute configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventAttributeExt extends BaseExt {

    /**
     *
     * <p>
     * Type Version
     */
    @Schema(description = "Event attribute configuration content")
    private Content content;

    @Schema(description = "Event attribute configuration content")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {

        @Schema(description = "Whether to keep the attribute value persistently", example = "true")
        private String keep;

        @Schema(description = "UI rendering configuration for the attribute")
        private Ui ui;

        @Schema(description = "Validation rules for the attribute value")
        private Validation validation;

        @Schema(description = "Security configuration for the attribute")
        private Security security;

        @Schema(description = "Scope to which the attribute applies")
        private AppliesTo appliesTo;

    }

    @Schema(description = "UI rendering configuration for the attribute", example = "form")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ui {

        @Schema(description = "UI component type used to render the attribute", example = "input")
        private String component;

        @Schema(description = "Whether the attribute is required in the UI form", example = "true")
        private Boolean required;

        @Schema(description = "Placeholder text displayed in the input field", example = "请输入")
        private String placeholder;

        @Schema(description = "Selectable options for the attribute, each as a key-value map")
        private List<Map<String, Object>> options;

        @Schema(description = "Group name used to organize the attribute in the UI", example = "basic")
        private String group;

        @Schema(description = "Display order of the attribute within its group", example = "1")
        private Integer order;

        @Schema(description = "Variables expression used by the UI component")
        private String variables;

    }

    @Schema(description = "Validation rules for the attribute value", example = "standard")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Validation {

        @Schema(description = "Minimum allowed value or length for the attribute", example = "0")
        private String min;

        @Schema(description = "Maximum allowed value or length for the attribute", example = "100")
        private String max;

        @Schema(description = "Regular expression the attribute value must match", example = "^[a-zA-Z0-9_]+$")
        private String regex;

    }

    @Schema(description = "Security configuration for the attribute", example = "basic")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Security {

        @Schema(description = "Whether the attribute value is treated as a secret", example = "true")
        private Boolean secret;

    }

    @Schema(description = "Scope to which the attribute applies")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliesTo {

        @Schema(description = "Event type flags this attribute applies to")
        private List<String> eventTypeFlags;

        @Schema(description = "Event source types this attribute applies to")
        private List<String> eventSourceTypes;

    }

}
