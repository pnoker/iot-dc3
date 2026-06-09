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

import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmRulePipelineServiceImplTest {

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private RuleNotificationService ruleNotificationService;

    @Mock
    private RuleAlarmPersistenceService ruleAlarmPersistenceService;

    @InjectMocks
    private AlarmRulePipelineServiceImpl service;

    // ---------- fixtures ----------

    private static RuleFact fact(Long tenantId, AlarmTargetTypeEnum targetType, Long entityId) {
        return new RuleFact(tenantId, targetType, entityId, null,
                LocalDateTime.of(2026, 5, 21, 12, 0), Map.of("value", 100));
    }

    private static RuleMatch match(RuleFact fact) {
        RuleBO rule = new RuleBO();
        rule.setId(1L);
        return RuleMatch.firing(rule, fact);
    }

    private static NotifyHistoryBO history(long id) {
        NotifyHistoryBO bo = new NotifyHistoryBO();
        bo.setId(id);
        return bo;
    }

    // ---------- process ----------

    @Test
    void processEvaluatesRuleEngineAndNotifies() {
        RuleFact fact = fact(7L, AlarmTargetTypeEnum.POINT, 11L);
        RuleMatch m = match(fact);
        when(ruleEngine.evaluate(fact)).thenReturn(List.of(m));
        when(ruleNotificationService.notify(m)).thenReturn(List.of(history(1L)));

        List<NotifyHistoryBO> result = service.process(fact);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(ruleAlarmPersistenceService).ensureAlarm(m);
    }

    @Test
    void processReturnsEmptyWhenNoRulesMatch() {
        RuleFact fact = fact(7L, AlarmTargetTypeEnum.POINT, 11L);
        when(ruleEngine.evaluate(fact)).thenReturn(List.of());

        List<NotifyHistoryBO> result = service.process(fact);

        assertThat(result).isEmpty();
        verify(ruleAlarmPersistenceService, never()).ensureAlarm(any());
        verify(ruleNotificationService, never()).notify(any());
    }

    // ---------- processBatch: empty / invalid ----------

    @Test
    void processBatchReturnsEmptyForNullAndEmptyInput() {
        assertThat(service.processBatch(null)).isEmpty();
        assertThat(service.processBatch(List.of())).isEmpty();
        verifyNoPipelineInteraction();
    }

    @Test
    void processBatchFiltersNullFactsAndFactsWithNullTenantIdOrTargetType() {
        List<RuleFact> facts = Arrays.asList(
                null,
                fact(7L, (AlarmTargetTypeEnum) null, 11L), // null target type
                fact(null, AlarmTargetTypeEnum.POINT, 11L), // null tenantId
                fact(7L, AlarmTargetTypeEnum.POINT, 11L));  // valid

        RuleMatch m = match(fact(7L, AlarmTargetTypeEnum.POINT, 11L));
        when(ruleEngine.evaluate(any())).thenReturn(List.of(m));
        when(ruleNotificationService.notifyBatch(anyList())).thenReturn(List.of(history(1L)));

        List<NotifyHistoryBO> result = service.processBatch(facts);

        assertThat(result).hasSize(1);
        // Only 1 evaluation for the single valid fact
        verify(ruleEngine, times(1)).evaluate(any());
    }

    // ---------- processBatch: grouping ----------

    @Test
    void processBatchGroupsFactsByRuleCacheKeyAndCallsNotifyBatchOnce() {
        // 3 facts: two share the same group, one is in a different group
        RuleFact f1 = fact(7L, AlarmTargetTypeEnum.POINT, 11L);
        RuleFact f2 = fact(7L, AlarmTargetTypeEnum.POINT, 11L); // same group as f1
        RuleFact f3 = fact(7L, AlarmTargetTypeEnum.POINT, 22L); // different entity

        RuleMatch m1 = match(f1);
        RuleMatch m2 = match(f2);
        RuleMatch m3 = match(f3);

        when(ruleEngine.evaluate(f1)).thenReturn(List.of(m1));
        when(ruleEngine.evaluate(f2)).thenReturn(List.of(m2));
        when(ruleEngine.evaluate(f3)).thenReturn(List.of(m3));
        when(ruleNotificationService.notifyBatch(anyList())).thenReturn(List.of(history(1L), history(2L), history(3L)));

        List<NotifyHistoryBO> result = service.processBatch(List.of(f1, f2, f3));

        assertThat(result).hasSize(3);
        verify(ruleEngine, times(3)).evaluate(any());
        verify(ruleAlarmPersistenceService, times(3)).ensureAlarm(any());
        // Single notifyBatch call amortizes writes
        verify(ruleNotificationService).notifyBatch(anyList());
        verify(ruleNotificationService, never()).notify(any());
    }

    @Test
    void processBatchReturnsEmptyWhenNoMatchesProduced() {
        RuleFact fact = fact(7L, AlarmTargetTypeEnum.POINT, 11L);
        when(ruleEngine.evaluate(fact)).thenReturn(List.of());

        List<NotifyHistoryBO> result = service.processBatch(List.of(fact));

        assertThat(result).isEmpty();
        verify(ruleAlarmPersistenceService, never()).ensureAlarm(any());
        verify(ruleNotificationService, never()).notifyBatch(any());
    }

    // ---------- helpers ----------

    private void verifyNoPipelineInteraction() {
        verify(ruleEngine, never()).evaluate(any());
        verify(ruleAlarmPersistenceService, never()).ensureAlarm(any());
        verify(ruleNotificationService, never()).notify(any());
        verify(ruleNotificationService, never()).notifyBatch(any());
    }
}
