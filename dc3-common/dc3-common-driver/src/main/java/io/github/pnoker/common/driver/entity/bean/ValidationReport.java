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

package io.github.pnoker.common.driver.entity.bean;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Validation report produced by {@code DriverValidator}.
 * <p>
 * Contains the overall pass/fail decision and a structured list of per-attribute
 * issues for diagnostics. An empty issue list means the validation passed.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Whether the validation passed (no ERROR-level issues).
     */
    @Builder.Default
    private boolean passed = true;

    /**
     * Structured list of issues found during validation.
     * May include both ERROR and WARNING level items.
     */
    @Builder.Default
    private List<AttributeIssue> issues = Collections.emptyList();

    /**
     * Convenience factory for a passing report with no issues.
     */
    public static ValidationReport passed() {
        return ValidationReport.builder()
                .passed(true)
                .issues(Collections.emptyList())
                .build();
    }

    /**
     * Issue severity.
     */
    public enum IssueLevel {
        ERROR,
        WARNING
    }

    /**
     * Per-attribute validation issue.
     */
    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AttributeIssue implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * The attribute code that failed validation.
         */
        private String attributeCode;

        /**
         * Error severity level.
         */
        private IssueLevel level;

        /**
         * Human-readable description of the problem.
         */
        private String message;

        /**
         * Expected value hint, e.g. "1-65535" or "non-empty string".
         */
        private String expected;
    }

}
