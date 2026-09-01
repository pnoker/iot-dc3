package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.PointValueDO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Durable relational receipt for point-value ingestion. */
public interface ReactivePointValueIngestOutbox {

    /**
     * Insert new receipts and claim them for the supplied owner in one
     * transaction. Existing receipts are never re-claimed by a duplicate
     * delivery.
     */
    Mono<List<PointValueDO>> enqueue(List<PointValueDO> values, String owner);

    Flux<PointValueDO> findPersisted(List<PointValueDO> values);

    Mono<Integer> markPersisted(PointValueDO value, String owner);

    Flux<PointValueDO> claim(String owner, int limit);

    Mono<Integer> markProcessed(PointValueDO value);

    Mono<Integer> markFailed(PointValueDO value, String owner, String error);
}
