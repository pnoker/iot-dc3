package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.bo.dashboard.AlertItemRow;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** Reactive, tenant-scoped read/write port for dashboard alert projections. */
public interface ReactiveAlertStore {

    /** Lists alert rows using canonical offset pagination and whitelisted sorting. */
    Mono<OffsetPage<AlertItemRow>> list(Long tenantId, String source, Integer alarmTypeFlag,
                                         Integer confirmFlag, LocalDateTime from, PageRequest page);

    /** Updates confirmation state for one tenant-owned alert row and source. */
    Mono<Boolean> updateConfirm(Long tenantId, String source, Long id, byte confirmFlag);
}
