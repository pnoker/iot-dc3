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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.property.AlarmCacheProperties;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Caches the candidate rule list for each {@code (tenantId, alarmTargetTypeFlag,
 * entityId)} tuple so rule evaluation does not hit {@code dc3_rule} on every
 * incoming fact. Without this the database QPS is linear in the point-value
 * upload rate, which dominates cost at scale.
 *
 * <p>Cached values are already-built {@link RuleBO}s, so we also avoid repeating
 * the DO -> BO conversion on every lookup.
 *
 * <p>Invalidation is driven by {@code RuleServiceImpl} on add/update/delete:
 * because rule visibility depends on tenant, target type, and entity id, the
 * registry exposes both per-tenant and global flush. The configured TTL is a
 * safety net — ordinary correctness comes from the explicit invalidations.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Slf4j
@Component
public class RuleRegistry {

    private final RuleCandidateLookup ruleCandidateLookup;

    private final RuleBuilder ruleBuilder;

    private final Cache<RuleCacheKey, List<RuleBO>> cache;

    public RuleRegistry(RuleCandidateLookup ruleCandidateLookup, RuleBuilder ruleBuilder,
                        AlarmCacheProperties alarmCacheProperties) {
        this.ruleCandidateLookup = ruleCandidateLookup;
        this.ruleBuilder = ruleBuilder;
        AlarmCacheProperties.CacheTuning tuning = alarmCacheProperties.getRule();
        this.cache = Caffeine.newBuilder()
                .maximumSize(tuning.getMaxSize())
                .expireAfterWrite(Duration.ofSeconds(tuning.getTtlSeconds()))
                .build();
    }

    /**
     * Returns the candidate rule list for {@code fact}. Subsequent calls within
     * the configured TTL window return the cached list directly; the database
     * is only hit on cache miss.
     */
    public List<RuleBO> findCandidates(RuleFact fact) {
        if (Objects.isNull(fact) || Objects.isNull(fact.getTenantId())
                || Objects.isNull(fact.getAlarmTargetTypeFlag()) || Objects.isNull(fact.getEntityId())) {
            return List.of();
        }
        RuleCacheKey key = new RuleCacheKey(
                fact.getTenantId(),
                fact.getAlarmTargetTypeFlag(),
                fact.getEntityId());
        return cache.get(key, k -> ruleBuilder.buildBOListByDOList(ruleCandidateLookup.findCandidates(fact)));
    }

    /**
     * Drops every cached entry for the given tenant. Invoked by rule
     * add/update/delete to ensure the next evaluation reflects the change. The
     * granularity is intentionally tenant-wide — finer eviction would have to
     * traverse the cache anyway, and rule mutations are rare enough that the
     * cost is negligible.
     */
    public void invalidateTenant(Long tenantId) {
        if (Objects.isNull(tenantId)) {
            return;
        }
        cache.asMap().keySet().removeIf(key -> Objects.equals(key.tenantId(), tenantId));
    }

    /**
     * Drops every cached entry. Used by tests and (potentially) by an admin
     * endpoint that wants to force-refresh the engine.
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * Cache key — combines the three dimensions the lookup actually filters on.
     * Recorded as a record so equals/hashCode follow value semantics.
     */
    public record RuleCacheKey(Long tenantId, AlarmTargetTypeEnum targetType, Long entityId) {
    }

}
