package io.github.pnoker.common.data.biz.store;

import io.github.pnoker.common.entity.bo.PointValueBO;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive write orchestration for telemetry ingest. */
public interface PointValueIngestService {

    Mono<Boolean> saveValue(PointValueBO valueBO);

    Mono<List<PointValueBO>> saveValues(List<PointValueBO> valueBOList);

    Mono<Void> markProcessed(List<PointValueBO> valueBOList);

    /** Replay leased/pending receipts after a process crash. */
    Mono<Integer> replayPending();
}
