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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.constant.service.AlarmConstant;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.repository.ReactiveRuleStateLookup;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RuleEngineImplTest {
    @Mock
    private RuleRegistry ruleRegistry;

    @Mock
    private ReactiveRuleStateLookup ruleStateLookup;

    @Mock
    private RuleEvaluator ruleEvaluator;

    @InjectMocks
    private RuleEngineImpl engine;

    @Test
    void returnsEmptyWhenFactIsNull() {
        assertThat(engine.evaluate(null).collectList().block()).isEmpty();
    }

    @Test
    void returnsEmptyWhenTenantIdMissing() {
        assertThat(engine.evaluate(
                                new RuleFact(null, AlarmTargetTypeEnum.POINT, 11L, null, LocalDateTime.now(), Map.of()))
                        .collectList()
                        .block())
                .isEmpty();
    }

    @Test
    void emitsFiringWithoutStateLookup() {
        RuleBO rule = rule(1L);
        when(ruleRegistry.findCandidates(any())).thenReturn(Mono.just(List.of(rule)));
        when(ruleEvaluator.matches(eq(rule), any())).thenReturn(Mono.just(true));
        List<RuleMatch> matches = engine.evaluate(fact()).collectList().block();
        assertThat(matches)
                .singleElement()
                .extracting(RuleMatch::getMatchType)
                .isEqualTo(AlarmConstant.MATCH_TYPE_FIRING);
        verify(ruleStateLookup, never()).hasFiringState(anyLong(), anyLong(), anyByte(), anyLong());
    }

    @Test
    void emitsRecoveryOnlyWhenStateExists() {
        RuleBO rule = rule(1L);
        when(ruleRegistry.findCandidates(any())).thenReturn(Mono.just(List.of(rule)));
        when(ruleEvaluator.matches(eq(rule), any())).thenReturn(Mono.just(false));
        when(ruleEvaluator.recovers(eq(rule), any())).thenReturn(Mono.just(true));
        when(ruleStateLookup.hasFiringState(anyLong(), anyLong(), anyByte(), anyLong()))
                .thenReturn(Mono.just(true));
        assertThat(engine.evaluate(fact()).collectList().block())
                .singleElement()
                .extracting(RuleMatch::getMatchType)
                .isEqualTo(AlarmConstant.MATCH_TYPE_RECOVERY);
    }

    @Test
    void suppressesRecoveryWithoutState() {
        RuleBO rule = rule(1L);
        when(ruleRegistry.findCandidates(any())).thenReturn(Mono.just(List.of(rule)));
        when(ruleEvaluator.matches(eq(rule), any())).thenReturn(Mono.just(false));
        when(ruleEvaluator.recovers(eq(rule), any())).thenReturn(Mono.just(true));
        when(ruleStateLookup.hasFiringState(anyLong(), anyLong(), anyByte(), anyLong()))
                .thenReturn(Mono.just(false));
        assertThat(engine.evaluate(fact()).collectList().block()).isEmpty();
    }

    @Test
    void firingTakesPrecedenceOverRecovery() {
        RuleBO rule = rule(1L);
        when(ruleRegistry.findCandidates(any())).thenReturn(Mono.just(List.of(rule)));
        when(ruleEvaluator.matches(eq(rule), any())).thenReturn(Mono.just(true));
        assertThat(engine.evaluate(fact()).collectList().block())
                .singleElement()
                .extracting(RuleMatch::getMatchType)
                .isEqualTo(AlarmConstant.MATCH_TYPE_FIRING);
        verify(ruleEvaluator, never()).recovers(eq(rule), any());
    }

    private static RuleBO rule(long id) {
        RuleBO value = new RuleBO();
        value.setId(id);
        value.setRuleCode("rule-" + id);
        value.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.POINT);
        RuleExt ext = new RuleExt();
        RuleExt.Content content = new RuleExt.Content();
        content.setSeverity("P1");
        content.setEventType("threshold");
        ext.setContent(content);
        value.setRuleExt(ext);
        return value;
    }

    private static RuleFact fact() {
        return new RuleFact(7L, AlarmTargetTypeEnum.POINT, 11L, null, LocalDateTime.now(), Map.of("value", 100));
    }
}
