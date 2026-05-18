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
import io.github.pnoker.common.data.dal.DeviceEventManager;
import io.github.pnoker.common.data.dal.DriverEventManager;
import io.github.pnoker.common.data.dal.RuleStateManager;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.model.DeviceEventDO;
import io.github.pnoker.common.data.entity.model.DriverEventDO;
import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.entity.ext.RuleAlarmEventExt;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import io.github.pnoker.common.enums.RuleStateFlagEnum;
import io.github.pnoker.common.utils.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmEventRecordServiceImplTest {

    @Mock
    private DeviceEventManager deviceEventManager;

    @Mock
    private DriverEventManager driverEventManager;

    @Mock
    private RuleStateManager ruleStateManager;

    @InjectMocks
    private AlarmEventRecordServiceImpl service;

    @Test
    void pointRuleMatchPersistsDeviceEventAndBackfillsFactEventId() {
        doAnswer(invocation -> {
            DeviceEventDO event = invocation.getArgument(0);
            event.setId(101L);
            return true;
        }).when(deviceEventManager).save(any(DeviceEventDO.class));

        RuleMatch match = match(AlarmTargetTypeFlagEnum.POINT,
                Map.of("deviceId", 3L, "numValue", BigDecimal.valueOf(90)));

        service.ensureEvent(match);

        ArgumentCaptor<DeviceEventDO> captor = ArgumentCaptor.forClass(DeviceEventDO.class);
        verify(deviceEventManager).save(captor.capture());
        DeviceEventDO event = captor.getValue();
        assertThat(event.getDeviceId()).isEqualTo(3L);
        assertThat(event.getPointId()).isEqualTo(4L);
        assertThat(event.getEventTypeFlag()).isEqualTo(DeviceEventTypeEnum.ALARM.getIndex());
        assertThat(event.getEventExt().getType()).isEqualTo(AlarmConstant.EXT_RULE_EVENT);
        RuleAlarmEventExt.Content detail = JsonUtil.parseObject(event.getEventExt().getRemark(),
                RuleAlarmEventExt.Content.class);
        assertThat(detail.getRuleCode()).isEqualTo("temperature-high");
        assertThat(detail.getTargetType()).isEqualTo(AlarmTargetTypeFlagEnum.POINT);
        assertThat(detail.getValues()).containsEntry("deviceId", 3);
        assertThat(match.getFact().getEventId()).isEqualTo(101L);
    }

    @Test
    void driverRuleMatchPersistsDriverEvent() {
        doAnswer(invocation -> {
            DriverEventDO event = invocation.getArgument(0);
            event.setId(202L);
            return true;
        }).when(driverEventManager).save(any(DriverEventDO.class));

        RuleMatch match = match(AlarmTargetTypeFlagEnum.DRIVER, Map.of("status", "offline"));

        service.ensureEvent(match);

        ArgumentCaptor<DriverEventDO> captor = ArgumentCaptor.forClass(DriverEventDO.class);
        verify(driverEventManager).save(captor.capture());
        assertThat(captor.getValue().getDriverId()).isEqualTo(4L);
        assertThat(captor.getValue().getEventTypeFlag()).isEqualTo(DriverEventTypeEnum.ALARM.getIndex());
        assertThat(match.getFact().getEventId()).isEqualTo(202L);
    }

    @Test
    void existingEventIdIsReused() {
        RuleMatch match = match(AlarmTargetTypeFlagEnum.DEVICE, Map.of("status", "offline"));
        match.getFact().setEventId(99L);

        service.ensureEvent(match);

        verify(deviceEventManager, never()).save(any(DeviceEventDO.class));
        verify(driverEventManager, never()).save(any(DriverEventDO.class));
        assertThat(match.getFact().getEventId()).isEqualTo(99L);
    }

    @Test
    void activeRuleStateEventIdIsReused() {
        RuleStateDO state = new RuleStateDO();
        state.setStateFlag(RuleStateFlagEnum.FIRING.getIndex());
        state.setEventId(303L);
        when(ruleStateManager.getOne(any())).thenReturn(state);
        RuleMatch match = match(AlarmTargetTypeFlagEnum.DEVICE, Map.of("status", "offline"));

        service.ensureEvent(match);

        verify(deviceEventManager, never()).save(any(DeviceEventDO.class));
        verify(driverEventManager, never()).save(any(DriverEventDO.class));
        assertThat(match.getFact().getEventId()).isEqualTo(303L);
    }

    @Test
    void recoveryMatchDoesNotCreateNewEvent() {
        RuleMatch match = match(AlarmTargetTypeFlagEnum.POINT, Map.of("deviceId", 3L));
        match.setMatchType(AlarmConstant.MATCH_TYPE_RECOVERY);

        service.ensureEvent(match);

        verify(deviceEventManager, never()).save(any(DeviceEventDO.class));
    }

    @Test
    void recoveryMatchReusesActiveRuleStateEventId() {
        RuleStateDO state = new RuleStateDO();
        state.setStateFlag(RuleStateFlagEnum.FIRING.getIndex());
        state.setEventId(404L);
        when(ruleStateManager.getOne(any())).thenReturn(state);
        RuleMatch match = match(AlarmTargetTypeFlagEnum.POINT, Map.of("deviceId", 3L));
        match.setMatchType(AlarmConstant.MATCH_TYPE_RECOVERY);

        service.ensureEvent(match);

        verify(deviceEventManager, never()).save(any(DeviceEventDO.class));
        assertThat(match.getFact().getEventId()).isEqualTo(404L);
    }

    private RuleMatch match(AlarmTargetTypeFlagEnum targetType, Map<String, Object> values) {
        RuleBO rule = new RuleBO();
        rule.setId(1L);
        rule.setRuleName("Temperature High");
        rule.setRuleCode("temperature-high");
        rule.setAlarmTargetTypeFlag(targetType);
        RuleExt.Content content = new RuleExt.Content(null, null, null, "P1", "temperature_high", List.of());
        RuleExt ext = new RuleExt(content);
        rule.setRuleExt(ext);

        RuleFact fact = new RuleFact(1L, targetType, 4L, null, LocalDateTime.now(), values);
        return RuleMatch.firing(rule, fact);
    }

}
