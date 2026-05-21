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
import io.github.pnoker.common.data.entity.property.AlarmWindowProperties;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test exercising the full short-window evaluation chain with
 * real WindowSampleBuffer + LocalWindowDataSource + WindowedRuleEvaluator +
 * RuleEvaluatorImpl + RuleEngineImpl. Only the rule-loading and state-lookup
 * collaborators are mocked.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@ExtendWith(MockitoExtension.class)
class WindowedAlarmPipelineIntegrationTest {

    @Mock
    private RuleRegistry ruleRegistry;

    @Mock
    private RuleStateLookup ruleStateLookup;

    private WindowSampleBuffer buffer;
    private LocalWindowDataSource localSource;
    private WindowedRuleEvaluator windowedEvaluator;
    private RuleEvaluatorImpl ruleEvaluator;
    private RuleEngineImpl engine;

    private static final long TENANT_ID = 7L;
    private static final long POINT_ID = 11L;

    @BeforeEach
    void setUp() {
        AlarmWindowProperties props = new AlarmWindowProperties();
        buffer = new WindowSampleBuffer(props);
        localSource = new LocalWindowDataSource(buffer);
        windowedEvaluator = new WindowedRuleEvaluator(localSource);
        ruleEvaluator = new RuleEvaluatorImpl(windowedEvaluator);
        engine = new RuleEngineImpl(ruleRegistry, ruleStateLookup, ruleEvaluator);
    }

    private void feed(double value) {
        feed(value, "online");
    }

    private void feed(double value, String calValue) {
        LocalDateTime ts = LocalDateTime.now();
        buffer.append(
                WindowSampleKey.of(TENANT_ID, AlarmTargetTypeFlagEnum.POINT, POINT_ID),
                new WindowSample(value, calValue, ts));
    }

    private RuleFact factOnPoint() {
        // Use a synthetic fact that arrives just after the most recently
        // appended sample, so the evaluator's window includes it.
        return new RuleFact(TENANT_ID, AlarmTargetTypeFlagEnum.POINT, POINT_ID, null,
                LocalDateTime.now().plusNanos(1), Map.of("numValue", 0));
    }

    private static RuleBO rule(String mode, String operator, BigDecimal threshold, int minSamples) {
        return rule(mode, operator, threshold, minSamples, "numValue");
    }

    private static RuleBO rule(String mode, String operator, BigDecimal threshold, int minSamples, String field) {
        RuleExt.Content content = new RuleExt.Content(
                new RuleExt.Condition(field, operator, "offline", threshold, null, null, "C"),
                new RuleExt.Window(mode, "PT3M", minSamples),
                null,
                "P1",
                "ALARM",
                List.of("temperature"));
        RuleExt ext = new RuleExt(content);
        ext.setType("POINT_VALUE_RULE");
        ext.setVersion(1);
        RuleBO rule = new RuleBO();
        rule.setId(1L);
        rule.setRuleCode("rule");
        rule.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.POINT);
        rule.setRuleExt(ext);
        return rule;
    }

    @Test
    void avgRuleFiresWhenWindowAverageExceedsThreshold() {
        feed(80);
        feed(82);
        feed(85);
        feed(88);
        feed(90);
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(rule("AVG", ">", BigDecimal.valueOf(80), 3)));

        List<RuleMatch> matches = engine.evaluate(factOnPoint());

        // AVG = (80+82+85+88+90)/5 = 85 > 80 → firing.
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getMatchType()).isEqualTo(AlarmConstant.MATCH_TYPE_FIRING);
    }

    @Test
    void avgRuleDoesNotFireBelowMinSamples() {
        feed(80);
        feed(82);
        feed(85);
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(rule("AVG", ">", BigDecimal.valueOf(80), 5)));

        List<RuleMatch> matches = engine.evaluate(factOnPoint());

        // 3 samples < minSamples(5) → don't fire.
        assertThat(matches).isEmpty();
    }

    @Test
    void countRuleFiresOnSampleCount() {
        // Five samples, condition is COUNT > 4 → fire.
        feed(80);
        feed(82);
        feed(85);
        feed(88);
        feed(90);
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(rule("COUNT", ">", BigDecimal.valueOf(4), 1)));

        List<RuleMatch> matches = engine.evaluate(factOnPoint());

        assertThat(matches).hasSize(1);
    }

    @Test
    void anyModeFiresWhenOneSampleSatisfies() {
        // Mixed status: 4 online + 1 offline; ANY (status==offline) → fire.
        feed(0, "online");
        feed(0, "online");
        feed(0, "online");
        feed(0, "online");
        feed(0, "offline");
        RuleBO anyRule = rule("ANY", "==", null, 1, "calValue");
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(anyRule));

        List<RuleMatch> matches = engine.evaluate(factOnPoint());

        assertThat(matches).hasSize(1);
    }

    @Test
    void allModeDoesNotFireWhenOneSampleFails() {
        feed(0, "online");
        feed(0, "online");
        feed(0, "online");
        feed(0, "online");
        feed(0, "online");
        RuleBO allRule = rule("ALL", "==", null, 1, "calValue");
        // All online; the rule is ALL (calValue == offline) → don't fire.
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(allRule));

        List<RuleMatch> matches = engine.evaluate(factOnPoint());

        assertThat(matches).isEmpty();
    }

    @Test
    void lastModeUsesFactValueDirectly() {
        // No buffer feed — LAST evaluates the current fact's named field
        // via ConditionEvaluator without reading the window.
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(rule("LAST", ">", BigDecimal.valueOf(80), 1)));

        // factOnPoint() default value is 0 → don't fire
        assertThat(engine.evaluate(factOnPoint())).isEmpty();

        // Build a fact that carries a numValue of 95
        RuleFact firingFact = new RuleFact(TENANT_ID, AlarmTargetTypeFlagEnum.POINT, POINT_ID, null,
                LocalDateTime.now(), Map.of("numValue", 95));
        assertThat(engine.evaluate(firingFact)).hasSize(1);
    }

}
