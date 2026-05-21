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
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.data.entity.property.AlarmCacheProperties;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleRegistryTest {

    @Mock
    private RuleCandidateLookup ruleCandidateLookup;

    @Mock
    private RuleBuilder ruleBuilder;

    private RuleRegistry registry;

    @BeforeEach
    void setUp() {
        AlarmCacheProperties props = new AlarmCacheProperties();
        registry = new RuleRegistry(ruleCandidateLookup, ruleBuilder, props);
    }

    private static RuleFact fact(long tenantId, long entityId) {
        return new RuleFact(tenantId, AlarmTargetTypeFlagEnum.POINT, entityId, null, LocalDateTime.now(), Map.of());
    }

    private static RuleBO rule(long id) {
        RuleBO bo = new RuleBO();
        bo.setId(id);
        return bo;
    }

    @Test
    void cachesAcrossRepeatedLookups() {
        when(ruleCandidateLookup.findCandidates(any())).thenReturn(List.of(new RuleDO()));
        when(ruleBuilder.buildBOListByDOList(any())).thenReturn(List.of(rule(1L)));

        // Same fact twice → only one DB lookup; the second call is a cache hit.
        registry.findCandidates(fact(7L, 11L));
        registry.findCandidates(fact(7L, 11L));

        verify(ruleCandidateLookup, times(1)).findCandidates(any());
        verify(ruleBuilder, times(1)).buildBOListByDOList(any());
    }

    @Test
    void distinctEntitiesGetSeparateCacheEntries() {
        when(ruleCandidateLookup.findCandidates(any())).thenReturn(List.of(new RuleDO()));
        when(ruleBuilder.buildBOListByDOList(any())).thenReturn(List.of(rule(1L)));

        registry.findCandidates(fact(7L, 11L));
        registry.findCandidates(fact(7L, 12L));

        verify(ruleCandidateLookup, times(2)).findCandidates(any());
    }

    @Test
    void invalidateTenantDropsOnlyMatchingTenant() {
        when(ruleCandidateLookup.findCandidates(any())).thenReturn(List.of(new RuleDO()));
        when(ruleBuilder.buildBOListByDOList(any())).thenReturn(List.of(rule(1L)));

        registry.findCandidates(fact(7L, 11L));
        registry.findCandidates(fact(8L, 11L));

        registry.invalidateTenant(7L);

        // tenant 7 should re-load on the next lookup; tenant 8 stays cached.
        registry.findCandidates(fact(7L, 11L));
        registry.findCandidates(fact(8L, 11L));

        verify(ruleCandidateLookup, times(3)).findCandidates(any()); // 2 misses + 1 reload after invalidate
    }

    @Test
    void invalidateAllDropsEverything() {
        when(ruleCandidateLookup.findCandidates(any())).thenReturn(List.of(new RuleDO()));
        when(ruleBuilder.buildBOListByDOList(any())).thenReturn(List.of(rule(1L)));

        registry.findCandidates(fact(7L, 11L));
        registry.invalidateAll();
        registry.findCandidates(fact(7L, 11L));

        verify(ruleCandidateLookup, times(2)).findCandidates(any());
    }

    @Test
    void returnsEmptyForIncompleteFact() {
        // A fact with null tenantId / entityId should not be cached, and must
        // not trip the underlying lookup either — the engine guards against
        // bad input upstream.
        assertThat(registry.findCandidates(null)).isEmpty();
        assertThat(registry.findCandidates(new RuleFact(null, AlarmTargetTypeFlagEnum.POINT, 11L,
                null, LocalDateTime.now(), Map.of()))).isEmpty();
        verify(ruleCandidateLookup, times(0)).findCandidates(any());
    }

}
