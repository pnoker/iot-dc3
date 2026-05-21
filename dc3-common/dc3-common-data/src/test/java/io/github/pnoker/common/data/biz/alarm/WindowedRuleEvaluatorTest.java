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
import io.github.pnoker.common.enums.WindowMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WindowedRuleEvaluatorTest {

    @Mock
    private WindowDataSource windowDataSource;

    @InjectMocks
    private WindowedRuleEvaluator evaluator;

    private static RuleBO rule(String operator, BigDecimal threshold, RuleExt.Recovery recovery) {
        RuleExt.Content content = new RuleExt.Content(
                new RuleExt.Condition("numValue", operator, null, threshold, null, null, "C"),
                null,
                recovery,
                "P1",
                "ALARM",
                List.of("temperature"));
        RuleExt ext = new RuleExt(content);
        ext.setType("POINT_VALUE_RULE");
        ext.setVersion(1);
        RuleBO rule = new RuleBO();
        rule.setId(1L);
        rule.setRuleCode("temp-high");
        rule.setRuleExt(ext);
        return rule;
    }

    private static RuleFact fact() {
        return new RuleFact(7L, AlarmTargetTypeFlagEnum.POINT, 11L, null, LocalDateTime.now(), Map.of());
    }

    private static WindowSpec spec(WindowMode mode, int minSamples) {
        return WindowSpec.ok(mode, Duration.ofMinutes(3), minSamples);
    }

    @Test
    void avgFiresWhenAggregateAboveThreshold() {
        when(windowDataSource.aggregate(any(), any(), eq(WindowMode.AVG)))
                .thenReturn(new WindowDataSource.AggregateOutcome(BigDecimal.valueOf(85), 5));

        boolean fired = evaluator.matches(rule(">", BigDecimal.valueOf(80), null), fact(), spec(WindowMode.AVG, 3));
        assertThat(fired).isTrue();
    }

    @Test
    void avgDoesNotFireBelowThreshold() {
        when(windowDataSource.aggregate(any(), any(), eq(WindowMode.AVG)))
                .thenReturn(new WindowDataSource.AggregateOutcome(BigDecimal.valueOf(72), 5));

        boolean fired = evaluator.matches(rule(">", BigDecimal.valueOf(80), null), fact(), spec(WindowMode.AVG, 3));
        assertThat(fired).isFalse();
    }

    @Test
    void respectsMinSamplesGate() {
        when(windowDataSource.aggregate(any(), any(), eq(WindowMode.AVG)))
                .thenReturn(new WindowDataSource.AggregateOutcome(BigDecimal.valueOf(85), 2));

        // 2 samples in the window but minSamples=5 → don't fire even though
        // the aggregate would otherwise satisfy the threshold.
        boolean fired = evaluator.matches(rule(">", BigDecimal.valueOf(80), null), fact(), spec(WindowMode.AVG, 5));
        assertThat(fired).isFalse();
    }

    @Test
    void countModeUsesSampleCountAsValue() {
        when(windowDataSource.aggregate(any(), any(), eq(WindowMode.COUNT)))
                .thenReturn(new WindowDataSource.AggregateOutcome(BigDecimal.valueOf(7), 7));

        boolean fired = evaluator.matches(rule(">=", BigDecimal.valueOf(5), null), fact(), spec(WindowMode.COUNT, 1));
        assertThat(fired).isTrue();
    }

    @Test
    void allModeFiresWhenEverySampleSatisfies() {
        List<WindowSample> samples = List.of(
                new WindowSample(85.0, "85", LocalDateTime.now()),
                new WindowSample(82.0, "82", LocalDateTime.now()),
                new WindowSample(90.0, "90", LocalDateTime.now()));
        when(windowDataSource.samples(any(), any())).thenReturn(samples);

        boolean fired = evaluator.matches(rule(">", BigDecimal.valueOf(80), null), fact(), spec(WindowMode.ALL, 3));
        assertThat(fired).isTrue();
    }

    @Test
    void allModeFailsWhenOneSampleDoesNotSatisfy() {
        List<WindowSample> samples = List.of(
                new WindowSample(85.0, "85", LocalDateTime.now()),
                new WindowSample(70.0, "70", LocalDateTime.now()),
                new WindowSample(90.0, "90", LocalDateTime.now()));
        when(windowDataSource.samples(any(), any())).thenReturn(samples);

        boolean fired = evaluator.matches(rule(">", BigDecimal.valueOf(80), null), fact(), spec(WindowMode.ALL, 3));
        assertThat(fired).isFalse();
    }

    @Test
    void anyModeFiresWhenAtLeastOneSampleSatisfies() {
        List<WindowSample> samples = List.of(
                new WindowSample(70.0, "70", LocalDateTime.now()),
                new WindowSample(72.0, "72", LocalDateTime.now()),
                new WindowSample(85.0, "85", LocalDateTime.now()));
        when(windowDataSource.samples(any(), any())).thenReturn(samples);

        boolean fired = evaluator.matches(rule(">", BigDecimal.valueOf(80), null), fact(), spec(WindowMode.ANY, 1));
        assertThat(fired).isTrue();
    }

    @Test
    void anyModeFailsWhenNoSampleSatisfies() {
        List<WindowSample> samples = List.of(
                new WindowSample(70.0, "70", LocalDateTime.now()),
                new WindowSample(75.0, "75", LocalDateTime.now()));
        when(windowDataSource.samples(any(), any())).thenReturn(samples);

        boolean fired = evaluator.matches(rule(">", BigDecimal.valueOf(80), null), fact(), spec(WindowMode.ANY, 1));
        assertThat(fired).isFalse();
    }

    @Test
    void allModeRespectsMinSamples() {
        // Only 1 sample present but minSamples=3 → don't fire even if it
        // satisfies the condition.
        when(windowDataSource.samples(any(), any())).thenReturn(List.of(
                new WindowSample(85.0, "85", LocalDateTime.now())));

        boolean fired = evaluator.matches(rule(">", BigDecimal.valueOf(80), null), fact(), spec(WindowMode.ALL, 3));
        assertThat(fired).isFalse();
    }

    @Test
    void recoveryUsesSynthesizedConditionAndWindow() {
        when(windowDataSource.aggregate(any(), any(), eq(WindowMode.AVG)))
                .thenReturn(new WindowDataSource.AggregateOutcome(BigDecimal.valueOf(70), 5));
        RuleExt.Recovery recovery = new RuleExt.Recovery(true, "<=", BigDecimal.valueOf(75), "PT2M");

        boolean recovered = evaluator.recovers(
                rule(">", BigDecimal.valueOf(80), recovery), fact(), spec(WindowMode.AVG, 3));
        assertThat(recovered).isTrue();
    }

    @Test
    void recoveryNoOpsWhenRecoveryDisabled() {
        RuleExt.Recovery disabled = new RuleExt.Recovery(false, "<=", BigDecimal.valueOf(75), "PT2M");
        boolean recovered = evaluator.recovers(
                rule(">", BigDecimal.valueOf(80), disabled), fact(), spec(WindowMode.AVG, 3));
        assertThat(recovered).isFalse();
    }

    @Test
    void rejectsLastModeAtThisEntrypoint() {
        // LAST is handled by RuleEvaluatorImpl directly; the windowed
        // evaluator must not be reachable for LAST. Defensive guard.
        boolean fired = evaluator.matches(rule(">", BigDecimal.valueOf(80), null), fact(),
                WindowSpec.last());
        assertThat(fired).isFalse();
    }

}
