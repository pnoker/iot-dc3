package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.IdentityAuditLogDO;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import reactor.core.publisher.Mono;

/** Non-blocking cursor read port for identity audit events. */
public interface ReactiveAuditLogQueryStore {

    Mono<CursorPage<IdentityAuditLogDO>> list(IdentityAuditLogFilter filter);
}
