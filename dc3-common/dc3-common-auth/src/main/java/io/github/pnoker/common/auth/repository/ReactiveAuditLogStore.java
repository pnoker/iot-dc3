package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.IdentityAuditLogBO;
import reactor.core.publisher.Mono;

/** Non-blocking append port for identity and authorization audit events. */
public interface ReactiveAuditLogStore {

    /**
     * Append one audit event. The event is immutable after this call and must be tenant-scoped.
     *
     * @param event audit event to append
     * @return completion after the row has been persisted
     */
    Mono<Void> append(IdentityAuditLogBO event);
}
