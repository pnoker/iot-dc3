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
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleAlarmPersistenceServiceImplTest {

    @Mock
    private EntityAlarmManager entityAlarmManager;

    @Mock
    private RuleStateLookup ruleStateLookup;

    @InjectMocks
    private RuleAlarmPersistenceServiceImpl service;

    private static RuleBO rule(long id, String severity) {
        RuleBO bo = new RuleBO();
        bo.setId(id);
        bo.setRuleCode("rule-" + id);
        bo.setRuleName("rule-" + id);
        bo.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.POINT);
        RuleExt ext = new RuleExt();
        RuleExt.Content content = new RuleExt.Content();
        content.setSeverity(severity);
        content.setEventType("threshold");
        content.setLabels(List.of());
        ext.setContent(content);
        bo.setRuleExt(ext);
        return bo;
    }

    private static RuleMatch firingMatch(String severity) {
        RuleFact fact = new RuleFact(7L, AlarmTargetTypeFlagEnum.POINT, 11L, null, LocalDateTime.now(),
                Map.of("driverId", 3L, "deviceId", 5L));
        return RuleMatch.firing(rule(1L, severity), fact);
    }

    private static RuleMatch recoveryMatch() {
        RuleFact fact = new RuleFact(7L, AlarmTargetTypeFlagEnum.POINT, 11L, null, LocalDateTime.now(), Map.of());
        return RuleMatch.recovery(rule(1L, "P1"), fact);
    }

    @Test
    void persistsEntityAlarmWithSeverityFromRuleExt() {
        RuleMatch match = firingMatch("P0");
        when(ruleStateLookup.findFiringAlarmId(anyLong(), anyLong(), anyByte(), anyLong())).thenReturn(null);
        when(entityAlarmManager.save(any(EntityAlarmDO.class))).thenAnswer(inv -> {
            EntityAlarmDO d = inv.getArgument(0);
            d.setId(42L);
            return true;
        });

        service.ensureAlarm(match);

        ArgumentCaptor<EntityAlarmDO> captor = ArgumentCaptor.forClass(EntityAlarmDO.class);
        verify(entityAlarmManager).save(captor.capture());
        EntityAlarmDO persisted = captor.getValue();
        // Severity P0 → alarm_level_flag index 0
        assertThat(persisted.getAlarmLevelFlag()).isEqualTo(AlarmMessageLevelFlagEnum.P0.getIndex());
        assertThat(persisted.getRuleId()).isEqualTo(1L);
        assertThat(persisted.getTenantId()).isEqualTo(7L);
        // The fact gets the new alarm id back so downstream notification persists it
        assertThat(match.getFact().getAlarmId()).isEqualTo(42L);
    }

    @Test
    void defaultsToP2WhenSeverityIsBlank() {
        RuleMatch match = firingMatch(null);
        when(ruleStateLookup.findFiringAlarmId(anyLong(), anyLong(), anyByte(), anyLong())).thenReturn(null);
        when(entityAlarmManager.save(any(EntityAlarmDO.class))).thenAnswer(inv -> {
            EntityAlarmDO d = inv.getArgument(0);
            d.setId(99L);
            return true;
        });

        service.ensureAlarm(match);

        ArgumentCaptor<EntityAlarmDO> captor = ArgumentCaptor.forClass(EntityAlarmDO.class);
        verify(entityAlarmManager).save(captor.capture());
        assertThat(captor.getValue().getAlarmLevelFlag()).isEqualTo(AlarmMessageLevelFlagEnum.P2.getIndex());
    }

    @Test
    void reusesExistingFiringAlarmIdAndSkipsInsert() {
        RuleMatch match = firingMatch("P1");
        when(ruleStateLookup.findFiringAlarmId(anyLong(), anyLong(), anyByte(), anyLong())).thenReturn(101L);

        service.ensureAlarm(match);

        // No new entity alarm is written when a firing one already exists; the
        // fact's alarmId is just rebound to the existing alarm.
        verify(entityAlarmManager, never()).save(any());
        assertThat(match.getFact().getAlarmId()).isEqualTo(101L);
    }

    @Test
    void recoveryMatchWithoutFiringStateProducesNoEntityAlarmRow() {
        // Defense in depth: even if the engine slipped a recovery match through,
        // ensureAlarm should not create a new EntityAlarm row for a RECOVERY match
        // when no firing alarm exists to recover from.
        RuleMatch match = recoveryMatch();
        when(ruleStateLookup.findFiringAlarmId(anyLong(), anyLong(), anyByte(), anyLong())).thenReturn(null);

        service.ensureAlarm(match);

        verify(entityAlarmManager, never()).save(any());
        assertThat(match.getFact().getAlarmId()).isNull();
        assertThat(match.getMatchType()).isEqualTo(AlarmConstant.MATCH_TYPE_RECOVERY);
    }

    @Test
    void skipsWhenFactAlreadyHasAlarmId() {
        RuleMatch match = firingMatch("P1");
        match.getFact().setAlarmId(55L);

        service.ensureAlarm(match);

        verify(entityAlarmManager, never()).save(any());
        verify(ruleStateLookup, never()).findFiringAlarmId(anyLong(), anyLong(), anyByte(), anyLong());
    }

}
