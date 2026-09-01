package io.github.pnoker.db.r2dbc.core.transaction;

import reactor.core.publisher.Mono;

/** Transaction boundary for a consistent count-and-items page snapshot. */
public interface PageTransaction {

    <T> Mono<T> transactional(Mono<T> work);
}
