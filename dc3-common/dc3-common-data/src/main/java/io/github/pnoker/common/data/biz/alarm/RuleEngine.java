package io.github.pnoker.common.data.biz.alarm;

import reactor.core.publisher.Flux;

/** Reactive deterministic rule engine. */
public interface RuleEngine {
    Flux<RuleMatch> evaluate(RuleFact fact);
}
