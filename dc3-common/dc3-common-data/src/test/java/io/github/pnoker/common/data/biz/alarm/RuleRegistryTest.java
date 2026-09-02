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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.property.AlarmCacheProperties;
import io.github.pnoker.common.data.repository.ReactiveRuleStore;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleRegistryTest {

    @Mock
    private ReactiveRuleStore ruleStore;

    private RuleRegistry registry;

    private static RuleFact fact(long tenantId, long entityId) {
        return new RuleFact(tenantId, AlarmTargetTypeEnum.POINT, entityId, null, LocalDateTime.now(), Map.of());
    }

    private static RuleBO rule(long id) {
        RuleBO bo = new RuleBO();
        bo.setId(id);
        return bo;
    }

    @BeforeEach
    void setUp() {
        AlarmCacheProperties props = new AlarmCacheProperties();
        registry = new RuleRegistry(ruleStore, props);
    }

    @Test
    void cachesAcrossRepeatedLookups() {
        when(ruleStore.listEnabledCandidates(anyLong(), any(), anyLong()))
                .thenReturn(reactor.core.publisher.Flux.just(rule(1L)));

        // Same fact twice → only one DB lookup; the second call is a cache hit.
        registry.findCandidates(fact(7L, 11L)).block();
        registry.findCandidates(fact(7L, 11L)).block();

        verify(ruleStore, times(1)).listEnabledCandidates(anyLong(), any(), anyLong());
    }

    @Test
    void distinctEntitiesGetSeparateCacheEntries() {
        when(ruleStore.listEnabledCandidates(anyLong(), any(), anyLong()))
                .thenReturn(reactor.core.publisher.Flux.just(rule(1L)));

        registry.findCandidates(fact(7L, 11L)).block();
        registry.findCandidates(fact(7L, 12L)).block();

        verify(ruleStore, times(2)).listEnabledCandidates(anyLong(), any(), anyLong());
    }

    @Test
    void invalidateTenantDropsOnlyMatchingTenant() {
        when(ruleStore.listEnabledCandidates(anyLong(), any(), anyLong()))
                .thenReturn(reactor.core.publisher.Flux.just(rule(1L)));

        registry.findCandidates(fact(7L, 11L)).block();
        registry.findCandidates(fact(8L, 11L)).block();

        registry.invalidateTenant(7L);

        // tenant 7 should re-load on the next lookup; tenant 8 stays cached.
        registry.findCandidates(fact(7L, 11L)).block();
        registry.findCandidates(fact(8L, 11L)).block();

        verify(ruleStore, times(3)).listEnabledCandidates(anyLong(), any(), anyLong());
    }

    @Test
    void invalidateAllDropsEverything() {
        when(ruleStore.listEnabledCandidates(anyLong(), any(), anyLong()))
                .thenReturn(reactor.core.publisher.Flux.just(rule(1L)));

        registry.findCandidates(fact(7L, 11L)).block();
        registry.invalidateAll();
        registry.findCandidates(fact(7L, 11L)).block();

        verify(ruleStore, times(2)).listEnabledCandidates(anyLong(), any(), anyLong());
    }

    @Test
    void returnsEmptyForIncompleteFact() {
        // A fact with null tenantId / entityId should not be cached, and must
        // not trip the underlying lookup either — the engine guards against
        // bad input upstream.
        assertThat(registry.findCandidates(null).block()).isEmpty();
        assertThat(registry.findCandidates(
                                new RuleFact(null, AlarmTargetTypeEnum.POINT, 11L, null, LocalDateTime.now(), Map.of()))
                        .block())
                .isEmpty();
        verify(ruleStore, times(0)).listEnabledCandidates(anyLong(), any(), anyLong());
    }
}
