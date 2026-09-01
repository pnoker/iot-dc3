package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.enums.PointCommandTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/** Reactive, tenant-scoped persistence port for point command history. */
public interface ReactivePointCommandStore {

    Mono<PointCommandHistoryDO> find(Long tenantId, String commandId);

    Mono<PointCommandHistoryDO> insert(PointCommandHistoryDO command);

    Mono<Boolean> markSent(Long tenantId, String commandId, Instant sentAt);

    Mono<Boolean> markPublishFailed(Long tenantId, String commandId, String errorCode,
                                    String errorMessage, Instant finishedAt);

    Mono<Boolean> complete(Long tenantId, String commandId, PointCommandStatusEnum status,
                           String responseValue, String errorCode, String errorMessage, Instant finishedAt);

    Mono<Boolean> markDead(Long tenantId, String commandId, String errorCode,
                           String errorMessage, Instant finishedAt);

    Mono<OffsetPage<PointCommandHistoryDO>> list(Long tenantId, Long deviceId, Long pointId,
                                                 PointCommandStatusEnum status,
                                                 PointCommandTypeEnum type,
                                                 long offset, int limit, List<SortSpec> sort);
}
