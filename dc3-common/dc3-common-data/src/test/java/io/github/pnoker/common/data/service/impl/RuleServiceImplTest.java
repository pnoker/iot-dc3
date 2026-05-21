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

import io.github.pnoker.common.data.biz.alarm.RuleRegistry;
import io.github.pnoker.common.data.dal.RuleManager;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.exception.UnSupportException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RuleServiceImplTest {

    @Mock
    private RuleBuilder ruleBuilder;

    @Mock
    private RuleManager ruleManager;

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

        RuleBO rule = new RuleBO();
        rule.setRuleName("temp-high");
        rule.setRuleCode("temp-high");
        rule.setEntityId(1L);
        rule.setTenantId(1L);
        rule.setRuleExt(ext);
        return rule;
    }

    private static RuleBO ruleWithDuration(String mode, String duration) {
        RuleBO rule = rule(mode);
        rule.getRuleExt().getContent().setWindow(new RuleExt.Window(mode, duration, 1));
        return rule;
    }

    @Test
    void rejectsAddWhenWindowModeIsUnknown() {
        // The save validator now parses the spec instead of comparing strings;
        // unknown mode values are still rejected as they map to no enum.
        RuleBO rule = rule("FOOBAR");
        assertThatThrownBy(() -> service.add(rule))
                .isInstanceOf(UnSupportException.class)
                .hasMessageContaining("FOOBAR");
        verify(ruleManager, never()).save(any());
    }

    @Test
    void rejectsAddWhenAggregationModeHasZeroDuration() {
        RuleBO rule = ruleWithDuration("AVG", "PT0S");
        assertThatThrownBy(() -> service.add(rule))
                .isInstanceOf(UnSupportException.class)
                .hasMessageContaining("positive");
        verify(ruleManager, never()).save(any());
    }

    @Test
    void rejectsAddWhenDurationIsMalformed() {
        RuleBO rule = ruleWithDuration("AVG", "5 minutes");
        assertThatThrownBy(() -> service.add(rule))
                .isInstanceOf(UnSupportException.class)
                .hasMessageContaining("ISO-8601");
        verify(ruleManager, never()).save(any());
    }

    @Test
    void allowsAddWhenWindowModeIsLast() {
        // The window-mode validator is the only behavior under test here; the
        // persistence path uses a MyBatis-Plus chain wrapper (`ruleManager.lambdaQuery()`)
        // we don't mock. We assert the *negative* — that the SUT did not throw
        // UnSupportException — and tolerate a downstream NPE coming from the
        // unstubbed query path.
        RuleBO rule = rule("LAST");
        assertThatThrownBy(() -> service.add(rule)).isNotInstanceOf(UnSupportException.class);
    }

    @Test
    void allowsAddWhenWindowIsNull() {
        RuleBO rule = rule(null);
        assertThatThrownBy(() -> service.add(rule)).isNotInstanceOf(UnSupportException.class);
    }

    @Test
    void allowsAddWhenWindowModeIsAvgWithValidDuration() {
        // Phase 4 lifts the LAST-only gate; aggregation modes with valid
        // ISO-8601 durations are accepted at save. Runtime evaluation of
        // these modes lands in a follow-up commit.
        RuleBO rule = ruleWithDuration("AVG", "PT3M");
        assertThatThrownBy(() -> service.add(rule)).isNotInstanceOf(UnSupportException.class);
    }

}
