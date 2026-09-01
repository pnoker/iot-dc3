package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.repository.ReactiveRuleStateLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Reactive deterministic rule engine implementation. */
@Service
@RequiredArgsConstructor
public class RuleEngineImpl implements RuleEngine {

    private final RuleRegistry ruleRegistry;
    private final ReactiveRuleStateLookup ruleStateLookup;
    private final RuleEvaluator ruleEvaluator;

    @Override
    public Flux<RuleMatch> evaluate(RuleFact fact) {
        if (fact == null || fact.getTenantId() == null || fact.getAlarmTargetTypeFlag() == null) {
            return Flux.empty();
        }
        return ruleRegistry.findCandidates(fact).flatMapMany(Flux::fromIterable)
                .concatMap(rule -> {
                    return ruleEvaluator.matches(rule, fact)
                            .flatMapMany(matches -> matches
                                    ? Flux.just(RuleMatch.firing(rule, fact))
                                    : ruleEvaluator.recovers(rule, fact)
                                            .flatMapMany(recovers -> recovers
                                                    ? hasFiringState(rule, fact).filter(Boolean.TRUE::equals)
                                                    .map(ignored -> RuleMatch.recovery(rule, fact))
                                                    : Mono.empty()));
                });
    }

    private Mono<Boolean> hasFiringState(RuleBO rule, RuleFact fact) {
        if (rule == null || rule.getId() == null || rule.getId() <= 0 || fact.getEntityId() == null
                || fact.getEntityId() <= 0 || rule.getAlarmTargetTypeFlag() == null) {
            return Mono.just(false);
        }
        return ruleStateLookup.hasFiringState(fact.getTenantId(), rule.getId(),
                rule.getAlarmTargetTypeFlag().getIndex(), fact.getEntityId());
    }
}
