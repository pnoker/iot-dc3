package io.github.pnoker.db.r2dbc.core.operation;

import io.github.pnoker.db.r2dbc.core.tenant.TenantScope;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Durable operation port used by asynchronous HTTP/gRPC commands.
 * Implementations must scope every lookup and mutation by tenant and must not
 * infer ownership from a request-local context.
 */
public interface OperationRepository {

    Mono<OperationState> create(TenantScope tenant, OperationState state);

    Mono<OperationState> findById(TenantScope tenant, UUID operationId);

    Mono<OperationState> findByIdempotencyKey(TenantScope tenant, String idempotencyKey);

    /**
     * Apply one validated state transition using optimistic status matching.
     * A missing row or stale expected status is reported as an error.
     */
    Mono<OperationState> transition(
            TenantScope tenant,
            UUID operationId,
            OperationState.Status expectedStatus,
            OperationState nextState);
}
