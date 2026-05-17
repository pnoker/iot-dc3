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
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorImplTest {

    private final RuleEvaluatorImpl evaluator = new RuleEvaluatorImpl();

    @Test
    void matchesNumericThresholdRule() {
        RuleBO rule = rule(">", BigDecimal.valueOf(80), null);
        RuleFact fact = fact(Map.of("numValue", BigDecimal.valueOf(86.5)));

        assertThat(evaluator.matches(rule, fact)).isTrue();
    }

    @Test
    void doesNotMatchWhenThresholdIsMissing() {
        RuleBO rule = rule("<", null, null);
        RuleFact fact = fact(Map.of("numValue", BigDecimal.valueOf(70)));

        assertThat(evaluator.matches(rule, fact)).isFalse();
    }

    @Test
    void matchesTextStatusRule() {
        RuleBO rule = rule("==", null, "offline");
        rule.getRuleExt().getContent().getCondition().setField("status");
        RuleFact fact = fact(Map.of("status", "offline"));

        assertThat(evaluator.matches(rule, fact)).isTrue();
    }

    @Test
    void evaluatesRecoveryCondition() {
        RuleBO rule = rule(">", BigDecimal.valueOf(80), null);
        rule.getRuleExt().getContent().setRecovery(new RuleExt.Recovery(true, "<=", BigDecimal.valueOf(75), "PT2M"));
        RuleFact fact = fact(Map.of("numValue", BigDecimal.valueOf(72)));

        assertThat(evaluator.recovers(rule, fact)).isTrue();
    }

    private RuleBO rule(String operator, BigDecimal threshold, String expected) {
        RuleExt.Content content = new RuleExt.Content(
                new RuleExt.Condition("numValue", operator, expected, threshold, null, null, "C"),
                new RuleExt.Window("LAST", "PT1M", 1),
                null,
                "P1",
                "ALARM",
                List.of("temperature"));
        RuleExt ext = new RuleExt(content);
        ext.setType("POINT_VALUE_RULE");
        ext.setVersion(1);

        RuleBO rule = new RuleBO();
        rule.setId(1L);
        rule.setRuleCode("temperature-high");
        rule.setRuleExt(ext);
        return rule;
    }

    private RuleFact fact(Map<String, Object> values) {
        return new RuleFact(
                1L,
                AlarmTargetTypeFlagEnum.POINT,
                1001L,
                2001L,
                LocalDateTime.now(),
                values);
    }

}
