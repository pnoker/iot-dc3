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

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.github.pnoker.common.data.dal.RuleStateManager;
import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.RuleStateFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleStateLookupTest {

    private static final long TENANT_ID = 7L;
    private static final long RULE_ID = 1L;
    private static final byte TARGET_TYPE = AlarmTargetTypeFlagEnum.POINT.getIndex();
    private static final long ENTITY_ID = 11L;
    @Mock
    private RuleStateManager ruleStateManager;
    @Mock
    private LambdaQueryChainWrapper<RuleStateDO> chainWrapper;
    @InjectMocks
    private RuleStateLookup lookup;

    // ---------- hasFiringState ----------

    @Test
    void hasFiringStateReturnsTrueWhenRowExists() {
        when(ruleStateManager.lambdaQuery()).thenReturn(chainWrapper);
        when(chainWrapper.eq(any(), any())).thenReturn(chainWrapper);
        when(chainWrapper.exists()).thenReturn(true);

        assertThat(lookup.hasFiringState(TENANT_ID, RULE_ID, TARGET_TYPE, ENTITY_ID)).isTrue();

        verify(chainWrapper).eq(any(), eq(TENANT_ID));
        verify(chainWrapper).eq(any(), eq(RULE_ID));
        verify(chainWrapper).eq(any(), eq(TARGET_TYPE));
        verify(chainWrapper).eq(any(), eq(ENTITY_ID));
        verify(chainWrapper).eq(any(), eq(RuleStateFlagEnum.FIRING.getIndex()));
    }

    @Test
    void hasFiringStateReturnsFalseWhenNoRow() {
        when(ruleStateManager.lambdaQuery()).thenReturn(chainWrapper);
        when(chainWrapper.eq(any(), any())).thenReturn(chainWrapper);
        when(chainWrapper.exists()).thenReturn(false);

        assertThat(lookup.hasFiringState(TENANT_ID, RULE_ID, TARGET_TYPE, ENTITY_ID)).isFalse();
    }

    // ---------- getFiringAlarmId ----------

    @Test
    void getFiringAlarmIdReturnsAlarmIdWhenFound() {
        RuleStateDO state = new RuleStateDO();
        state.setId(99L);
        state.setAlarmId(100L);

        when(ruleStateManager.lambdaQuery()).thenReturn(chainWrapper);
        when(chainWrapper.eq(any(), any())).thenReturn(chainWrapper);
        when(chainWrapper.gt(any(), any())).thenReturn(chainWrapper);
        when(chainWrapper.orderByDesc((com.baomidou.mybatisplus.core.toolkit.support.SFunction<RuleStateDO, ?>) any())).thenReturn(chainWrapper);
        when(chainWrapper.last(anyString())).thenReturn(chainWrapper);
        when(chainWrapper.one()).thenReturn(state);

        assertThat(lookup.getFiringAlarmId(TENANT_ID, RULE_ID, TARGET_TYPE, ENTITY_ID)).isEqualTo(100L);

        verify(chainWrapper).gt(any(), eq(0L));
        verify(chainWrapper).orderByDesc((com.baomidou.mybatisplus.core.toolkit.support.SFunction<RuleStateDO, ?>) any());
        verify(chainWrapper).last("limit 1");
    }

    @Test
    void getFiringAlarmIdReturnsNullWhenNotFound() {
        when(ruleStateManager.lambdaQuery()).thenReturn(chainWrapper);
        when(chainWrapper.eq(any(), any())).thenReturn(chainWrapper);
        when(chainWrapper.gt(any(), any())).thenReturn(chainWrapper);
        when(chainWrapper.orderByDesc((com.baomidou.mybatisplus.core.toolkit.support.SFunction<RuleStateDO, ?>) any())).thenReturn(chainWrapper);
        when(chainWrapper.last(anyString())).thenReturn(chainWrapper);
        when(chainWrapper.one()).thenReturn(null);

        assertThat(lookup.getFiringAlarmId(TENANT_ID, RULE_ID, TARGET_TYPE, ENTITY_ID)).isNull();
    }

    @Test
    void getFiringAlarmIdReturnsNullWhenStateHasNullAlarmId() {
        RuleStateDO state = new RuleStateDO();
        state.setId(99L);
        state.setAlarmId(null);

        when(ruleStateManager.lambdaQuery()).thenReturn(chainWrapper);
        when(chainWrapper.eq(any(), any())).thenReturn(chainWrapper);
        when(chainWrapper.gt(any(), any())).thenReturn(chainWrapper);
        when(chainWrapper.orderByDesc((com.baomidou.mybatisplus.core.toolkit.support.SFunction<RuleStateDO, ?>) any())).thenReturn(chainWrapper);
        when(chainWrapper.last(anyString())).thenReturn(chainWrapper);
        when(chainWrapper.one()).thenReturn(state);

        assertThat(lookup.getFiringAlarmId(TENANT_ID, RULE_ID, TARGET_TYPE, ENTITY_ID)).isNull();
    }
}
