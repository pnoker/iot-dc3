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
 * JSON extension object for command attribute configuration.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Schema(description = "Command attribute extension. JSON extension object describing the configuration of a single command attribute, including UI rendering, validation rules, security and applicability scope.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommandAttributeExt extends BaseExt {

    /**
     *
     * <p>
     * Type Version
     */
    @Schema(description = "Command attribute content, holding the detailed configuration of the attribute.")
    private Content content;

    @Schema(description = "Command attribute content. Detailed configuration of a command attribute, grouped into retention, UI, validation, security and applicability sections.")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {

        @Schema(description = "Retention strategy for the attribute value, indicating whether and how the value is kept.")
        private String keep;

        @Schema(description = "UI rendering configuration for the attribute.")
        private Ui ui;

        @Schema(description = "Validation rules applied to the attribute value.")
        private Validation validation;

        @Schema(description = "Security configuration for the attribute, such as whether the value is sensitive.")
        private Security security;

        @Schema(description = "Applicability scope describing which command and call types the attribute applies to.")
        private AppliesTo appliesTo;

    }

    @Schema(description = "UI rendering configuration. Describes how the command attribute is displayed and edited in the front-end form.")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ui {

        @Schema(description = "Front-end component type used to render the attribute input.")
        private String component;

        @Schema(description = "Whether the attribute is required in the UI form.")
        private Boolean required;

        @Schema(description = "Placeholder text displayed in the input when empty.")
        private String placeholder;

        @Schema(description = "Selectable options for the input, each option represented as a key-value map.")
        private List<Map<String, Object>> options;

        @Schema(description = "Group name used to organize related attributes in the UI.")
        private String group;

        @Schema(description = "Display order of the attribute within its group.")
        private Integer order;

        @Schema(description = "Associated variables used during rendering or value substitution.")
        private String variables;

    }

    @Schema(description = "Validation rules. Constraints applied to the command attribute value.")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Validation {

        @Schema(description = "Minimum allowed value or length for the attribute.")
        private String min;

        @Schema(description = "Maximum allowed value or length for the attribute.")
        private String max;

        @Schema(description = "Regular expression that the attribute value must match.")
        private String regex;

    }

    @Schema(description = "Security configuration. Security-related settings for the command attribute.")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Security {

        @Schema(description = "Whether the attribute value is sensitive and should be masked or protected.")
        private Boolean secret;

    }

    @Schema(description = "Applicability scope. Defines the command and call types the attribute applies to.")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliesTo {

        @Schema(description = "List of command type flags the attribute applies to.")
        private List<String> commandTypeFlags;

        @Schema(description = "List of call type flags the attribute applies to.")
        private List<String> callTypeFlags;

    }

}
