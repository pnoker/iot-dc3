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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;

/**
 * Alarm rule processing pipeline implementation.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class AlarmRulePipelineServiceImpl implements AlarmRulePipelineService {

    private final RuleEngine ruleEngine;

    private final RuleNotificationService ruleNotificationService;

    private final RuleAlarmPersistenceService ruleAlarmPersistenceService;

    @Override
    public Flux<NotifyHistoryBO> process(RuleFact fact) {
        if (fact == null) {
            return Flux.empty();
        }
        return ruleEngine.evaluate(fact)
                .concatMap(match -> ruleAlarmPersistenceService.ensureAlarm(match)
                        .flatMapMany(ruleNotificationService::notify));
    }

    @Override
    public Flux<NotifyHistoryBO> processBatch(List<RuleFact> facts) {
        if (facts == null || facts.isEmpty()) {
            return Flux.empty();
        }
        List<RuleFact> validFacts = facts.stream()
                .filter(Objects::nonNull)
                .filter(f -> Objects.nonNull(f.getTenantId()))
                .filter(f -> Objects.nonNull(f.getAlarmTargetTypeFlag()))
                .toList();
        if (validFacts.isEmpty()) {
            return Flux.empty();
        }

        // Group by (tenantId, alarmTargetTypeFlag, entityId) so RuleRegistry
        // cache lookups amortize across all facts in the same group.
        Map<RuleRegistry.RuleCacheKey, List<RuleFact>> grouped = validFacts.stream()
                .collect(Collectors.groupingBy(f ->
                        new RuleRegistry.RuleCacheKey(f.getTenantId(), f.getAlarmTargetTypeFlag(), f.getEntityId())));

        return Flux.fromIterable(grouped.values())
                .concatMap(Flux::fromIterable)
                .concatMap(ruleEngine::evaluate)
                .concatMap(match -> ruleAlarmPersistenceService.ensureAlarm(match))
                .collectList()
                .flatMapMany(matches -> matches.isEmpty()
                        ? Flux.empty()
                        : ruleNotificationService.notifyBatch(matches));
    }

}
