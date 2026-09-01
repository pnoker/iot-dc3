package io.github.pnoker.common.data.biz.alarm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.property.AlarmCacheProperties;
import io.github.pnoker.common.data.repository.ReactiveRuleStore;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Tenant-scoped reactive cache for runtime rule candidates. */
@Slf4j
@Component
public class RuleRegistry {
    private final ReactiveRuleStore ruleStore;
    private final Cache<RuleCacheKey, Mono<List<RuleBO>>> cache;

    public RuleRegistry(ReactiveRuleStore ruleStore, AlarmCacheProperties properties) {
        this.ruleStore = ruleStore;
        AlarmCacheProperties.CacheTuning tuning = properties.getRule();
        this.cache = Caffeine.newBuilder().maximumSize(tuning.getMaxSize())
                .expireAfterWrite(Duration.ofSeconds(tuning.getTtlSeconds())).build();
    }

    public Mono<List<RuleBO>> findCandidates(RuleFact fact) {
        if (fact == null || fact.getTenantId() == null || fact.getTenantId() <= 0
                || fact.getAlarmTargetTypeFlag() == null || fact.getEntityId() == null || fact.getEntityId() <= 0) {
            return Mono.just(List.of());
        }
        RuleCacheKey key = new RuleCacheKey(fact.getTenantId(), fact.getAlarmTargetTypeFlag(), fact.getEntityId());
        return cache.get(key, ignored -> ruleStore.listEnabledCandidates(fact.getTenantId(), fact.getAlarmTargetTypeFlag(), fact.getEntityId())
                .collectList().cache());
    }

    public void invalidateTenant(Long tenantId) {
        if (tenantId != null) cache.asMap().keySet().removeIf(key -> Objects.equals(key.tenantId(), tenantId));
    }

    public void invalidateAll() { cache.invalidateAll(); }

    public record RuleCacheKey(Long tenantId, AlarmTargetTypeEnum targetType, Long entityId) { }
}
