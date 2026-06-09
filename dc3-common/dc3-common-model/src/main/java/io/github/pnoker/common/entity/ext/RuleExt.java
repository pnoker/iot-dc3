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

import java.math.BigDecimal;
import java.util.List;

/**
 * JSON extension object for rule configuration.
 * <p>
 * Extended information related to rules.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JSON extension object for rule configuration, carrying extended information related to rules.")
public class RuleExt extends BaseExt {

    /**
     * Extended content.
     * <p>
     * The content can be distinguished by Type and Version.
     */
    @Schema(description = "Extended content, which can be distinguished by type and version.")
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Rule extended content, holding the condition, window, recovery and metadata of a rule.")
    public static class Content {

        /**
         * Deterministic rule condition.
         */
        @Schema(description = "Deterministic rule condition.")
        private Condition condition;

        /**
         * Evaluation window, expressed with ISO-8601 durations where time is needed.
         */
        @Schema(description = "Evaluation window, expressed with ISO-8601 durations where time is needed.")
        private Window window;

        /**
         * Optional recovery condition.
         */
        @Schema(description = "Optional recovery condition.")
        private Recovery recovery;

        /**
         * Alarm level, for example P0/P1/P2/P3.
         */
        @Schema(description = "Alarm level, for example P0/P1/P2/P3.", example = "P0")
        private String severity;

        /**
         * Event type produced when the rule is matched.
         */
        @Schema(description = "Event type produced when the rule is matched.")
        private String eventType;

        /**
         * Business labels for filtering and dashboards.
         */
        @Schema(description = "Business labels for filtering and dashboards.")
        private List<String> labels;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Deterministic rule condition describing the field, operator and comparison values.")
    public static class Condition {

        /**
         * Source field to evaluate, for example numValue, status, or age.
         */
        @Schema(description = "Source field to evaluate, for example numValue, status, or age.", example = "numValue")
        private String field;

        /**
         * Operator code, for example >, >=, <, <=, ==, !=, between, outside, silence.
         */
        @Schema(description = "Operator code, for example >, >=, <, <=, ==, !=, between, outside, silence.", example = ">")
        private String operator;

        /**
         * Expected string value for status-like rules.
         */
        @Schema(description = "Expected string value for status-like rules.")
        private String expected;

        /**
         * Numeric threshold for threshold rules.
         */
        @Schema(description = "Numeric threshold for threshold rules.")
        private BigDecimal threshold;

        /**
         * Lower bound for range rules.
         */
        @Schema(description = "Lower bound for range rules.")
        private BigDecimal low;

        /**
         * Upper bound for range rules.
         */
        @Schema(description = "Upper bound for range rules.")
        private BigDecimal high;

        /**
         * Display unit for value comparisons.
         */
        @Schema(description = "Display unit for value comparisons.")
        private String unit;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Evaluation window defining the aggregation mode, duration and minimum sample count.")
    public static class Window {

        /**
         * Evaluation mode, for example LAST, ALL, ANY, AVG, MIN, MAX, SUM, COUNT.
         */
        @Schema(description = "Evaluation mode, for example LAST, ALL, ANY, AVG, MIN, MAX, SUM, COUNT.", example = "AVG")
        private String mode;

        /**
         * ISO-8601 duration such as PT3M.
         */
        @Schema(description = "ISO-8601 duration such as PT3M.", example = "PT3M")
        private String duration;

        /**
         * Minimum samples required in the window.
         */
        @Schema(description = "Minimum samples required in the window.")
        private Integer minSamples;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Optional recovery condition describing when an alarm is considered recovered.")
    public static class Recovery {

        /**
         * Whether recovery evaluation is enabled.
         */
        @Schema(description = "Whether recovery evaluation is enabled.", example = "true")
        private Boolean enabled;

        /**
         * Recovery operator.
         */
        @Schema(description = "Recovery operator.", example = "<")
        private String operator;

        /**
         * Recovery threshold.
         */
        @Schema(description = "Recovery threshold.")
        private BigDecimal threshold;

        /**
         * ISO-8601 duration that must remain recovered.
         */
        @Schema(description = "ISO-8601 duration that must remain recovered.", example = "PT5M")
        private String duration;

    }

}
