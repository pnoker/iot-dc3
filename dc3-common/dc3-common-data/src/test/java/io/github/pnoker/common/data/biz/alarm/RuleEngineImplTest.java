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
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineImplTest {

    @Mock
    private RuleRegistry ruleRegistry;

    @Mock
    private RuleStateLookup ruleStateLookup;

    @Mock
    private RuleEvaluator ruleEvaluator;

    @InjectMocks
    private RuleEngineImpl engine;

    private static RuleBO rule(long id) {
        RuleBO bo = new RuleBO();
        bo.setId(id);
        bo.setRuleCode("rule-" + id);
        bo.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.POINT);
        RuleExt ext = new RuleExt();
        RuleExt.Content content = new RuleExt.Content();
        content.setSeverity("P1");
        content.setEventType("threshold");
        ext.setContent(content);
        bo.setRuleExt(ext);
        return bo;
    }

    private static RuleFact fact() {
        return new RuleFact(7L, AlarmTargetTypeEnum.POINT, 11L, null, LocalDateTime.now(), Map.of("value", 100));
    }

    @Test
    void returnsEmptyWhenFactIsNull() {
        assertThat(engine.evaluate(null)).isEmpty();
    }

    @Test
    void returnsEmptyWhenTenantIdMissing() {
        RuleFact fact = new RuleFact(null, AlarmTargetTypeEnum.POINT, 11L, null, LocalDateTime.now(), Map.of());
        assertThat(engine.evaluate(fact)).isEmpty();
    }

    @Test
    void emitsFiringWhenMatchesAndDoesNotConsultRuleState() {
        RuleBO rule = rule(1L);
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(rule));
        when(ruleEvaluator.matches(eq(rule), any())).thenReturn(true);

        List<RuleMatch> matches = engine.evaluate(fact());

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getMatchType()).isEqualTo(AlarmConstant.MATCH_TYPE_FIRING);
        // recovery short-circuited; ruleStateLookup must not be hit on the firing path
        verify(ruleStateLookup, never()).hasFiringState(anyLong(), anyLong(), anyByte(), anyLong());
    }

    @Test
    void emitsRecoveryOnlyWhenFiringStateExists() {
        RuleBO rule = rule(1L);
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(rule));
        when(ruleEvaluator.matches(eq(rule), any())).thenReturn(false);
        when(ruleEvaluator.recovers(eq(rule), any())).thenReturn(true);
        when(ruleStateLookup.hasFiringState(7L, 1L,
                AlarmTargetTypeEnum.POINT.getIndex(), 11L)).thenReturn(true);

        List<RuleMatch> matches = engine.evaluate(fact());

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getMatchType()).isEqualTo(AlarmConstant.MATCH_TYPE_RECOVERY);
    }

    @Test
    void suppressesRecoveryWhenNoFiringState() {
        RuleBO rule = rule(1L);
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(rule));
        when(ruleEvaluator.matches(eq(rule), any())).thenReturn(false);
        when(ruleEvaluator.recovers(eq(rule), any())).thenReturn(true);
        when(ruleStateLookup.hasFiringState(anyLong(), anyLong(), anyByte(), anyLong())).thenReturn(false);

        List<RuleMatch> matches = engine.evaluate(fact());

        // Cold-start scenario: a normal fact happens to satisfy the recovery
        // threshold, but there's nothing to recover from. Don't manufacture a
        // RECOVERED state out of thin air.
        assertThat(matches).isEmpty();
    }

    @Test
    void prefersFiringWhenBothMatchesAndRecoversAreTrue() {
        // Edge case: a malformed rule whose condition and recovery overlap. The
        // engine should treat it as a firing event, not both.
        RuleBO rule = rule(1L);
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(rule));
        when(ruleEvaluator.matches(eq(rule), any())).thenReturn(true);

        List<RuleMatch> matches = engine.evaluate(fact());

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getMatchType()).isEqualTo(AlarmConstant.MATCH_TYPE_FIRING);
        verify(ruleEvaluator, never()).recovers(eq(rule), any());
    }

    @Test
    void evaluatesAllCandidatesIndependently() {
        RuleBO ruleA = rule(1L);
        RuleBO ruleB = rule(2L);
        when(ruleRegistry.findCandidates(any())).thenReturn(List.of(ruleA, ruleB));
        when(ruleEvaluator.matches(eq(ruleA), any())).thenReturn(true);

        List<RuleMatch> matches = engine.evaluate(fact());

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getRule().getId()).isEqualTo(1L);
    }

}
