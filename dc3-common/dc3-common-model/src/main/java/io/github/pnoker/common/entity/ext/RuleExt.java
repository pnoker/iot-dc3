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
public class RuleExt extends BaseExt {

    /**
     * Extended content.
     * <p>
     * The content can be distinguished by Type and Version.
     */
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {

        /**
         * Deterministic rule condition.
         */
        private Condition condition;

        /**
         * Evaluation window, expressed with ISO-8601 durations where time is needed.
         */
        private Window window;

        /**
         * Optional recovery condition.
         */
        private Recovery recovery;

        /**
         * Alarm level, for example P0/P1/P2/P3.
         */
        private String severity;

        /**
         * Event type produced when the rule is matched.
         */
        private String eventType;

        /**
         * Business labels for filtering and dashboards.
         */
        private List<String> labels;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {

        /**
         * Source field to evaluate, for example numValue, status, or age.
         */
        private String field;

        /**
         * Operator code, for example >, >=, <, <=, ==, !=, between, outside, silence.
         */
        private String operator;

        /**
         * Expected string value for status-like rules.
         */
        private String expected;

        /**
         * Numeric threshold for threshold rules.
         */
        private BigDecimal threshold;

        /**
         * Lower bound for range rules.
         */
        private BigDecimal low;

        /**
         * Upper bound for range rules.
         */
        private BigDecimal high;

        /**
         * Display unit for value comparisons.
         */
        private String unit;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Window {

        /**
         * Evaluation mode, for example LAST, ALL, ANY, AVG, MIN, MAX, SUM, COUNT.
         */
        private String mode;

        /**
         * ISO-8601 duration such as PT3M.
         */
        private String duration;

        /**
         * Minimum samples required in the window.
         */
        private Integer minSamples;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recovery {

        /**
         * Whether recovery evaluation is enabled.
         */
        private Boolean enabled;

        /**
         * Recovery operator.
         */
        private String operator;

        /**
         * Recovery threshold.
         */
        private BigDecimal threshold;

        /**
         * ISO-8601 duration that must remain recovered.
         */
        private String duration;

    }

}
