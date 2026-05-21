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

import io.github.pnoker.common.constant.service.AlarmConstant;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.entity.ext.RuleExt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic evaluator for structured alarm rules.
 *
 * <p>Window-aware rules are not yet implemented in this release. The only
 * supported {@code RuleExt.Window.mode} value is {@code LAST} (or null/blank,
 * which is treated as LAST). Any other mode (AVG/MIN/MAX/SUM/COUNT/ALL/ANY) is
 * skipped at evaluation time with a one-time WARN per rule id, so a rule
 * configured against an unimplemented mode will not silently behave as LAST.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
public class RuleEvaluatorImpl implements RuleEvaluator {

    private final Set<Long> warnedUnsupportedRules = ConcurrentHashMap.newKeySet();

    @Override
    public boolean matches(RuleBO rule, RuleFact fact) {
        if (!isWindowModeSupported(rule)) {
            return false;
        }
        RuleExt.Condition condition = condition(rule);
        if (Objects.isNull(condition) || Objects.isNull(fact)) {
            return false;
        }
        return evaluate(condition, fact.value(condition.getField()));
    }

    @Override
    public boolean recovers(RuleBO rule, RuleFact fact) {
        if (Objects.isNull(rule) || Objects.isNull(rule.getRuleExt()) || Objects.isNull(rule.getRuleExt().getContent())
                || Objects.isNull(fact)) {
            return false;
        }
        if (!isWindowModeSupported(rule)) {
            return false;
        }
        RuleExt.Recovery recovery = rule.getRuleExt().getContent().getRecovery();
        RuleExt.Condition condition = rule.getRuleExt().getContent().getCondition();
        if (Objects.isNull(recovery) || !Boolean.TRUE.equals(recovery.getEnabled()) || Objects.isNull(condition)) {
            return false;
        }
        RuleExt.Condition recoveryCondition = new RuleExt.Condition(
                condition.getField(),
                recovery.getOperator(),
                null,
                recovery.getThreshold(),
                null,
                null,
                condition.getUnit());
        return evaluate(recoveryCondition, fact.value(condition.getField()));
    }

    private boolean isWindowModeSupported(RuleBO rule) {
        if (Objects.isNull(rule) || Objects.isNull(rule.getRuleExt())
                || Objects.isNull(rule.getRuleExt().getContent())) {
            return true;
        }
        RuleExt.Window window = rule.getRuleExt().getContent().getWindow();
        if (Objects.isNull(window) || StringUtils.isBlank(window.getMode())) {
            return true;
        }
        if (StringUtils.equalsIgnoreCase(window.getMode(), AlarmConstant.WINDOW_MODE_LAST)) {
            return true;
        }
        if (Objects.nonNull(rule.getId()) && warnedUnsupportedRules.add(rule.getId())) {
            log.warn("Skipping rule[{}] because window mode '{}' is not yet supported; only LAST is implemented",
                    rule.getId(), window.getMode());
        }
        return false;
    }

    private RuleExt.Condition condition(RuleBO rule) {
        if (Objects.isNull(rule) || Objects.isNull(rule.getRuleExt()) || Objects.isNull(rule.getRuleExt().getContent())) {
            return null;
        }
        return rule.getRuleExt().getContent().getCondition();
    }

    private boolean evaluate(RuleExt.Condition condition, Object value) {
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

    private boolean evaluateNumeric(String operator, BigDecimal actual, RuleExt.Condition condition) {
        if (Objects.isNull(actual)) {
            return false;
        }
        return switch (operator) {
            case ">" -> condition.getThreshold() != null && compare(actual, condition.getThreshold()) > 0;
            case ">=" -> condition.getThreshold() != null && compare(actual, condition.getThreshold()) >= 0;
            case "<" -> condition.getThreshold() != null && compare(actual, condition.getThreshold()) < 0;
            case "<=" -> condition.getThreshold() != null && compare(actual, condition.getThreshold()) <= 0;
            case "==" -> condition.getThreshold() != null && compare(actual, condition.getThreshold()) == 0;
            case "!=" -> condition.getThreshold() != null && compare(actual, condition.getThreshold()) != 0;
            case "between" -> condition.getLow() != null && condition.getHigh() != null
                    && compare(actual, condition.getLow()) >= 0 && compare(actual, condition.getHigh()) <= 0;
            case "outside" -> condition.getLow() != null && condition.getHigh() != null
                    && (compare(actual, condition.getLow()) < 0 || compare(actual, condition.getHigh()) > 0);
            default -> false;
        };
    }

    private boolean evaluateText(String operator, Object value, RuleExt.Condition condition) {
        String actual = Objects.toString(value, "");
        String expected = Objects.toString(condition.getExpected(), "");
        return switch (operator) {
            case "==", "eq" -> StringUtils.equals(actual, expected);
            case "!=", "ne" -> !StringUtils.equals(actual, expected);
            case "contains" -> StringUtils.contains(actual, expected);
            default -> false;
        };
    }

    private boolean isNumericOperator(String operator, RuleExt.Condition condition) {
        return switch (operator) {
            case ">", ">=", "<", "<=", "between", "outside" -> true;
            case "==", "!=" -> condition.getThreshold() != null;
            default -> false;
        };
    }

    private int compare(BigDecimal actual, BigDecimal expected) {
        return actual.compareTo(expected);
    }

    private BigDecimal toBigDecimal(Object value) {
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
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
