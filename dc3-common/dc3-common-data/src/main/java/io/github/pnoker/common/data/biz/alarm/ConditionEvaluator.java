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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.constant.common.BaseConstant;
import io.github.pnoker.common.entity.ext.RuleExt;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Stateless rule-condition evaluator. Extracted so both
 * {@link RuleEvaluatorImpl} (single-fact path) and {@link WindowedRuleEvaluator}
 * (windowed path) apply the exact same operator semantics — once the windowed
 * path has reduced its samples to a scalar (or is folding ALL/ANY per sample),
 * it just calls back into here.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * Evaluate {@code condition} against {@code value}. Returns false when the
     * condition is ill-formed or the value cannot be coerced.
     */
    public static boolean evaluate(RuleExt.Condition condition, Object value) {
        if (Objects.isNull(condition)) {
            return false;
        }
        String operator = StringUtils.lowerCase(StringUtils.trim(condition.getOperator()));
        if ("exists".equals(operator)) {
            return Objects.nonNull(value);
        }
        if ("missing".equals(operator)) {
            return Objects.isNull(value);
        }
        if (Objects.isNull(value)) {
            return false;
        }

        BigDecimal actual = toBigDecimal(value);
        if (isNumericOperator(operator, condition)) {
            return evaluateNumeric(operator, actual, condition);
        }
        return evaluateText(operator, value, condition);
    }

    /**
     * Synthesize the recovery-side condition: same field + unit, but the
     * operator and threshold come from {@link RuleExt.Recovery}. Mirrors the
     * legacy logic in {@code RuleEvaluatorImpl.recovers}.
     */
    public static RuleExt.Condition recoveryConditionOf(RuleExt.Condition condition, RuleExt.Recovery recovery) {
        if (Objects.isNull(condition) || Objects.isNull(recovery)) {
            return null;
        }
        return new RuleExt.Condition(
                condition.getField(),
                recovery.getOperator(),
                null,
                recovery.getThreshold(),
                null,
                null,
                condition.getUnit());
    }

    /**
     * Coerce a value into a {@link BigDecimal} for numeric comparisons.
     * Returns null when the value isn't a number / numeric string.
     */
    public static BigDecimal toBigDecimal(Object value) {
        try {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            if (value instanceof CharSequence text && StringUtils.isNotBlank(text)) {
                return new BigDecimal(text.toString());
            }
            return null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Evaluate a numeric condition against the actual value, dispatching by operator
     * ({@code > >= < <= == != between outside}). Returns false when the actual value or
     * the required thresholds are null.
     *
     * @param operator  the comparison operator
     * @param actual    the actual numeric value
     * @param condition the condition carrying thresholds
     * @return true if the condition is met
     */
    private static boolean evaluateNumeric(String operator, BigDecimal actual, RuleExt.Condition condition) {
        if (Objects.isNull(actual)) {
            return false;
        }
        return switch (operator) {
            case ">" -> condition.getThreshold() != null && actual.compareTo(condition.getThreshold()) > 0;
            case ">=" -> condition.getThreshold() != null && actual.compareTo(condition.getThreshold()) >= 0;
            case "<" -> condition.getThreshold() != null && actual.compareTo(condition.getThreshold()) < 0;
            case "<=" -> condition.getThreshold() != null && actual.compareTo(condition.getThreshold()) <= 0;
            case "==" -> condition.getThreshold() != null && actual.compareTo(condition.getThreshold()) == 0;
            case "!=" -> condition.getThreshold() != null && actual.compareTo(condition.getThreshold()) != 0;
            case "between" -> condition.getLow() != null && condition.getHigh() != null
                    && actual.compareTo(condition.getLow()) >= 0 && actual.compareTo(condition.getHigh()) <= 0;
            case "outside" -> condition.getLow() != null && condition.getHigh() != null
                    && (actual.compareTo(condition.getLow()) < 0 || actual.compareTo(condition.getHigh()) > 0);
            default -> false;
        };
    }

    /**
     * Evaluate a text condition against the actual value, dispatching by operator
     * ({@code == eq != ne contains}).
     *
     * @param operator  the comparison operator
     * @param value     the actual value
     * @param condition the condition carrying the expected text
     * @return true if the condition is met
     */
    private static boolean evaluateText(String operator, Object value, RuleExt.Condition condition) {
        String actual = Objects.toString(value, "");
        String expected = Objects.toString(condition.getExpected(), "");
        return switch (operator) {
            case "==", "eq" -> StringUtils.equals(actual, expected);
            case "!=", "ne" -> !StringUtils.equals(actual, expected);
            case "contains" -> StringUtils.contains(actual, expected);
            default -> false;
        };
    }

    /**
     * Return whether an operator is numeric: the comparison and range operators always
     * are; {@code ==}/{@code !=} are numeric only when a threshold is set.
     *
     * @param operator  the operator
     * @param condition the condition
     * @return true if the operator is numeric
     */
    private static boolean isNumericOperator(String operator, RuleExt.Condition condition) {
        return switch (operator) {
            case ">", ">=", "<", "<=", "between", "outside" -> true;
            case "==", "!=" -> condition.getThreshold() != null;
            default -> false;
        };
    }

}
