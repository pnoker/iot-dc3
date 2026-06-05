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

import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.WindowModeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Window-aware rule evaluation. Handles every {@link WindowModeEnum} other than
 * {@link WindowModeEnum#LAST} (which {@link RuleEvaluatorImpl} continues to
 * service inline because it does not need the data-source plumbing).
 *
 * <p>Behavior:
 *
 * <ul>
 *   <li>AVG/MIN/MAX/SUM/COUNT — pull a scalar aggregate from
 *       {@link WindowDataSource#aggregate(WindowSpec, RuleFact, WindowModeEnum)}
 *       and feed it into {@link ConditionEvaluator}; minSamples gate enforces
 *       the rule's "needs at least N samples" hint.</li>
 *   <li>ALL — every sample in the window must satisfy the rule's per-sample
 *       condition.</li>
 *   <li>ANY — at least one sample must satisfy.</li>
 * </ul>
 *
 * <p>Recovery uses the synthesized recovery condition (operator + threshold
 * from {@code RuleExt.Recovery}) but the same window + mode. That is the
 * legacy behavior — recovery does not get a separate window definition.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowedRuleEvaluator {

    private final WindowDataSource windowDataSource;

    /**
     * Pick the sample's numeric or text projection depending on the field the
     * rule's condition selects. Supports {@code numValue}, {@code calValue} /
     * {@code value} / {@code rawValue}; everything else falls back to the
     * numeric projection — the condition's operator coerces from there.
     */
    private static Object sampleValue(RuleExt.Condition condition, WindowSample sample) {
        if (Objects.isNull(condition) || Objects.isNull(condition.getField())) {
            return sample.numValue();
        }
        return switch (condition.getField()) {
            case "numValue" -> sample.numValue();
            case "calValue", "value", "rawValue" -> sample.calValue();
            default -> sample.numValue();
        };
    }

    private static RuleExt.Condition condition(RuleBO rule) {
        if (Objects.isNull(rule) || Objects.isNull(rule.getRuleExt()) || Objects.isNull(rule.getRuleExt().getContent())) {
            return null;
        }
        return rule.getRuleExt().getContent().getCondition();
    }

    /**
     * Evaluate the rule's firing branch against the windowed samples.
     */
    public boolean matches(RuleBO rule, RuleFact fact, WindowSpec spec) {
        RuleExt.Condition condition = condition(rule);
        if (Objects.isNull(condition) || Objects.isNull(spec) || !spec.valid() || spec.mode() == WindowModeEnum.LAST) {
            return false;
        }
        return evaluate(spec, fact, condition);
    }

    /**
     * Evaluate the rule's recovery branch against the windowed samples. Mirrors
     * {@link RuleEvaluatorImpl#recovers} for the LAST path: only fires when
     * recovery is enabled, the operator is non-null, and the synthesized
     * condition holds.
     */
    public boolean recovers(RuleBO rule, RuleFact fact, WindowSpec spec) {
        if (Objects.isNull(rule) || Objects.isNull(rule.getRuleExt())
                || Objects.isNull(rule.getRuleExt().getContent())) {
            return false;
        }
        RuleExt.Recovery recovery = rule.getRuleExt().getContent().getRecovery();
        RuleExt.Condition condition = rule.getRuleExt().getContent().getCondition();
        if (Objects.isNull(recovery) || !Boolean.TRUE.equals(recovery.getEnabled())
                || Objects.isNull(condition) || Objects.isNull(spec) || !spec.valid()
                || spec.mode() == WindowModeEnum.LAST) {
            return false;
        }
        return evaluate(spec, fact, ConditionEvaluator.recoveryConditionOf(condition, recovery));
    }

    private boolean evaluate(WindowSpec spec, RuleFact fact, RuleExt.Condition condition) {
        if (Objects.isNull(condition)) {
            return false;
        }
        WindowModeEnum mode = spec.mode();
        return switch (mode) {
            case AVG, MIN, MAX, SUM, COUNT -> evaluateAggregate(spec, fact, mode, condition);
            case ALL, ANY -> evaluateFold(spec, fact, mode, condition);
            default -> false;
        };
    }

    private boolean evaluateAggregate(WindowSpec spec, RuleFact fact, WindowModeEnum mode, RuleExt.Condition condition) {
        WindowDataSource.AggregateOutcome outcome = windowDataSource.aggregate(spec, fact, mode);
        if (outcome.sampleCount() < spec.minSamples()) {
            return false;
        }
        return ConditionEvaluator.evaluate(condition, outcome.value());
    }

    private boolean evaluateFold(WindowSpec spec, RuleFact fact, WindowModeEnum mode, RuleExt.Condition condition) {
        List<WindowSample> samples = windowDataSource.samples(spec, fact);
        if (samples.size() < spec.minSamples()) {
            return false;
        }
        if (samples.isEmpty()) {
            return false;
        }
        return switch (mode) {
            case ALL ->
                    samples.stream().allMatch(s -> ConditionEvaluator.evaluate(condition, sampleValue(condition, s)));
            case ANY ->
                    samples.stream().anyMatch(s -> ConditionEvaluator.evaluate(condition, sampleValue(condition, s)));
            default -> false;
        };
    }

}
