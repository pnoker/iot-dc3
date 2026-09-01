package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.EventHistoryDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive tenant-scoped persistence port for event history. */
public interface ReactiveEventHistoryStore {

    Mono<EventHistoryDO> insert(EventHistoryDO event);

    Mono<EventHistoryDO> findByRecordId(Long tenantId, String recordId);

    Mono<OffsetPage<EventHistoryDO>> list(Long tenantId, Long deviceId, Long eventId, String eventCode,
                                           Byte eventTypeFlag, long offset, int limit, List<SortSpec> sort);
}
