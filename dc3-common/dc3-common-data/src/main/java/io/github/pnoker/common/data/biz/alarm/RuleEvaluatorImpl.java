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
import io.github.pnoker.common.enums.WindowMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic evaluator for structured alarm rules. Dispatches by window
 * mode: LAST (or no window) evaluates the rule's condition against the fact's
 * named field directly; everything else delegates to
 * {@link WindowedRuleEvaluator}.
 *
 * <p>Invalid window specs (unknown mode, malformed duration) should already
 * be rejected upstream by the save validator. If one slips through, the
 * evaluator skips it with a one-time warn per rule id.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEvaluatorImpl implements RuleEvaluator {

    private final WindowedRuleEvaluator windowedRuleEvaluator;

    private final Set<Long> warnedInvalidRules = ConcurrentHashMap.newKeySet();

    private static RuleExt.Window window(RuleBO rule) {
        if (Objects.isNull(rule.getRuleExt()) || Objects.isNull(rule.getRuleExt().getContent())) {
            return null;
        }
        return rule.getRuleExt().getContent().getWindow();
    }

    private static RuleExt.Condition condition(RuleBO rule) {
        if (Objects.isNull(rule.getRuleExt()) || Objects.isNull(rule.getRuleExt().getContent())) {
            return null;
        }
        return rule.getRuleExt().getContent().getCondition();
    }

    @Override
    public boolean matches(RuleBO rule, RuleFact fact) {
        if (Objects.isNull(rule) || Objects.isNull(fact)) {
            return false;
        }
        WindowSpec spec = parseSpec(rule);
        if (!spec.valid()) {
            return false;
        }
        if (spec.mode() != WindowMode.LAST) {
            return windowedRuleEvaluator.matches(rule, fact, spec);
        }
        RuleExt.Condition condition = condition(rule);
        if (Objects.isNull(condition)) {
            return false;
        }
        return ConditionEvaluator.evaluate(condition, fact.value(condition.getField()));
    }

    @Override
    public boolean recovers(RuleBO rule, RuleFact fact) {
        if (Objects.isNull(rule) || Objects.isNull(rule.getRuleExt()) || Objects.isNull(rule.getRuleExt().getContent())
                || Objects.isNull(fact)) {
            return false;
        }
        WindowSpec spec = parseSpec(rule);
        if (!spec.valid()) {
            return false;
        }
        if (spec.mode() != WindowMode.LAST) {
            return windowedRuleEvaluator.recovers(rule, fact, spec);
        }
        RuleExt.Recovery recovery = rule.getRuleExt().getContent().getRecovery();
        RuleExt.Condition condition = rule.getRuleExt().getContent().getCondition();
        if (Objects.isNull(recovery) || !Boolean.TRUE.equals(recovery.getEnabled()) || Objects.isNull(condition)) {
            return false;
        }
        RuleExt.Condition recoveryCondition = ConditionEvaluator.recoveryConditionOf(condition, recovery);
        return ConditionEvaluator.evaluate(recoveryCondition, fact.value(condition.getField()));
    }

    private WindowSpec parseSpec(RuleBO rule) {
        RuleExt.Window window = window(rule);
        WindowSpec spec = WindowSpecParser.parse(window);
        if (!spec.valid() && Objects.nonNull(rule.getId()) && warnedInvalidRules.add(rule.getId())) {
            log.warn("Skipping rule[{}] because window spec is invalid: {}", rule.getId(), spec.reason());
        }
        return spec;
    }

}
