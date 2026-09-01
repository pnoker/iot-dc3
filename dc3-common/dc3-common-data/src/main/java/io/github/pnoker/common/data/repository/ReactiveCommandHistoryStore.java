package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/** Reactive, tenant-scoped persistence port for custom command history. */
public interface ReactiveCommandHistoryStore {

    Mono<CommandHistoryDO> find(Long tenantId, String recordId);

    Mono<CommandHistoryDO> insert(CommandHistoryDO history);

    Mono<Boolean> markSent(Long tenantId, String recordId, Instant sentAt);

    Mono<Boolean> markPublishFailed(Long tenantId, String recordId, String errorCode,
                                    String errorMessage, Instant finishedAt);

    Mono<Boolean> complete(Long tenantId, String recordId, PointCommandStatusEnum status,
                           String resultValues, String configSnapshot, String errorCode,
                           String errorMessage, Instant finishedAt);

    Mono<Boolean> markDead(Long tenantId, String recordId, String errorCode,
                           String errorMessage, Instant finishedAt);

    Mono<OffsetPage<CommandHistoryDO>> list(Long tenantId, Long deviceId, Long commandId,
                                            String commandCode, PointCommandStatusEnum status,
                                            long offset, int limit, List<SortSpec> sort);
}
