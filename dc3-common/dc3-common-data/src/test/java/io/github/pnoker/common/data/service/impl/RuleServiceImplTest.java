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
package io.github.pnoker.common.data.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.biz.alarm.RuleRegistry;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.data.repository.ReactiveRuleStore;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.exception.UnSupportException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RuleServiceImplTest {

    @Mock
    private RuleBuilder ruleBuilder;

    @Mock
    private ReactiveRuleStore ruleStore;

    @Mock
    private RuleRegistry ruleRegistry;

    @InjectMocks
    private RuleServiceImpl service;

    private static RuleBO rule(String windowMode) {
        RuleExt.Content content = new RuleExt.Content(
                new RuleExt.Condition("numValue", ">", null, BigDecimal.valueOf(80), null, null, "C"),
                windowMode == null ? null : new RuleExt.Window(windowMode, "PT3M", 1),
                null,
                "P1",
                "ALARM",
                List.of("temperature"));
        RuleExt ext = new RuleExt(content);
        ext.setType("POINT_VALUE_RULE");
        ext.setVersion(1);
        RuleBO value = new RuleBO();
        value.setRuleName("temp-high");
        value.setRuleCode("temp-high");
        value.setEntityId(1L);
        value.setTenantId(1L);
        value.setRuleExt(ext);
        return value;
    }

    private static RuleBO ruleWithDuration(String mode, String duration) {
        RuleBO value = rule(mode);
        value.getRuleExt().getContent().setWindow(new RuleExt.Window(mode, duration, 1));
        return value;
    }

    @Test
    void rejectsAddWhenWindowModeIsUnknown() {
        RuleBO value = rule("FOOBAR");
        assertThatThrownBy(() -> service.add(value).block())
                .isInstanceOf(UnSupportException.class)
                .hasMessageContaining("FOOBAR");
        verify(ruleStore, never()).insert(any());
    }

    @Test
    void rejectsAddWhenAggregationWindowHasZeroDuration() {
        RuleBO value = ruleWithDuration("AVG", "PT0S");
        assertThatThrownBy(() -> service.add(value).block())
                .isInstanceOf(UnSupportException.class)
                .hasMessageContaining("positive");
        verify(ruleStore, never()).insert(any());
    }

    @Test
    void rejectsAddWhenWindowDurationIsMalformed() {
        RuleBO value = ruleWithDuration("AVG", "5 minutes");
        assertThatThrownBy(() -> service.add(value).block())
                .isInstanceOf(UnSupportException.class)
                .hasMessageContaining("ISO-8601");
        verify(ruleStore, never()).insert(any());
    }

    @Test
    void acceptsValidWindowAfterReactivePersistenceCheck() {
        RuleBO value = rule("AVG");
        RuleDO stored = new RuleDO();
        stored.setTenantId(1L);
        stored.setRuleCode("temp-high");
        stored.setId(1L);
        when(ruleBuilder.buildDOByBO(value)).thenReturn(stored);
        when(ruleStore.existsActiveCode(1L, "temp-high", null)).thenReturn(Mono.just(false));
        when(ruleStore.insert(stored)).thenReturn(Mono.just(stored));
        when(ruleBuilder.buildBOByDO(stored)).thenReturn(value);
        service.add(value).block();
        verify(ruleStore).insert(stored);
    }
}
